/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.common.configuration;

import java.util.LinkedList;
import java.util.List;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StellarStatementReporterTest {

  private TestingServer testZkServer;
  private String zookeeperUrl;
  private CuratorFramework client;
  private List<String> expected;

  @Before
  public void setUp() throws Exception {
    testZkServer = new TestingServer(true);
    zookeeperUrl = testZkServer.getConnectString();
    client = ConfigurationsUtils.getClient(zookeeperUrl);
    expected = new LinkedList<>();
    expected.add("Apache Metron->PARSER->squid->full_hostname");
    expected.add("Apache Metron->PARSER->squid->domain_without_subdomains");
    expected.add("Apache Metron->ENRICHMENT->snort->THREAT_TRIAGE->(default)->rule");
    expected.add("Apache Metron->PROFILER->example2->only_if");
    expected.add("Apache Metron->PROFILER->example2->init->num_dns");
    expected.add("Apache Metron->PROFILER->example2->init->num_http");
    expected.add("Apache Metron->PROFILER->example2->update->num_dns");
    expected.add("Apache Metron->PROFILER->example2->update->num_http");
    expected.add("Apache Metron->PROFILER->example2->profile_result->profile_expression");
    expected.add("Apache Metron->PROFILER->example1->only_if");
    expected.add("Apache Metron->PROFILER->example1->init->total_bytes");
    expected.add("Apache Metron->PROFILER->example1->update->total_bytes");
    expected.add("Apache Metron->PROFILER->example1->profile_result->profile_expression");
    expected.add("Apache Metron->PROFILER->percentiles->only_if");
    expected.add("Apache Metron->PROFILER->percentiles->init->s");
    expected.add("Apache Metron->PROFILER->percentiles->update->s");
    expected.add("Apache Metron->PROFILER->percentiles->profile_result->profile_expression");
    client.start();
  }

  @After
  public void tearDown() throws Exception {
    client.close();
    testZkServer.close();
    testZkServer.stop();
  }


  /**
   * {
   * "parserClassName": "org.apache.metron.parsers.GrokParser",
   * "sensorTopic": "squid",
   * "parserConfig": {
   * "grokPath": "/patterns/squid",
   * "patternLabel": "SQUID_DELIMITED",
   * "timestampField": "timestamp"
   * },
   * "fieldTransformations" : [
   * {
   * "transformation" : "STELLAR",
   * "output" : [ "full_hostname", "domain_without_subdomains" ],
   * "config" : {
   * "full_hostname" : "URL_TO_HOST(url)",
   * "domain_without_subdomains" : "DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"
   * }
   * }
   * ]
   * }
   */
  @Multiline
  public static String squidParserConfig;

  /**
   * {
   * "parserClassName": "org.apache.metron.parsers.GrokParser",
   * "sensorTopic": "squid",
   * "parserConfig": {
   * "grokPath": "/patterns/squid",
   * "patternLabel": "SQUID_DELIMITED",
   * "timestampField": "timestamp"
   * },
   * "fieldTransformations" : [
   * {
   * "transformation" : "STELLAR",
   * "output" : [ "full_hostname", "domain_without_subdomains" ],
   * "config" : {
   * "full_hostname" : "URL_TO_HOST(url)",
   * "domain_without_subdomains" : "DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"
   * }
   * }
   */
  @Multiline
  public static String badParserConfig;


  /**
   * {
   * "enrichment" : {
   * "fieldMap":
   * {
   * "geo": ["ip_dst_addr", "ip_src_addr"],
   * "host": ["host"]
   * }
   * },
   * "threatIntel" : {
   * "fieldMap":
   * {
   * "hbaseThreatIntel": ["ip_src_addr", "ip_dst_addr"]
   * },
   * "fieldToTypeMap":
   * {
   * "ip_src_addr" : ["malicious_ip"],
   * "ip_dst_addr" : ["malicious_ip"]
   * },
   * "triageConfig" : {
   * "riskLevelRules" : [
   * {
   * "rule" : "not(IN_SUBNET(ip_dst_addr, '192.168.0.0/24'))",
   * "score" : 10
   * }
   * ],
   * "aggregator" : "MAX"
   * }
   * }
   * }
   */
  @Multiline
  public static String snortEnrichmentConfig;

  /**
   * {
   * "enrichment" : {
   * "fieldMap":
   * {
   * "geo": ["ip_dst_addr", "ip_src_addr"],
   * "host": ["host"]
   * }
   * },
   * "threatIntel" : {
   * "fieldMap":
   * {
   * "hbaseThreatIntel": ["ip_src_addr", "ip_dst_addr"]
   * },
   * "fieldToTypeMap":
   * {
   * "ip_src_addr" : ["malicious_ip"],
   * "ip_dst_addr" : ["malicious_ip"]
   * },
   * "triageConfig" : {
   * "riskLevelRules" : [
   * {
   * "rule" : "not(IN_SUBNET(ip_dst_addr, '192.168.0.0/24')))",
   * "score" : 10
   * }
   * ],
   * "aggregator" : "MAX"
   * }
   * }
   * }
   */
  @Multiline
  public static String badEnrichmentConfig;

  /**
   * {
   * "profiles": [
   * {
   * "profile": "example2",
   * "foreach": "ip_src_addr",
   * "onlyif": "protocol == 'DNS' or protocol == 'HTTP'",
   * "init": {
   * "num_dns": 1.0,
   * "num_http": 1.0
   * },
   * "update": {
   * "num_dns": "num_dns + (if protocol == 'DNS' then 1 else 0)",
   * "num_http": "num_http + (if protocol == 'HTTP' then 1 else 0)"
   * },
   * "result": "num_dns / num_http"
   * },
   * {
   * "profile": "example1",
   * "foreach": "ip_src_addr",
   * "onlyif": "protocol == 'HTTP'",
   * "init": {
   * "total_bytes": 0.0
   * },
   * "update": {
   * "total_bytes": "total_bytes + bytes_in"
   * },
   * "result": "total_bytes",
   * "expires": 30
   * },
   * {
   * "profile": "percentiles",
   * "foreach": "ip_src_addr",
   * "onlyif": "protocol == 'HTTP'",
   * "init":   { "s": "STATS_INIT(100)" },
   * "update": { "s": "STATS_ADD(s, length)" },
   * "result": "STATS_PERCENTILE(s, 0.7)"
   * }
   * ]
   * }
   */
  @Multiline
  public static String profilerConfig;

  @Test
  public void testVistVisitsAllConfiguredNodes() throws Exception {

    ConfigurationsUtils
        .writeSensorParserConfigToZookeeper("squid", squidParserConfig.getBytes(), zookeeperUrl);

    ConfigurationsUtils
        .writeSensorEnrichmentConfigToZookeeper("snort", snortEnrichmentConfig.getBytes(),
            zookeeperUrl);
    ConfigurationsUtils.writeProfilerConfigToZookeeper(profilerConfig.getBytes(), client);

    StellarStatementReporter stellarStatementReporter = new StellarStatementReporter();
    stellarStatementReporter.vist(client, (names, statement) -> {
      String name = String.join("->", names);
      Assert.assertTrue(expected.contains(name));
      expected.remove(name);
    }, (names, error) -> Assert.assertTrue("Should not get errors", false));

    Assert.assertTrue(expected.isEmpty());
  }

  @Test
  public void testErrorCalledWithBadParserConfig() throws Exception {
    // calling this way to avoid getting the serialize error in ConfigurationUtils instead of visit
    ConfigurationsUtils.writeToZookeeper(ConfigurationType.PARSER.getZookeeperRoot() + "/" + "squid", badParserConfig.getBytes(),client);
    StellarStatementReporter stellarStatementReporter = new StellarStatementReporter();
    stellarStatementReporter.vist(client, (names, statement) -> {
      Assert.assertTrue("This should have failed", false);
    }, (names, error) -> {
      Assert.assertTrue("Should  get errors", true);
    });
  }

  @Test(expected = RuntimeException.class)
  public void testExceptionThrownGetsBadThreatConfig() throws Exception{
    ConfigurationsUtils
        .writeSensorEnrichmentConfigToZookeeper("snort", badEnrichmentConfig.getBytes(),
            zookeeperUrl);
    StellarStatementReporter stellarStatementReporter = new StellarStatementReporter();
    stellarStatementReporter.vist(client, (names, statement) -> {
      Assert.assertTrue("This should have failed", false);
    }, (names, error) -> Assert.assertTrue("Should not get errors", false));
  }
}