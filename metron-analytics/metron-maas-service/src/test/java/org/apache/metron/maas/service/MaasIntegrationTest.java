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
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.hadoop.fs.FileSystem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.util.JarFinder;
import org.apache.hadoop.util.Shell;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler;
import org.apache.metron.maas.discovery.ServiceDiscoverer;
import org.apache.metron.maas.config.MaaSConfig;
import org.apache.metron.maas.config.Model;
import org.apache.metron.maas.config.ModelEndpoint;
import org.apache.metron.maas.queue.ZKQueue;
import org.apache.metron.maas.submit.ModelSubmission;
import org.apache.metron.maas.util.ConfigUtil;
import org.apache.zookeeper.KeeperException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MaasIntegrationTest {
  private static final Log LOG =
          LogFactory.getLog(MaasIntegrationTest.class);

  protected MiniYARNCluster yarnCluster = null;
  protected YarnConfiguration conf = null;
  private static final int NUM_NMS = 1;

  protected final static String APPMASTER_JAR =
          JarFinder.getJar(ApplicationMaster.class);
  private TestingServer testZkServer;
  private String zookeeperUrl;
  private CuratorFramework client;
  @Before
  public void setup() throws Exception {
    setupInternal(NUM_NMS);
  }

  protected void setupInternal(int numNodeManager) throws Exception {

    LOG.info("Starting up YARN cluster");
    testZkServer = new TestingServer(true);
    zookeeperUrl = testZkServer.getConnectString();
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    client = CuratorFrameworkFactory.newClient(zookeeperUrl, retryPolicy);
    client.start();
    conf = new YarnConfiguration();
    conf.setInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB, 128);
    conf.set("yarn.log.dir", "target");
    conf.setBoolean(YarnConfiguration.TIMELINE_SERVICE_ENABLED, true);
    conf.set(YarnConfiguration.RM_SCHEDULER, CapacityScheduler.class.getName());
    conf.setBoolean(YarnConfiguration.NODE_LABELS_ENABLED, true);

    if (yarnCluster == null) {
      yarnCluster =
              new MiniYARNCluster(MaasIntegrationTest.class.getSimpleName(), 1,
                      numNodeManager, 1, 1, true);
      yarnCluster.init(conf);

      yarnCluster.start();

      waitForNMsToRegister();

      URL url = Thread.currentThread().getContextClassLoader().getResource("yarn-site.xml");
      if (url == null) {
        throw new RuntimeException("Could not find 'yarn-site.xml' dummy file in classpath");
      }
      Configuration yarnClusterConfig = yarnCluster.getConfig();
      yarnClusterConfig.set("yarn.application.classpath", new File(url.getPath()).getParent());
      //write the document to a buffer (not directly to the file, as that
      //can cause the file being written to get read -which will then fail.
      ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
      yarnClusterConfig.writeXml(bytesOut);
      bytesOut.close();
      //write the bytes to the file in the classpath
      OutputStream os = new FileOutputStream(new File(url.getPath()));
      os.write(bytesOut.toByteArray());
      os.close();
    }
    FileContext fsContext = FileContext.getLocalFSFileContext();
    fsContext
            .delete(
                    new Path(conf
                            .get("yarn.timeline-service.leveldb-timeline-store.path")),
                    true);
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      LOG.info("setup thread sleep interrupted. message=" + e.getMessage());
    }
  }

  @After
  public void tearDown() throws IOException {
    if(testZkServer != null) {
      testZkServer.close();
    }
    if (yarnCluster != null) {
      try {
        yarnCluster.stop();
      } finally {
        yarnCluster = null;
      }
    }
    FileContext fsContext = FileContext.getLocalFSFileContext();
    fsContext
            .delete(
                    new Path(conf
                            .get("yarn.timeline-service.leveldb-timeline-store.path")),
                    true);
  }

  @Test(timeout=900000)
  public void testMaaSWithDomain() throws Exception {
    testDSShell(true);
  }

  @Test(timeout=900000)
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
            "--jar", APPMASTER_JAR,
            "--zk_quorum", zookeeperUrl,
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

    LOG.info("Initializing DS Client");
    final Client client = new Client(new Configuration(yarnCluster.getConfig()));
    boolean initSuccess = client.init(args);
    Assert.assertTrue(initSuccess);
    LOG.info("Running DS Client");
    final AtomicBoolean result = new AtomicBoolean(false);
    Thread t = new Thread() {
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
    yarnClient.init(new Configuration(yarnCluster.getConfig()));
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
    Assert.assertTrue(errorMessage, verified);
    FileSystem fs = FileSystem.get(conf);
    try {
      new ModelSubmission().execute(FileSystem.get(conf)
              , new String[]{
                      "--name", "dummy",
                      "--version", "1.0",
                      "--zk_quorum", zookeeperUrl,
                      "--zk_root", configRoot,
                      "--local_model_path", "src/test/resources/maas",
                      "--hdfs_model_path", new Path(fs.getHomeDirectory(), "maas/dummy").toString(),
                      "--num_instances", "1",
                      "--memory", "100",
                      "--mode", "ADD",

              });
      ServiceDiscoverer discoverer = new ServiceDiscoverer(this.client, config.getServiceRoot());
      discoverer.start();
      {
        boolean passed = false;
        for (int i = 0; i < 100; ++i) {
          try {
            List<ModelEndpoint> endpoints = discoverer.getEndpoints(new Model("dummy", "1.0"));
            if (endpoints != null && endpoints.size() == 1) {
              String output = makeRESTcall(new URL(endpoints.get(0).getUrl() + "/echo/casey"));
              if (output.contains("casey")) {
                passed = true;
                break;
              }
            }
          } catch (Exception e) {
          }
          Thread.sleep(2000);
        }
        Assert.assertTrue(passed);
      }

      {
        List<ModelEndpoint> endpoints = discoverer.getEndpoints(new Model("dummy", "1.0"));
        Assert.assertNotNull(endpoints);
        Assert.assertEquals(1, endpoints.size());
      }
      new ModelSubmission().execute(FileSystem.get(conf)
              , new String[]{
                      "--name", "dummy",
                      "--version", "1.0",
                      "--zk_quorum", zookeeperUrl,
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
        Assert.assertTrue(passed);
      }
    }
    finally {
      cleanup();
    }
  }

  private void cleanup() {
    try {
      System.out.println("Cleaning up...");
      String line;
      Process p = Runtime.getRuntime().exec("ps -e");
      BufferedReader input =
              new BufferedReader(new InputStreamReader(p.getInputStream()));
      while ((line = input.readLine()) != null) {
        if(line.contains("dummy_rest.sh")) {
          String pid = Iterables.get(Splitter.on(" ").split(line.replaceAll("\\s+", " ")), 0);
          System.out.println("Killing " + pid + " from " + line);
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
              (conn.getInputStream())));

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

    Assert.assertTrue("Unknown format for hostname " + appHostname,
            appHostname.contains("/"));
    Assert.assertTrue("Unknown format for hostname " + hostname,
            hostname.contains("/"));

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



  protected void waitForNMsToRegister() throws Exception {
    int sec = 60;
    while (sec >= 0) {
      if (yarnCluster.getResourceManager().getRMContext().getRMNodes().size()
              >= NUM_NMS) {
        break;
      }
      Thread.sleep(1000);
      sec--;
    }
  }
  private int verifyContainerLog(int containerNum,
                                 List<String> expectedContent, boolean count, String expectedWord) {
    File logFolder =
            new File(yarnCluster.getNodeManager(0).getConfig()
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
    Assert.assertTrue(currentContainerLogFileIndex != -1);
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
            br = new BufferedReader(new FileReader(output));
            int numOfline = 0;
            while ((sCurrentLine = br.readLine()) != null) {
              if (count) {
                if (sCurrentLine.contains(expectedWord)) {
                  numOfWords++;
                }
              } else if (output.getName().trim().equals("stdout")){
                if (! Shell.WINDOWS) {
                  Assert.assertEquals("The current is" + sCurrentLine,
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
              Assert.assertTrue(stdOutContent.containsAll(expectedContent));
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
