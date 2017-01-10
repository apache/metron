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
package org.apache.metron.maas.service.runner;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.metron.maas.config.Endpoint;
import org.apache.metron.maas.util.ConfigUtil;
import org.apache.metron.maas.config.MaaSConfig;
import org.apache.metron.maas.config.ModelEndpoint;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class Runner {
  private static final int NUM_ATTEMPTS = 10*60;
  private static final int SLEEP_AMT = 1000;

  public enum RunnerOptions {
    HELP("h", code -> {
      Option o = new Option(code, "help", false, "This screen");
      o.setRequired(false);
      return o;
    })
    ,ZK_QUORUM("zq", code -> {
      Option o = new Option(code, "zk_quorum", true, "Zookeeper Quorum");
      o.setRequired(true);
      return o;
    })
    ,ZK_ROOT("zr", code -> {
      Option o = new Option(code, "zk_root", true, "Zookeeper Root");
      o.setRequired(true);
      return o;
    })
    ,SCRIPT("s", code -> {
      Option o = new Option(code, "script", true, "Script Path");
      o.setRequired(true);
      return o;
    })
    ,NAME("n", code -> {
      Option o = new Option(code, "name", true, "Name");
      o.setRequired(true);
      return o;
    })
    ,CONTAINER_ID("ci", code -> {
      Option o = new Option(code, "container_id", true, "Container ID");
      o.setRequired(true);
      return o;
    })
    ,HOSTNAME("hn", code -> {
      Option o = new Option(code, "hostname", true, "Hostname for container");
      o.setRequired(true);
      return o;
    })
    ,VERSION("v", code -> {
      Option o = new Option(code, "version", true, "Version");
      o.setRequired(true);
      return o;
    })
    ;
    Option option;
    String shortCode;
    RunnerOptions(String shortCode
                 , Function<String, Option> optionHandler
                 ) {
      this.shortCode = shortCode;
      this.option = optionHandler.apply(shortCode);
    }

    public Map.Entry<RunnerOptions, String> of(String value) {
      if(option.hasArg()) {
        return new AbstractMap.SimpleEntry<>(this, value);
      }
      return new AbstractMap.SimpleEntry<>(this, null);
    }

    @SafeVarargs
    public static String toArgs(Map.Entry<RunnerOptions, String> ... arg) {
      return
      Joiner.on(" ").join(Iterables.transform(Arrays.asList(arg)
                                             , a -> "-" + a.getKey().shortCode
                                                  + (a.getValue() == null?"":(" " + a.getValue()))
                                             )
                         );

    }

    public boolean has(CommandLine cli) {
      return cli.hasOption(shortCode);
    }

    public String get(CommandLine cli) {
      return cli.getOptionValue(shortCode);
    }
    public String get(CommandLine cli, String def) {
      return has(cli)?cli.getOptionValue(shortCode):def;
    }


    public static CommandLine parse(CommandLineParser parser, String[] args) throws ParseException {
      try {
        CommandLine cli = parser.parse(getOptions(), args);
        if(HELP.has(cli)) {
          printHelp();
          System.exit(0);
        }
        return cli;
      } catch (ParseException e) {
        System.err.println("Unable to parse args: " + Joiner.on(' ').join(args));
        e.printStackTrace(System.err);
        printHelp();
        throw e;
      }
    }

    public static void printHelp() {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp( "MaaSRunner", getOptions());
    }

    public static Options getOptions() {
      Options ret = new Options();
      for(RunnerOptions o : RunnerOptions.values()) {
        ret.addOption(o.option);
      }
      return ret;
    }
  }
  private static final Log LOG = LogFactory.getLog(Runner.class);
  private static Process p;
  private static ServiceDiscovery<ModelEndpoint> serviceDiscovery = null;
  public static void main(String... argv) throws Exception {
    CommandLine cli = RunnerOptions.parse(new PosixParser(), argv);
    String zkQuorum = RunnerOptions.ZK_QUORUM.get(cli);
    String zkRoot = RunnerOptions.ZK_ROOT.get(cli);
    String script = RunnerOptions.SCRIPT.get(cli);
    String name = RunnerOptions.NAME.get(cli);
    String version = RunnerOptions.VERSION.get(cli);
    String containerId = RunnerOptions.CONTAINER_ID.get(cli);
    String hostname = RunnerOptions.HOSTNAME.get(cli);
    CuratorFramework client = null;

    LOG.error("Running script " + script);
    LOG.info("Local Directory Contents");
    for(File f : new File(".").listFiles()) {
      LOG.info("  " + f.getName());
    }
    try {
      RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
      client = CuratorFrameworkFactory.newClient(zkQuorum, retryPolicy);
      client.start();
      MaaSConfig config = ConfigUtil.INSTANCE.read(client, zkRoot, new MaaSConfig(), MaaSConfig.class);
      JsonInstanceSerializer<ModelEndpoint> serializer = new JsonInstanceSerializer<>(ModelEndpoint.class);
      try {
        serviceDiscovery = ServiceDiscoveryBuilder.builder(ModelEndpoint.class)
                .client(client)
                .basePath(config.getServiceRoot())
                .serializer(serializer)
                .build();
      }
      finally {
      }
      LOG.info("Created service @ " + config.getServiceRoot());


      serviceDiscovery.start();

      File cwd = new File(script).getParentFile();
      final String cmd = new File(cwd, script).getAbsolutePath();
        try {
          p = new ProcessBuilder(cmd).directory(cwd).start();

        }
        catch(Exception e) {
          LOG.info("Unable to execute " + cmd + " from " + new File(".").getAbsolutePath());
          LOG.error(e.getMessage(), e);
          throw new IllegalStateException(e.getMessage(), e);
        }


      try {
        LOG.info("Started " + cmd);
        Endpoint ep = readEndpoint(cwd);
        URL endpointUrl =correctLocalUrl(hostname, ep.getUrl());
        ep.setUrl(endpointUrl.toString());
        LOG.info("Read endpoint " + ep);
        ModelEndpoint endpoint = new ModelEndpoint();
        {
          endpoint.setName(name);
          endpoint.setContainerId(containerId);
          endpoint.setEndpoint(ep);
          endpoint.setVersion(version);
        };
        ServiceInstanceBuilder<ModelEndpoint> builder = ServiceInstance.<ModelEndpoint> builder()
                                                                       .address(endpointUrl.getHost())
                                                                       .id(containerId)
                                                                       .name(name)
                                                                       .port(endpointUrl.getPort())
                                                                       .registrationTimeUTC(System.currentTimeMillis())
                                                                       .serviceType(ServiceType.STATIC)
                                                                       .payload(endpoint)
                                                                       ;
        final ServiceInstance<ModelEndpoint> instance = builder.build();
        try {
          LOG.info("Installing service instance: " + instance + " at " + serviceDiscovery);
          serviceDiscovery.registerService(instance);
          LOG.info("Installed instance " + name + ":" + version + "@" + endpointUrl);
        }
        catch(Throwable t) {
          LOG.error("Unable to install instance " + name + ":" + version + "@" + endpointUrl, t);
        }

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
          @Override
          public void run()
          {
            LOG.info("KILLING CONTAINER PROCESS...");
            if(p != null) {
              LOG.info("Process destroyed forcibly");
              p.destroyForcibly();
            }
          }
        });
      }
      finally {
        if (p.waitFor() != 0) {
          String stderr = Joiner.on("\n").join(IOUtils.readLines(p.getErrorStream()));
          String stdout = Joiner.on("\n").join(IOUtils.readLines(p.getInputStream()));
          throw new IllegalStateException("Unable to execute " + script + ".  Stderr is: " + stderr + "\nStdout is: " + stdout);
        }
      }
    }
    finally {
      if(serviceDiscovery != null) {
        CloseableUtils.closeQuietly(serviceDiscovery);
      }
      if(client != null) {
        CloseableUtils.closeQuietly(client);
      }
    }
  }

  private static Set<String> localAddresses = new HashSet<String>() {{
    add("localhost");
    add("127.0.0.1");
    add("0.0.0.0");
  }};
  private static URL correctLocalUrl(String hostname, String tmpUrl) throws MalformedURLException {
    URL tmp = new URL(tmpUrl);
    if(hostname != null && hostname.length() > 0 && localAddresses.contains(tmp.getHost())) {
      URL endpointUrl = null;
      try {
        endpointUrl = new URL(tmp.getProtocol(), hostname, tmp.getPort(), tmp.getFile());
      } catch (MalformedURLException e) {
        LOG.error("Unable to process " + hostname + " as a valid hostname", e);
        return tmp;
      }
      return endpointUrl;
    }
    return tmp;
  }

  private static Endpoint readEndpoint(File cwd) throws Exception {
    String content = "";
    File f = new File(cwd, "endpoint.dat");
    for(int i = 0;i < NUM_ATTEMPTS;i++) {
      if(f.exists()) {
        try {
          content = Files.toString(f, Charsets.US_ASCII);
        }
        catch(IOException ioe) {
        }
        if(content != null && content.length() > 0) {
          try {
            Endpoint ep = ConfigUtil.INSTANCE.read(content.getBytes(), Endpoint.class);
            return ep;
          }
          catch(Exception ex) {
            LOG.error("Unable to parse " + content + ": " + ex.getMessage(), ex);
          }
        }
      }
      Thread.sleep(SLEEP_AMT);
    }
    throw new IllegalStateException("Unable to start process within the allotted time (10 minutes)");
  }
}
