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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.log4j.PropertyConfigurator;
import org.apache.metron.dataloads.bulk.ThreatIntelBulkLoader;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.dataloads.extractor.inputformat.WholeFileFormat;
import org.apache.metron.dataloads.nonbulk.flatfile.SimpleEnrichmentFlatFileLoader;
import org.apache.metron.dataloads.hbase.mr.HBaseUtil;
import org.apache.metron.enrichment.converter.HbaseConverter;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.common.utils.JSONUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
   }

    @After
    public void teardown() throws Exception {
        HBaseUtil.INSTANCE.teardown(testUtil);
    }

    @Test
    public void testCommandLine() throws Exception {
        Configuration conf = HBaseConfiguration.create();

        String[] argv = {"-c cf", "-t enrichment", "-e extractor.json", "-n enrichment_config.json", "-l log4j", "-i input.csv"};
        String[] otherArgs = new GenericOptionsParser(conf, argv).getRemainingArgs();

        CommandLine cli = SimpleEnrichmentFlatFileLoader.LoadOptions.parse(new PosixParser(), otherArgs);
        Assert.assertEquals(extractorJson,SimpleEnrichmentFlatFileLoader.LoadOptions.EXTRACTOR_CONFIG.get(cli).trim());
        Assert.assertEquals(cf, SimpleEnrichmentFlatFileLoader.LoadOptions.HBASE_CF.get(cli).trim());
        Assert.assertEquals(tableName,SimpleEnrichmentFlatFileLoader.LoadOptions.HBASE_TABLE.get(cli).trim());
        Assert.assertEquals(enrichmentJson,SimpleEnrichmentFlatFileLoader.LoadOptions.ENRICHMENT_CONFIG.get(cli).trim());
        Assert.assertEquals(csvFile,SimpleEnrichmentFlatFileLoader.LoadOptions.INPUT.get(cli).trim());
        Assert.assertEquals(log4jProperty, SimpleEnrichmentFlatFileLoader.LoadOptions.LOG4J_PROPERTIES.get(cli).trim());
    }

    @Test
    public void test() throws Exception {

        Assert.assertNotNull(testTable);
        String contents = "google.com,1,foo";

        EnrichmentConverter converter = new EnrichmentConverter();
        ExtractorHandler handler = ExtractorHandler.load(extractorConfig);
        Extractor e = handler.getExtractor();
        File file = new File (contents);
        SimpleEnrichmentFlatFileLoader loader = new SimpleEnrichmentFlatFileLoader();
        testTable.put(loader.extract(contents, e, cf, converter));

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

}
