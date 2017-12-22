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

package org.apache.metron.management;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;
import static org.apache.metron.stellar.dsl.Context.Capabilities.ZOOKEEPER_CLIENT;

import java.util.HashMap;
import java.util.Properties;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.stellar.dsl.Context;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ValidationFunctionsIntegrationTest extends BaseIntegrationTest {

  private static ZKServerComponent zkServerComponent;
  private static ComponentRunner runner;

  /**
   * {
   * "parserClassName": "org.apache.metron.parsers.GrokParser",
   * "sensorTopic": "foo",
   * "parserConfig": {
   * "grokPath": "/patterns/foo",
   * "patternLabel": "FOO_DELIMITED",
   * "timestampField": "timestamp"
   * },
   * "fieldTransformations" : [
   * {
   * "transformation" : "STELLAR"
   * ,"output" : [ "full_hostname", "domain_without_subdomains" ]
   * ,"config" : {
   * "full_hostname" : "URL_TO_HOST(url)"
   * ,"domain_without_subdomains" : "DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"
   * }
   * }
   * ]
   * }
   */
  @Multiline
  private static String config;

  /**
   * {
   * "parserClassName": "org.apache.metron.parsers.GrokParser",
   * "sensorTopic": "bar",
   * "parserConfig": {
   * "grokPath": "/patterns/bar",
   * "patternLabel": "BAR_DELIMITED",
   * "timestampField": "timestamp"
   * },
   * "fieldTransformations" : [
   * {
   * "transformation" : "STELLAR"
   * ,"output" : [ "full_hostname", "domain_without_subdomains" ]
   * ,"config" : {
   * "full_hostname" : "URL_TO_HOST(url)="
   * ,"domain_without_subdomains" : "DOMAIN_REMOVE_SUBDOMAINS(full_hostname)="
   * }
   * }
   * ]
   * }
   */
  @Multiline
  private static String badConfig;

  /**
   *╔═════════════════════════════════════════════════════════════════════════════════════════╤══════════════════════════════════════════╤═════════════════════════════════════════════════════╤═══════╗
   *║ PATH                                                                                    │ RULE                                     │ ERROR                                               │ VALID ║
   *╠═════════════════════════════════════════════════════════════════════════════════════════╪══════════════════════════════════════════╪═════════════════════════════════════════════════════╪═══════╣
   *║ Apache Metron/PARSER/bar/fieldTransformations/0/Field Mapping/full_hostname             │ URL_TO_HOST(url)=                        │ Syntax error @ 1:16 token recognition error at: '=' │ false ║
   *╟─────────────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────┼─────────────────────────────────────────────────────┼───────╢
   *║ Apache Metron/PARSER/bar/fieldTransformations/0/Field Mapping/domain_without_subdomains │ DOMAIN_REMOVE_SUBDOMAINS(full_hostname)= │ Syntax error @ 1:39 token recognition error at: '=' │ false ║
   *╟─────────────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────┼─────────────────────────────────────────────────────┼───────╢
   *║ Apache Metron/PARSER/foo/fieldTransformations/0/Field Mapping/full_hostname             │ URL_TO_HOST(url)                         │                                                     │ true  ║
   *╟─────────────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────┼─────────────────────────────────────────────────────┼───────╢
   *║ Apache Metron/PARSER/foo/fieldTransformations/0/Field Mapping/domain_without_subdomains │ DOMAIN_REMOVE_SUBDOMAINS(full_hostname)  │                                                     │ true  ║
   *╚═════════════════════════════════════════════════════════════════════════════════════════╧══════════════════════════════════════════╧═════════════════════════════════════════════════════╧═══════╝
   **/
  @Multiline
  private static String expectedOutput;


  /**
   *╔════════════════════════════════════════════════╤══════════════════════════════════════════╤═════════════════════╤═══════╗
   *║ PATH                                           │ RULE                                     │ ERROR               │ VALID ║
   *╠════════════════════════════════════════════════╪══════════════════════════════════════════╪═════════════════════╪═══════╣
   *║ Apache                                         │ URL_TO_HOST(url)=                        │ Syntax error @ 1:16 │ false ║
   *║ Metron/PARSER/bar/fieldTransformations/0/Field │                                          │ token recognition   │       ║
   *║ Mapping/full_hostname                          │                                          │ error at: '='       │       ║
   *╟────────────────────────────────────────────────┼──────────────────────────────────────────┼─────────────────────┼───────╢
   *║ Apache                                         │ DOMAIN_REMOVE_SUBDOMAINS(full_hostname)= │ Syntax error @ 1:39 │ false ║
   *║ Metron/PARSER/bar/fieldTransformations/0/Field │                                          │ token recognition   │       ║
   *║ Mapping/domain_without_subdomains              │                                          │ error at: '='       │       ║
   *╟────────────────────────────────────────────────┼──────────────────────────────────────────┼─────────────────────┼───────╢
   *║ Apache                                         │ URL_TO_HOST(url)                         │                     │ true  ║
   *║ Metron/PARSER/foo/fieldTransformations/0/Field │                                          │                     │       ║
   *║ Mapping/full_hostname                          │                                          │                     │       ║
   *╟────────────────────────────────────────────────┼──────────────────────────────────────────┼─────────────────────┼───────╢
   *║ Apache                                         │ DOMAIN_REMOVE_SUBDOMAINS(full_hostname)  │                     │ true  ║
   *║ Metron/PARSER/foo/fieldTransformations/0/Field │                                          │                     │       ║
   *║ Mapping/domain_without_subdomains              │                                          │                     │       ║
   *╚════════════════════════════════════════════════╧══════════════════════════════════════════╧═════════════════════╧═══════╝
   */
  @Multiline
  private static String expectedWrappedOutput;

  @BeforeClass
  public static void setupZookeeper() throws Exception {
    Properties properties = new Properties();
    zkServerComponent = getZKServerComponent(properties);
    zkServerComponent.start();
  }

  @AfterClass
  public static void tearDownZookeeper() {
    zkServerComponent.stop();
  }


  @Test
  public void test() throws Exception {
    try( CuratorFramework client = ConfigurationsUtils.getClient(zkServerComponent.getConnectionString())) {
      client.start();
      setupConfiguration(client);
      Context context = new Context.Builder().with(ZOOKEEPER_CLIENT, () -> client).build();
      Object out = run("VALIDATE_STELLAR_RULE_CONFIGS()", new HashMap<>(), context);
      Assert.assertEquals(expectedOutput, out);
    }

  }
  @Test
  public void testWrapped() throws Exception {
    try( CuratorFramework client = ConfigurationsUtils.getClient(zkServerComponent.getConnectionString())) {
      client.start();
      setupConfiguration(client);
      Context context = new Context.Builder().with(ZOOKEEPER_CLIENT, () -> client).build();
      Object out = run("VALIDATE_STELLAR_RULE_CONFIGS(20)", new HashMap<>(), context);
      Assert.assertEquals(expectedWrappedOutput, out);
    }

  }
  private void setupConfiguration(CuratorFramework client) throws Exception {

    // upload squid configuration to zookeeper
    ConfigurationsUtils.writeSensorParserConfigToZookeeper("foo",
        config.getBytes(), client);

    ConfigurationsUtils.writeSensorParserConfigToZookeeper("bar",
        badConfig.getBytes(), client);

  }

}