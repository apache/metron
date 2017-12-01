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
import org.apache.metron.stellar.common.utils.validation.ExpressionConfigurationHolder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StellarStatementReporterTest {

  private TestingServer testZkServer;
  private String zookeeperUrl;
  private CuratorFramework client;
  private List<String> expected;
  private List<String> expectedStatements;
  private List<String> foundStatements;
  @Before
  public void setUp() throws Exception {
    testZkServer = new TestingServer(true);
    zookeeperUrl = testZkServer.getConnectString();
    client = ConfigurationsUtils.getClient(zookeeperUrl);
    expected = new LinkedList<>();
    expected.add("Apache Metron/PARSER/squid");
    expected.add("Apache Metron/ENRICHMENT/snort");
    expected.add("Apache Metron/PROFILER/example2");
    expected.add("Apache Metron/PROFILER/example1");
    expected.add("Apache Metron/PROFILER/percentiles");

    expectedStatements = new LinkedList<>();
    expectedStatements.add("Apache Metron/PARSER/squid/fieldTransformations/0/Field Mapping/full_hostname");
    expectedStatements.add("Apache Metron/PARSER/squid/fieldTransformations/0/Field Mapping/domain_without_subdomains");
    expectedStatements.add("Apache Metron/ENRICHMENT/snort/enrichment/default/foo");
    expectedStatements.add("Apache Metron/ENRICHMENT/snort/enrichment/default/ALL_CAPS");
    expectedStatements.add("Apache Metron/ENRICHMENT/snort/threatIntel/triageConfig/riskLevelRules/0/rule");
    expectedStatements.add("Apache Metron/PROFILER/example2/onlyif");
    expectedStatements.add("Apache Metron/PROFILER/example2/init/num_dns");
    expectedStatements.add("Apache Metron/PROFILER/example2/init/num_http");
    expectedStatements.add("Apache Metron/PROFILER/example2/update/num_dns");
    expectedStatements.add("Apache Metron/PROFILER/example2/update/num_http");
    expectedStatements.add("Apache Metron/PROFILER/example2/result/profileExpressions/expression");
    expectedStatements.add("Apache Metron/PROFILER/example1/onlyif");
    expectedStatements.add("Apache Metron/PROFILER/example1/init/total_bytes");
    expectedStatements.add("Apache Metron/PROFILER/example1/update/total_bytes");
    expectedStatements.add("Apache Metron/PROFILER/example1/result/profileExpressions/expression");
    expectedStatements.add("Apache Metron/PROFILER/percentiles/onlyif");
    expectedStatements.add("Apache Metron/PROFILER/percentiles/init/s");
    expectedStatements.add("Apache Metron/PROFILER/percentiles/update/s");
    expectedStatements.add("Apache Metron/PROFILER/percentiles/result/profileExpressions/expression");

    foundStatements = new LinkedList<>();

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
   * "host": ["host"],
   * "stellar" : {
   *    "type" : "STELLAR",
   *     "config" : {
   *        "foo" : "1 + 1",
   *        "ALL_CAPS" : "TO_UPPER(source.type)"
   *      }
   *  }
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
  public void testProvideAllConfiguredNodes() throws Exception {
    ConfigurationsUtils
        .writeSensorParserConfigToZookeeper("squid", squidParserConfig.getBytes(), zookeeperUrl);

    ConfigurationsUtils
        .writeSensorEnrichmentConfigToZookeeper("snort", snortEnrichmentConfig.getBytes(),
            zookeeperUrl);
    ConfigurationsUtils.writeProfilerConfigToZookeeper(profilerConfig.getBytes(), client);

    StellarStatementReporter stellarStatementReporter = new StellarStatementReporter();
    List<ExpressionConfigurationHolder> holders = stellarStatementReporter
        .provideConfigurations(client,
            (names, error) -> Assert.assertTrue("Should not get errors", false));
    holders.forEach((h) -> {
      try {
        h.discover();
        h.visit((p,s) -> {
          foundStatements.add(p);
        },(s,e) -> {
          Assert.assertTrue(e.getMessage(),false);
        });
      } catch (IllegalAccessException e) {
        Assert.assertTrue(e.getMessage(), false);
      }
    });

    holders.forEach((h) -> expected.remove(h.getFullName()));
    Assert.assertTrue(expected.isEmpty());

    foundStatements.removeAll(expectedStatements);
    Assert.assertTrue(foundStatements.isEmpty());
  }

  @Test
  public void testErrorCalledWithBadParserConfig() throws Exception {
    // calling this way to avoid getting the serialize error in ConfigurationUtils instead of visit
    ConfigurationsUtils
        .writeToZookeeper(ConfigurationType.PARSER.getZookeeperRoot() + "/" + "squid",
            badParserConfig.getBytes(), client);
    StellarStatementReporter stellarStatementReporter = new StellarStatementReporter();
    stellarStatementReporter.provideConfigurations(client, (names, error) -> {
      Assert.assertTrue("Should  get errors", true);
    });
  }

  @Test(expected = RuntimeException.class)
  public void testExceptionThrownGetsBadThreatConfig() throws Exception {
    ConfigurationsUtils
        .writeSensorEnrichmentConfigToZookeeper("snort", badEnrichmentConfig.getBytes(),
            zookeeperUrl);
    StellarStatementReporter stellarStatementReporter = new StellarStatementReporter();
    stellarStatementReporter.provideConfigurations(client,
        (names, error) -> Assert.assertTrue("Should not get errors", false));
  }
}