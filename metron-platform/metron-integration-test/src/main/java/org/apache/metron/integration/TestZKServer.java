/*
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

package org.apache.metron.integration;

import java.io.Closeable;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.stellar.common.configuration.ConfigurationsUtils;


/**
 * Closable wrapper around ZKServerComponent so it can be cleanly used in a resource protection block
 */
public class TestZKServer implements Closeable {
  private ZKServerComponent testZkServer;
  private String zookeeperUrl;

  /**
   * BiConsuming interface that allows Exceptions to be thrown
   */
  @FunctionalInterface
  public interface ThrowingBiConsumer<T, U> {
    void accept(T t, U u) throws Exception;
  }

  /**
   * Utility method to allow lambdas to automatically be fed a started Zookeeper client and server
   * which are automatically cleaned up after the lambda finishes running or throws an exception
   * @param testFunc  Lambda containing the code to run with the zookeeper client/server
   * @throws Exception Any exceptions thrown by the 'testFunc' lambda will bubble up the call chain
   */
  static public void runWithZK(ThrowingBiConsumer<TestZKServer,CuratorFramework> testFunc) throws Exception {
    try (TestZKServer zkServer = new TestZKServer();
         CuratorFramework zkClient = zkServer.newClient()) {
      zkClient.start();
      testFunc.accept(zkServer, zkClient);
    }
  }

  public TestZKServer() throws UnableToStartException {
    testZkServer = new ZKServerComponent();
    testZkServer.start();
    zookeeperUrl = testZkServer.getConnectionString();
  }

  public String getZookeeperUrl() {
    return zookeeperUrl;
  }

  /**
   * Create a new zookeeper client configured to use our test Zookeeper server
   * @return CuratorFramework client
   */
  public CuratorFramework newClient() {
    return ConfigurationsUtils.getClient(zookeeperUrl);
  }

  @Override
  public void close() {
    testZkServer.stop();
    testZkServer.reset();
  }
}
