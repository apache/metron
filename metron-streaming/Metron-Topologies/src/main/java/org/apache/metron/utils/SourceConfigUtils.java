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
package org.apache.metron.utils;

import org.apache.commons.cli.*;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.metron.Constants;
import org.apache.metron.domain.SourceConfig;
import org.apache.zookeeper.KeeperException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class SourceConfigUtils {

  public static CuratorFramework getClient(String zookeeperUrl) {
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    return CuratorFrameworkFactory.newClient(zookeeperUrl, retryPolicy);
  }

  public static void writeToZookeeperFromFile(String sourceName, String filePath, String zookeeperUrl) throws Exception {
    writeToZookeeper(sourceName, Files.readAllBytes(Paths.get(filePath)), zookeeperUrl);
  }

  public static void writeToZookeeper(String sourceName, byte[] configData, String zookeeperUrl) throws Exception {
    CuratorFramework client = getClient(zookeeperUrl);
    client.start();
    try {
      client.setData().forPath(Constants.ZOOKEEPER_TOPOLOGY_ROOT + "/" + sourceName, configData);
    } catch(KeeperException.NoNodeException e) {
      client.create().creatingParentsIfNeeded().forPath(Constants.ZOOKEEPER_TOPOLOGY_ROOT + "/" + sourceName, configData);
    }
    client.close();
  }

  public static byte[] readConfigBytesFromZookeeper(String sourceName, String zookeeperUrl) throws Exception {
    CuratorFramework client = getClient(zookeeperUrl);
    client.start();
    byte[] data = client.getData().forPath(Constants.ZOOKEEPER_TOPOLOGY_ROOT + "/" + sourceName);
    client.close();
    return data;
  }

  public static SourceConfig readConfigFromZookeeper(String sourceName, String zookeeperUrl) throws Exception {
    byte[] data = readConfigBytesFromZookeeper(sourceName, zookeeperUrl);
    return SourceConfig.load(new ByteArrayInputStream(data));
  }

  public static void dumpConfigs(String zookeeperUrl) throws Exception {
    CuratorFramework client = getClient(zookeeperUrl);
    client.start();
    List<String> children = client.getChildren().forPath(Constants.ZOOKEEPER_TOPOLOGY_ROOT);
    for(String child: children) {
      byte[] data = client.getData().forPath(Constants.ZOOKEEPER_TOPOLOGY_ROOT + "/" + child);
      System.out.println("Config for source " + child);
      System.out.println(new String(data));
      System.out.println();
    }
    client.close();
  }

  public static void main(String[] args) {

    Options options = new Options();
    {
      Option o = new Option("h", "help", false, "This screen");
      o.setRequired(false);
      options.addOption(o);
    }
    {
      Option o = new Option("p", "config_files", true, "Path to the source config files.  Must be named like \"$source\"-config.json");
      o.setArgName("DIR_NAME");
      o.setRequired(false);
      options.addOption(o);
    }
    {
      Option o = new Option("z", "zk", true, "Zookeeper Quroum URL (zk1:2181,zk2:2181,...");
      o.setArgName("ZK_QUORUM");
      o.setRequired(true);
      options.addOption(o);
    }

    try {
      CommandLineParser parser = new PosixParser();
      CommandLine cmd = null;
      try {
        cmd = parser.parse(options, args);
      }
      catch(ParseException pe) {
        pe.printStackTrace();
        final HelpFormatter usageFormatter = new HelpFormatter();
        usageFormatter.printHelp("SourceConfigUtils", null, options, null, true);
        System.exit(-1);
      }
      if( cmd.hasOption("h") ){
        final HelpFormatter usageFormatter = new HelpFormatter();
        usageFormatter.printHelp("SourceConfigUtils", null, options, null, true);
        System.exit(0);
      }

      String zkQuorum = cmd.getOptionValue("z");
      if(cmd.hasOption("p")) {
        String sourcePath = cmd.getOptionValue("p");
        File root = new File(sourcePath);

        if (root.isDirectory()) {
          for (File child : root.listFiles()) {
            writeToZookeeperFromFile(child.getName().replaceFirst("-config.json", ""), child.getPath(), zkQuorum);
          }
        }
      }

      SourceConfigUtils.dumpConfigs(zkQuorum);

    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }

  }
}
