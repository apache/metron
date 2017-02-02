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
package org.apache.metron.dataloads.nonbulk.flatfile;

import com.google.common.collect.ImmutableList;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.PosixParser;
import org.apache.curator.test.TestingServer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.dataloads.hbase.mr.HBaseUtil;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class SimpleEnrichmentFlatFileLoaderTest {

  private HBaseTestingUtility testUtil;

  /** The test table. */
  private HTable testTable;
  private String tableName = "enrichment";
  private String cf = "cf";
  private String csvFile="input.csv";
  private String extractorJson = "extractor.json";
  private String enrichmentJson = "enrichment_config.json";
  private String log4jProperty = "log4j";

  private TestingServer testZkServer;
  private String zookeeperUrl;

  Configuration config = null;
  /**
   {
      "config" : {
        "columns" : {
          "host" : 0,
          "meta" : 2
                    },
        "indicator_column" : "host",
        "separator" : ",",
        "type" : "enrichment"
                 },
      "extractor" : "CSV"
   }
   */
  @Multiline
  private static String extractorConfig;

  @Before
  public void setup() throws Exception {
    Map.Entry<HBaseTestingUtility, Configuration> kv = HBaseUtil.INSTANCE.create(true);
    config = kv.getValue();
    testUtil = kv.getKey();
    testTable = testUtil.createTable(Bytes.toBytes(tableName), Bytes.toBytes(cf));
    testZkServer = new TestingServer(true);
    zookeeperUrl = testZkServer.getConnectString();
  }

  @After
  public void teardown() throws Exception {
    HBaseUtil.INSTANCE.teardown(testUtil);
  }

  @Test
  public void testCommandLine() throws Exception {
    Configuration conf = HBaseConfiguration.create();

    String[] argv = { "-c cf", "-t enrichment"
            , "-e extractor.json", "-n enrichment_config.json"
            , "-l log4j", "-i input.csv"
            , "-p 2", "-b 128"
    };
    String[] otherArgs = new GenericOptionsParser(conf, argv).getRemainingArgs();

    CommandLine cli = SimpleEnrichmentFlatFileLoader.LoadOptions.parse(new PosixParser(), otherArgs);
    Assert.assertEquals(extractorJson,SimpleEnrichmentFlatFileLoader.LoadOptions.EXTRACTOR_CONFIG.get(cli).trim());
    Assert.assertEquals(cf, SimpleEnrichmentFlatFileLoader.LoadOptions.HBASE_CF.get(cli).trim());
    Assert.assertEquals(tableName,SimpleEnrichmentFlatFileLoader.LoadOptions.HBASE_TABLE.get(cli).trim());
    Assert.assertEquals(enrichmentJson,SimpleEnrichmentFlatFileLoader.LoadOptions.ENRICHMENT_CONFIG.get(cli).trim());
    Assert.assertEquals(csvFile,SimpleEnrichmentFlatFileLoader.LoadOptions.INPUT.get(cli).trim());
    Assert.assertEquals(log4jProperty, SimpleEnrichmentFlatFileLoader.LoadOptions.LOG4J_PROPERTIES.get(cli).trim());
    Assert.assertEquals("2", SimpleEnrichmentFlatFileLoader.LoadOptions.NUM_THREADS.get(cli).trim());
    Assert.assertEquals("128", SimpleEnrichmentFlatFileLoader.LoadOptions.BATCH_SIZE.get(cli).trim());
  }

  @Test
  public void test() throws Exception {
    Assert.assertNotNull(testTable);
    String contents = "google.com,1,foo";

    EnrichmentConverter converter = new EnrichmentConverter();
    ExtractorHandler handler = ExtractorHandler.load(extractorConfig);
    Extractor e = handler.getExtractor();
    SimpleEnrichmentFlatFileLoader loader = new SimpleEnrichmentFlatFileLoader();
    Stream<String> contentStreams = ImmutableList.of(contents).stream();
    ThreadLocal<ExtractorState> state = new ThreadLocal<ExtractorState>() {
      @Override
      protected ExtractorState initialValue() {
        return new ExtractorState(testTable, e, converter);
      }
    };
    loader.load(ImmutableList.of(contentStreams)
               , state
               , cf
               , 2
               );

    ResultScanner scanner = testTable.getScanner(Bytes.toBytes(cf));
    List<LookupKV<EnrichmentKey, EnrichmentValue>> results = new ArrayList<>();
    for(Result r : scanner) {
      results.add(converter.fromResult(r, cf));
    }
    Assert.assertEquals(1, results.size());
    Assert.assertEquals(results.get(0).getKey().indicator, "google.com");
    Assert.assertEquals(results.get(0).getKey().type, "enrichment");
    Assert.assertEquals(results.get(0).getValue().getMetadata().size(), 2);
    Assert.assertEquals(results.get(0).getValue().getMetadata().get("meta"), "foo");
    Assert.assertEquals(results.get(0).getValue().getMetadata().get("host"), "google.com");
  }

  /**
   *{
   *  "config" : {
   *    "columns" : {
   *      "domain" : 1
   *    },
   *    "value_transform" : {
   *      "domain" : "TO_UPPER(domain)"
   *    },
   *    "indicator_transform" : {
   *      "indicator" : "TO_UPPER(indicator)"
   *    },
   *    "value_filter" : "LENGTH(domain) > 0",
   *    "indicator_filter" : "LENGTH(indicator) > 0",
   *    "indicator_column" : "domain",
   *    "type" : "topdomain",
   *    "separator" : ",",
   *    "zk_quorum" : "%ZK_QUORUM%"
   *  },
   *  "extractor" : "CSV"
   *}
   */
  @Multiline
  private static String stellarExtractorConfig;

  @Test
  public void transforms_fields() throws Exception {
    Assert.assertNotNull(testTable);
    // TODO
//    ConfigurationsUtils.writeGlobalConfigToZookeeper(globalConfig, zookeeperUrl);
    String[] contents = new String[]{
            "1,google.com",
            "2,"
    };

    EnrichmentConverter converter = new EnrichmentConverter();
    ExtractorHandler handler = ExtractorHandler.load(stellarExtractorConfig.replaceAll("%ZK_QUORUM", zookeeperUrl));
    Extractor e = handler.getExtractor();
    SimpleEnrichmentFlatFileLoader loader = new SimpleEnrichmentFlatFileLoader();
    List<Put> extract = loader.extract(contents[0], e, cf, converter);
    testTable.put(extract);
    extract = loader.extract(contents[1], e, cf, converter);
    testTable.put(extract);

    ResultScanner scanner = testTable.getScanner(Bytes.toBytes(cf));
    List<LookupKV<EnrichmentKey, EnrichmentValue>> results = new ArrayList<>();
    for (Result r : scanner) {
      results.add(converter.fromResult(r, cf));
    }
    Assert.assertEquals(results.get(0).getKey().type, "topdomain");
    Assert.assertEquals(results.get(0).getKey().getIndicator(), "GOOGLE.COM");
    Assert.assertEquals(results.get(0).getValue().getMetadata().size(), 1);
    Assert.assertEquals(results.get(0).getValue().getMetadata().get("domain"), "GOOGLE.COM");
  }
}
