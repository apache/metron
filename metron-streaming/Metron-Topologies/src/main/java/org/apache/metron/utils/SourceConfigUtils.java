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

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.metron.Constants;
import org.apache.metron.domain.SourceConfig;
import org.apache.zookeeper.KeeperException;

import java.io.ByteArrayInputStream;
import java.io.File;
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
    try {
      File root = new File("./metron-streaming/Metron-Common/src/test/resources/config/source/");
      for(File child: root.listFiles()) {
        writeToZookeeperFromFile(child.getName().replaceFirst("-config.json", ""), child.getPath(), "node1:2181");
      }
      SourceConfigUtils.dumpConfigs("node1:2181");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


}
