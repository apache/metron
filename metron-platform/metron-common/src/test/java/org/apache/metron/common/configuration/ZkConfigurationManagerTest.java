/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.common.configuration;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.CloseableUtils;
import org.apache.metron.common.configuration.manager.ZkConfigurationManager;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.apache.metron.common.configuration.ConfigurationType.GLOBAL;
import static org.apache.metron.common.configuration.ConfigurationType.PROFILER;

/**
 * Tests the ZkConfigurationManager.
 */
public class ZkConfigurationManagerTest {

  TestingServer server;
  CuratorFramework client;
  ZkConfigurationManager manager;

  @Before
  public void setup() throws Exception {
    server = new TestingServer();
    server.start();

    client = CuratorFrameworkFactory.newClient(server.getConnectString(), new ExponentialBackoffRetry(1000, 3));
    client.start();
  }

  @After
  public void tearDown() throws Exception {
    CloseableUtils.closeQuietly(manager);
    CloseableUtils.closeQuietly(client);
    CloseableUtils.closeQuietly(server);
  }

  /**
   * By writing to zookeeper BEFORE we open the ConfigurationManager, we are testing that
   * initialization of the values occurs.
   */
  @Test
  public void testInitialize() throws Exception {

    // write the configuration to zookeeper
    final Map<String, Object> expected = Collections.singletonMap("key", 22);
    writeToZookeeper(GLOBAL.getZookeeperRoot(), expected);

    // create the manager
    manager = new ZkConfigurationManager(client)
            .with(GLOBAL.getZookeeperRoot())
            .open();

    // validate
    Optional<Map> actual = manager.get(GLOBAL.getZookeeperRoot(), Map.class);
    assertTrue(actual.isPresent());
    assertEquals(expected, actual.get());
  }

  /**
   * By writing to zookeeper BEFORE we open the ConfigurationManager, we are testing that
   * initialization of the values occurs.
   */
  @Test
  public void testInitializeWithTwoPaths() throws Exception {

    // write global configuration to zookeeper
    final Map<String, Object> expectedGlobal = Collections.singletonMap("key", 22);
    writeToZookeeper(GLOBAL.getZookeeperRoot(), expectedGlobal);

    // write profiler configuration to zookeeper
    ProfilerConfig expectedProfiler = new ProfilerConfig();
    writeToZookeeper(PROFILER.getZookeeperRoot(), expectedProfiler);

    // create the manager
    manager = new ZkConfigurationManager(client)
            .with(GLOBAL.getZookeeperRoot())
            .with(PROFILER.getZookeeperRoot())
            .open();

    {
      // validate global configuration
      Optional<Map> actual = manager.get(GLOBAL.getZookeeperRoot(), Map.class);
      assertTrue(actual.isPresent());
      assertEquals(expectedGlobal, actual.get());
    }
    {
      // validate the profiler configuration
      Optional<ProfilerConfig> actual = manager.get(PROFILER.getZookeeperRoot(), ProfilerConfig.class);
      assertTrue(actual.isPresent());
      assertEquals(expectedProfiler, actual.get());
    }
  }

  /**
   * By writing to zookeeper AFTER we open the ConfigurationManager, we are testing that
   * the manager can update itself and remain in-sync.
   */
  @Test
  public void testUpdate() throws Exception {

    // create the manager
    manager = new ZkConfigurationManager(client)
            .with(GLOBAL.getZookeeperRoot())
            .open();

    {
      // ensure no configuration defined yet
      Optional<String> value = manager.get(GLOBAL.getZookeeperRoot(), String.class);
      assertFalse(value.isPresent());
    }

    // write the global configuration to zookeeper
    final Map<String, Object> expected = Collections.singletonMap("key", 22);
    writeToZookeeper(GLOBAL.getZookeeperRoot(), expected);

    // wait until the 'update' takes
    waitOrTimeout(() -> {
      boolean result = false;
      try {
        result = manager.get(GLOBAL.getZookeeperRoot(), Map.class).isPresent();
      } catch(Exception e) {
        throw new RuntimeException(e);
      }
      return result;
    }, timeout(seconds(90)));

    {
      // validate
      Optional<Map> actual = manager.get(GLOBAL.getZookeeperRoot(), Map.class);
      assertTrue(actual.isPresent());
      assertEquals(expected, actual.get());
    }
  }


  /**
   * If the path is deleted from Zookeeper, the configuration value should be removed.
   */
  @Test
  public void testDelete() throws Exception {

    // write the configuration to zookeeper
    final Map<String, Object> expected = Collections.singletonMap("key", 22);
    writeToZookeeper(GLOBAL.getZookeeperRoot(), expected);

    // create the manager
    manager = new ZkConfigurationManager(client)
            .with(GLOBAL.getZookeeperRoot())
            .open();

    {
      // ensure the value is in zookeeper
      Optional<Map> actual = manager.get(GLOBAL.getZookeeperRoot(), Map.class);
      assertTrue(actual.isPresent());
      assertEquals(expected, actual.get());
    }

    // delete the path from zookeeper
    deleteInZookeeper(GLOBAL.getZookeeperRoot());

    // wait until the 'delete' takes
    waitOrTimeout(() -> {
      boolean result = false;
      try {
        result = !manager.get(GLOBAL.getZookeeperRoot(), Map.class).isPresent();
      } catch(Exception e) {
        throw new RuntimeException(e);
      }
      return result;
    }, timeout(seconds(90)));

    {
      // validate - the configuration value should have been removed
      Optional<Map> actual = manager.get(GLOBAL.getZookeeperRoot(), Map.class);
      assertFalse(actual.isPresent());
    }
  }

  @Test
  public void testMissing() throws Exception {

    // setup
    final String path = "/path/does/not/exist";
    manager = new ZkConfigurationManager(client)
            .with(path)
            .open();

    // validate
    Optional<String> value = manager.get(path, String.class);
    assertFalse(value.isPresent());
  }

  /**
   * Any paths not specified via 'with(path)' should be ignored.
   */
  @Test
  public void testIgnorePaths() throws Exception {

    // write the configuration to zookeeper
    final Map<String, Object> expected = Collections.singletonMap("key", 22);
    writeToZookeeper(GLOBAL.getZookeeperRoot(), expected);

    // create the manager - do not call with(...)
    manager = new ZkConfigurationManager(client).open();

    // validate
    Optional<Map> actual = manager.get(GLOBAL.getZookeeperRoot(), Map.class);
    assertFalse(actual.isPresent());
  }

  /**
   * Write test data to Zookeeper.
   * @param zkPath The path in Zookeeper to write to.
   * @param value The value to write.
   */
  private void writeToZookeeper(String zkPath, Object value) throws Exception {
    byte[] serialized = JSONUtils.INSTANCE.toJSON(value);
    client.create()
            .creatingParentsIfNeeded()
            .forPath(zkPath, serialized);
  }

  /**
   * Deletes data stored in Zookeeper.
   * @param zkPath The path to delete.
   */
  private void deleteInZookeeper(String zkPath) throws Exception {
    client.delete()
            .deletingChildrenIfNeeded()
            .forPath(zkPath);
  }
}
