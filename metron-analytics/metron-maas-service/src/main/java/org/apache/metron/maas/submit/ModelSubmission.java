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
package org.apache.metron.maas.submit;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.PropertyConfigurator;
import org.apache.metron.maas.config.*;
import org.apache.metron.maas.discovery.ServiceDiscoverer;
import org.apache.metron.maas.service.Constants;
import org.apache.metron.maas.service.Log4jPropertyHelper;
import org.apache.metron.maas.util.ConfigUtil;
import org.apache.metron.maas.queue.Queue;
import org.apache.metron.maas.queue.ZKQueue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ModelSubmission {
  public enum ModelSubmissionOptions {
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
      o.setRequired(false);
      return o;
    })
    ,LOCAL_MODEL_PATH("lmp", code -> {
      Option o = new Option(code, "local_model_path", true, "Model Path (local)");
      o.setRequired(false);
      return o;
    })
    ,HDFS_MODEL_PATH("hmp", code -> {
      Option o = new Option(code, "hdfs_model_path", true, "Model Path (HDFS)");
      o.setRequired(false);
      return o;
    })
    ,NAME("n", code -> {
      Option o = new Option(code, "name", true, "Model Name");
      o.setRequired(false);
      return o;
    })
    ,VERSION("v", code -> {
      Option o = new Option(code, "version", true, "Model version");
      o.setRequired(false);
      return o;
    })
    ,NUM_INSTANCES("ni", code -> {
      Option o = new Option(code, "num_instances", true, "Number of model instances");
      o.setRequired(false);
      return o;
    })
    ,MEMORY("m", code -> {
      Option o = new Option(code, "memory", true, "Memory for container");
      o.setRequired(false);
      return o;
    })
    ,MODE("mo", code -> {
      Option o = new Option(code, "mode", true, "ADD, LIST or REMOVE");
      o.setRequired(true);
      return o;
    })
    ,LOG4J_PROPERTIES("l", code -> {
      Option o = new Option(code, "log4j", true, "The log4j properties file to load");
      o.setArgName("FILE");
      o.setRequired(false);
      return o;
    })
    ;
    Option option;
    String shortCode;
    ModelSubmissionOptions(String shortCode
                 , Function<String, Option> optionHandler
                 ) {
      this.shortCode = shortCode;
      this.option = optionHandler.apply(shortCode);
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

    public Map.Entry<ModelSubmissionOptions, String> of(String value) {
      if(option.hasArg()) {
        return new AbstractMap.SimpleEntry<>(this, value);
      }
      return new AbstractMap.SimpleEntry<>(this, null);
    }

    @SafeVarargs
    public static String toArgs(Map.Entry<ModelSubmissionOptions, String> ... arg) {
      return
      Joiner.on(" ").join(Iterables.transform(Arrays.asList(arg)
                                             , a -> "-" + a.getKey().option.getOpt()
                                                  + (a.getValue() == null?"":(" " + a.getValue()))
                                             )
                         );

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
      formatter.printHelp( "ModelSubmission", getOptions());
    }

    public static Options getOptions() {
      Options ret = new Options();
      for(ModelSubmissionOptions o : ModelSubmissionOptions.values()) {
        ret.addOption(o.option);
      }
      return ret;
    }
  }

  public void execute(FileSystem fs, String... argv) throws Exception {
    CommandLine cli = ModelSubmissionOptions.parse(new PosixParser(), argv);
    if(ModelSubmissionOptions.LOG4J_PROPERTIES.has(cli)) {
      Log4jPropertyHelper.updateLog4jConfiguration(ModelSubmission.class, ModelSubmissionOptions.LOG4J_PROPERTIES.get(cli));
    }
    ModelRequest request = null;
    CuratorFramework client = null;
    try {
      RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
      client = CuratorFrameworkFactory.newClient(ModelSubmissionOptions.ZK_QUORUM.get(cli), retryPolicy);
      client.start();
      MaaSConfig config = ConfigUtil.INSTANCE.read(client, ModelSubmissionOptions.ZK_ROOT.get(cli, "/metron/maas/config"), new MaaSConfig(), MaaSConfig.class);
      String mode = ModelSubmissionOptions.MODE.get(cli);
      if ( mode.equalsIgnoreCase("ADD")) {
        request = new ModelRequest() {{
          setName(ModelSubmissionOptions.NAME.get(cli));
          setAction(Action.ADD);
          setVersion(ModelSubmissionOptions.VERSION.get(cli));
          setNumInstances(Integer.parseInt(ModelSubmissionOptions.NUM_INSTANCES.get(cli)));
          setMemory(Integer.parseInt(ModelSubmissionOptions.MEMORY.get(cli)));
          setPath(ModelSubmissionOptions.HDFS_MODEL_PATH.get(cli));
        }};
      } else if(mode.equalsIgnoreCase("REMOVE")) {
        request = new ModelRequest() {{
          setName(ModelSubmissionOptions.NAME.get(cli));
          setAction(Action.REMOVE);
          setNumInstances(Integer.parseInt(ModelSubmissionOptions.NUM_INSTANCES.get(cli)));
          setVersion(ModelSubmissionOptions.VERSION.get(cli));
        }};
      }
      else if(mode.equalsIgnoreCase("LIST")) {
        String name = ModelSubmissionOptions.NAME.get(cli, null);
        String version = ModelSubmissionOptions.VERSION.get(cli, null);
        ServiceDiscoverer serviceDiscoverer = new ServiceDiscoverer(client, config.getServiceRoot());

        Model model = new Model(name, version);
        Map<Model, List<ModelEndpoint>> endpoints = serviceDiscoverer.listEndpoints(model);
        for(Map.Entry<Model, List<ModelEndpoint>> kv : endpoints.entrySet()) {
          String modelTitle = "Model " + kv.getKey().getName() + " @ " + kv.getKey().getVersion();
          System.out.println(modelTitle);
          for(ModelEndpoint endpoint : kv.getValue()){
            System.out.println(endpoint);
          }
        }
      }


      if (ModelSubmissionOptions.LOCAL_MODEL_PATH.has(cli)) {
        File localDir = new File(ModelSubmissionOptions.LOCAL_MODEL_PATH.get(cli));
        Path hdfsPath = new Path(ModelSubmissionOptions.HDFS_MODEL_PATH.get(cli));
        updateHDFS(fs, localDir, hdfsPath);
      }
      Queue<ModelRequest> queue = config.createQueue(ImmutableMap.of(ZKQueue.ZK_CLIENT, client));
      queue.enqueue(request);
    } finally {
      if (client != null) {
        client.close();
      }
    }
  }

  public static void main(String... argv) throws Exception {
    FileSystem fs = FileSystem.get(new Configuration());
    ModelSubmission submission = new ModelSubmission();
    submission.execute(fs, argv);
  }

  public static void updateHDFS(FileSystem fs, File localDir, Path hdfsPath) throws IOException {
    if(localDir.exists() && localDir.isDirectory()) {
      if(!fs.exists(hdfsPath)) {
        fs.mkdirs(hdfsPath);
      }
      for(File f : localDir.listFiles()) {
        if(f.getName().equals(Constants.ENDPOINT_DAT)) {
          //skip the endpoint if it exists accidentally, we don't want to localize that.
          continue;
        }
        Path p = new Path(hdfsPath, f.getName());
        FSDataOutputStream out = fs.create(p);
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
        IOUtils.copy(in, out);
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
      }
    }
  }
}
