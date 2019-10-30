/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.maas.service;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.util.Shell;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.components.YarnComponent;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.maas.config.MaaSConfig;
import org.apache.metron.maas.config.Model;
import org.apache.metron.maas.config.ModelEndpoint;
import org.apache.metron.maas.discovery.ServiceDiscoverer;
import org.apache.metron.maas.queue.ZKQueue;
import org.apache.metron.maas.submit.ModelSubmission;
import org.apache.metron.maas.util.ConfigUtil;
import org.apache.metron.test.utils.UnitTestHelper;
import org.apache.zookeeper.KeeperException;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;

public class MaasIntegrationTest {
  private static final Log LOG =
          LogFactory.getLog(MaasIntegrationTest.class);
  private static CuratorFramework client;
  private static ComponentRunner runner;
  private static YarnComponent yarnComponent;
  private static ZKServerComponent zkServerComponent;

  @BeforeAll
  public static void setupBeforeClass() throws Exception {
    UnitTestHelper.setJavaLoggingLevel(Level.SEVERE);
    LOG.info("Starting up YARN cluster");

    zkServerComponent = new ZKServerComponent();
    yarnComponent = new YarnComponent().withApplicationMasterClass(ApplicationMaster.class).withTestName(MaasIntegrationTest.class.getSimpleName());

    runner = new ComponentRunner.Builder()
            .withComponent("yarn", yarnComponent)
            .withComponent("zk", zkServerComponent)
            .withMillisecondsBetweenAttempts(15000)
            .withNumRetries(10)
            .build();
    runner.start();

    String zookeeperUrl = zkServerComponent.getConnectionString();
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    client = CuratorFrameworkFactory.newClient(zookeeperUrl, retryPolicy);
    client.start();
  }

  @AfterAll
  public static void tearDownAfterClass(){
    if(client != null){
      client.close();
    }
    runner.stop();
  }

  @AfterEach
  public void tearDown() {
    runner.reset();
  }

  @Test
  @Timeout(900000)
  public void testMaaSWithDomain() throws Exception {
    testDSShell(true);
  }

  @Test
  @Timeout(900000)
  public void testMaaSWithoutDomain() throws Exception {
    testDSShell(false);
  }

  public void testDSShell(boolean haveDomain) throws Exception {
    MaaSConfig config = new MaaSConfig() {{
      setServiceRoot("/maas/service");
      setQueueConfig(new HashMap<String, Object>() {{
        put(ZKQueue.ZK_PATH, "/maas/queue");
      }});
    }};
    String configRoot = "/maas/config";
    byte[] configData = ConfigUtil.INSTANCE.toBytes(config);
    try {
      client.setData().forPath(configRoot, configData);
    }
    catch(KeeperException.NoNodeException e) {
      client.create().creatingParentsIfNeeded().forPath(configRoot, configData);
    }
    String[] args = {
            "--jar", yarnComponent.getAppMasterJar(),
            "--zk_quorum", zkServerComponent.getConnectionString(),
            "--zk_root", configRoot,
            "--master_memory", "512",
            "--master_vcores", "2",
    };
    if (haveDomain) {
      String[] domainArgs = {
              "--domain",
              "TEST_DOMAIN",
              "--view_acls",
              "reader_user reader_group",
              "--modify_acls",
              "writer_user writer_group",
              "--create"
      };
      List<String> argsList = new ArrayList<String>(Arrays.asList(args));
      argsList.addAll(Arrays.asList(domainArgs));
      args = argsList.toArray(new String[argsList.size()]);
    }

    YarnConfiguration conf = yarnComponent.getConfig();
    LOG.info("Initializing DS Client");
    final Client client = new Client(new Configuration(conf));
    boolean initSuccess = client.init(args);
    assertTrue(initSuccess);
    LOG.info("Running DS Client");
    final AtomicBoolean result = new AtomicBoolean(false);
    Thread t = new Thread() {
      @Override
      public void run() {
        try {
          result.set(client.run());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };
    t.start();

    YarnClient yarnClient = YarnClient.createYarnClient();
    yarnClient.init(new Configuration(conf));
    yarnClient.start();
    String hostName = NetUtils.getHostname();

    boolean verified = false;
    String errorMessage = "";
    while(!verified) {
      List<ApplicationReport> apps = yarnClient.getApplications();
      if (apps.size() == 0 ) {
        Thread.sleep(10);
        continue;
      }
      ApplicationReport appReport = apps.get(0);
      if(appReport.getHost().equals("N/A")) {
        Thread.sleep(10);
        continue;
      }
      errorMessage =
              "Expected host name to start with '" + hostName + "', was '"
                      + appReport.getHost() + "'. Expected rpc port to be '-1', was '"
                      + appReport.getRpcPort() + "'.";
      if (checkHostname(appReport.getHost()) && appReport.getRpcPort() == -1) {
        verified = true;
      }
      if (appReport.getYarnApplicationState() == YarnApplicationState.FINISHED) {
        break;
      }
    }
    assertTrue(verified, errorMessage);
    FileSystem fs = FileSystem.get(conf);
    try {
      new ModelSubmission().execute(FileSystem.get(conf)
              , new String[]{
                      "--name", "dummy",
                      "--version", "1.0",
                      "--zk_quorum", zkServerComponent.getConnectionString(),
                      "--zk_root", configRoot,
                      "--local_model_path", "src/test/resources/maas",
                      "--hdfs_model_path", new Path(fs.getHomeDirectory(), "maas/dummy").toString(),
                      "--num_instances", "1",
                      "--memory", "100",
                      "--mode", "ADD",
                      "--log4j", "src/test/resources/log4j.properties"

              });
      ServiceDiscoverer discoverer = new ServiceDiscoverer(this.client, config.getServiceRoot());
      discoverer.start();
      {
        boolean passed = false;
        for (int i = 0; i < 100; ++i) {
          try {
            List<ModelEndpoint> endpoints = discoverer.getEndpoints(new Model("dummy", "1.0"));
            if (endpoints != null && endpoints.size() == 1) {
              LOG.trace("Found endpoints: " + endpoints.get(0));
              String output = makeRESTcall(new URL(endpoints.get(0).getEndpoint().getUrl() + "/echo/casey"));
              if (output.contains("casey")) {
                passed = true;
                break;
              }
            }
          } catch (Exception e) {
          }
          Thread.sleep(2000);
        }
        assertTrue(passed);
      }

      {
        List<ModelEndpoint> endpoints = discoverer.getEndpoints(new Model("dummy", "1.0"));
        assertNotNull(endpoints);
        assertEquals(1, endpoints.size());
      }
      new ModelSubmission().execute(FileSystem.get(conf)
              , new String[]{
                      "--name", "dummy",
                      "--version", "1.0",
                      "--zk_quorum", zkServerComponent.getConnectionString(),
                      "--zk_root", configRoot,
                      "--num_instances", "1",
                      "--mode", "REMOVE",

              });
      {
        boolean passed = false;
        for (int i = 0; i < 100; ++i) {
          try {
            List<ModelEndpoint> endpoints = discoverer.getEndpoints(new Model("dummy", "1.0"));
            //ensure that the endpoint is dead.
            if (endpoints == null || endpoints.size() == 0) {
              passed = true;
              break;
            }
          } catch (Exception e) {
          }
          Thread.sleep(2000);
        }
        assertTrue(passed);
      }
    }
    finally {
      cleanup();
    }
  }

  private void cleanup() {
    try {
      LOG.trace("Cleaning up...");
      String line;
      Process p = Runtime.getRuntime().exec("ps -e");
      BufferedReader input =
              new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
      while ((line = input.readLine()) != null) {
        if(line.contains("dummy_rest.sh")) {
          String pid = Iterables.get(Splitter.on(" ").split(line.replaceAll("\\s+", " ").trim()), 0);
          LOG.trace("Killing " + pid + " from " + line);
          Runtime.getRuntime().exec("kill -9 " + pid);
        }
      }
      input.close();
    } catch (Exception err) {
      err.printStackTrace();
    }
  }
  private String makeRESTcall(URL url) throws IOException {
    HttpURLConnection conn = null;
    //make connection
    try{
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");

      if (conn.getResponseCode() != 200) {
        throw new RuntimeException("Failed : HTTP error code : "
                + conn.getResponseCode());
      }

      BufferedReader br = new BufferedReader(new InputStreamReader(
              (conn.getInputStream()), StandardCharsets.UTF_8));

      String output = "";
      String line;
      while ((line = br.readLine()) != null) {
        output += line + "\n";
      }
      return output;
    }
    finally {
      if(conn != null) {
        conn.disconnect();
      }
    }
  }

  /*
   * NetUtils.getHostname() returns a string in the form "hostname/ip".
   * Sometimes the hostname we get is the FQDN and sometimes the short name. In
   * addition, on machines with multiple network interfaces, it runs any one of
   * the ips. The function below compares the returns values for
   * NetUtils.getHostname() accounting for the conditions mentioned.
   */
  private boolean checkHostname(String appHostname) throws Exception {

    String hostname = NetUtils.getHostname();
    if (hostname.equals(appHostname)) {
      return true;
    }

    assertTrue(appHostname.contains("/"), "Unknown format for hostname " + appHostname);
    assertTrue(hostname.contains("/"), "Unknown format for hostname " + hostname);

    String[] appHostnameParts = appHostname.split("/");
    String[] hostnameParts = hostname.split("/");

    return (compareFQDNs(appHostnameParts[0], hostnameParts[0]) && checkIPs(
            hostnameParts[0], hostnameParts[1], appHostnameParts[1]));
  }

  private boolean compareFQDNs(String appHostname, String hostname)
          throws Exception {
    if (appHostname.equals(hostname)) {
      return true;
    }
    String appFQDN = InetAddress.getByName(appHostname).getCanonicalHostName();
    String localFQDN = InetAddress.getByName(hostname).getCanonicalHostName();
    return appFQDN.equals(localFQDN);
  }

  private boolean checkIPs(String hostname, String localIP, String appIP)
          throws Exception {

    if (localIP.equals(appIP)) {
      return true;
    }
    boolean appIPCheck = false;
    boolean localIPCheck = false;
    InetAddress[] addresses = InetAddress.getAllByName(hostname);
    for (InetAddress ia : addresses) {
      if (ia.getHostAddress().equals(appIP)) {
        appIPCheck = true;
        continue;
      }
      if (ia.getHostAddress().equals(localIP)) {
        localIPCheck = true;
      }
    }
    return (appIPCheck && localIPCheck);

  }

  private int verifyContainerLog(int containerNum,
                                 List<String> expectedContent, boolean count, String expectedWord) {
    File logFolder =
            new File(yarnComponent.getYARNCluster().getNodeManager(0).getConfig()
                    .get(YarnConfiguration.NM_LOG_DIRS,
                            YarnConfiguration.DEFAULT_NM_LOG_DIRS));

    File[] listOfFiles = logFolder.listFiles();
    int currentContainerLogFileIndex = -1;
    for (int i = listOfFiles.length - 1; i >= 0; i--) {
      if (listOfFiles[i].listFiles().length == containerNum + 1) {
        currentContainerLogFileIndex = i;
        break;
      }
    }
    assertTrue(currentContainerLogFileIndex != -1);
    File[] containerFiles =
            listOfFiles[currentContainerLogFileIndex].listFiles();

    int numOfWords = 0;
    for (int i = 0; i < containerFiles.length; i++) {
      for (File output : containerFiles[i].listFiles()) {
        if (output.getName().trim().contains("stdout")) {
          BufferedReader br = null;
          List<String> stdOutContent = new ArrayList<String>();
          try {

            String sCurrentLine;
            br = new BufferedReader(new InputStreamReader(new FileInputStream(output), StandardCharsets.UTF_8));
            int numOfline = 0;
            while ((sCurrentLine = br.readLine()) != null) {
              if (count) {
                if (sCurrentLine.contains(expectedWord)) {
                  numOfWords++;
                }
              } else if (output.getName().trim().equals("stdout")){
                if (! Shell.WINDOWS) {
                  assertEquals("The current is" + sCurrentLine,
                          expectedContent.get(numOfline), sCurrentLine.trim());
                  numOfline++;
                } else {
                  stdOutContent.add(sCurrentLine.trim());
                }
              }
            }
            /* By executing bat script using cmd /c,
             * it will output all contents from bat script first
             * It is hard for us to do check line by line
             * Simply check whether output from bat file contains
             * all the expected messages
             */
            if (Shell.WINDOWS && !count
                    && output.getName().trim().equals("stdout")) {
              assertTrue(stdOutContent.containsAll(expectedContent));
            }
          } catch (IOException e) {
            e.printStackTrace();
          } finally {
            try {
              if (br != null)
                br.close();
            } catch (IOException ex) {
              ex.printStackTrace();
            }
          }
        }
      }
    }
    return numOfWords;
  }
}
