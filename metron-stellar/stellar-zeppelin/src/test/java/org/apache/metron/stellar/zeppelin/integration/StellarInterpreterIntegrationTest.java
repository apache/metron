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
package org.apache.metron.stellar.zeppelin.integration;

import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.zeppelin.StellarInterpreter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.apache.metron.stellar.zeppelin.StellarInterpreterProperty.ZOOKEEPER_URL;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * An integration test for the StellarInterpreter.
 */
public class StellarInterpreterIntegrationTest extends BaseIntegrationTest {

  private StellarInterpreter interpreter;
  private InterpreterContext context;
  private Properties properties;
  private String zookeeperURL;
  private ZKServerComponent zkServer;
  private ComponentRunner runner;

  @BeforeEach
  public void setup() throws Exception {

    // a component that uploads the global configuration
    Map<String, Object> globals = new HashMap<>();
    ConfigUploadComponent configUploader = new ConfigUploadComponent()
            .withGlobals(globals);

    // create zookeeper component
    properties = new Properties();
    zkServer = getZKServerComponent(properties);

    // can only get the zookeeperUrl AFTER it has started
    zkServer.withPostStartCallback((zk) -> {
      zookeeperURL = zk.getConnectionString();
      configUploader.withZookeeperURL(zookeeperURL);
    });

    // start the integration test components
    runner = new ComponentRunner.Builder()
            .withComponent("zk", zkServer)
            .withComponent("config", configUploader)
            .build();
    runner.start();

    context = mock(InterpreterContext.class);
  }

  @AfterEach
  public void tearDown() throws Exception {
    runner.stop();
  }

  /**
   * A user should be able to define a Zookeeper URL as a property.  When this property
   * is defined, a connection to Zookeeper is created and available in the Stellar session.
   */
  @Test
  public void testOpenWithZookeeperURL() {

    // define a zookeeper URL
    Properties props = new Properties();
    props.put(ZOOKEEPER_URL.toString(), zookeeperURL);

    // open the interpreter
    interpreter = new StellarInterpreter(props);
    interpreter.open();

    // a zookeeper client should be defined
    Optional<Object> zk = interpreter.getExecutor().getContext().getCapability(Context.Capabilities.ZOOKEEPER_CLIENT, false);
    assertTrue(zk.isPresent());
  }

}
