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

package org.apache.metron.dataloads.nonbulk.taxii;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.PosixParser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.dataloads.extractor.TransformFilterExtractorDecorator;
import org.apache.metron.dataloads.extractor.stix.StixExtractor;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.hbase.ColumnList;
import org.apache.metron.hbase.client.FakeHBaseClient;
import org.apache.metron.hbase.client.FakeHBaseClientFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaxiiIntegrationTest {

    @BeforeClass
    public static void setup() throws IOException {
        MockTaxiiService.start(8282);
    }

    @AfterClass
    public static void teardown() {
        MockTaxiiService.shutdown();
    }

    /**
     * {
     *   "endpoint": "http://localhost:8282/taxii-discovery-service",
     *   "type": "DISCOVER",
     *   "collection": "guest.Abuse_ch",
     *   "table": "threat_intel",
     *   "columnFamily": "cf",
     *   "allowedIndicatorTypes": [ "domainname:FQDN", "address:IPV_4_ADDR" ]
     * }
    */
    @Multiline
    static String taxiiConnectionConfig;

    private String connectionConfig = "connection.json";
    private String extractorJson = "extractor.json";
    private String enrichmentJson = "enrichment_config.json";
    private String log4jProperty = "log4j";
    private String beginTime = "04/14/2016 12:00:00";
    private String timeInteval = "10";

    @Test
    public void testCommandLine() throws Exception {
        Configuration conf = HBaseConfiguration.create();

        String[] argv = {"-c connection.json", "-e extractor.json", "-n enrichment_config.json", "-l log4j", "-p 10", "-b 04/14/2016 12:00:00"};
        String[] otherArgs = new GenericOptionsParser(conf, argv).getRemainingArgs();

        CommandLine cli = TaxiiLoader.TaxiiOptions.parse(new PosixParser(), otherArgs);
        Assert.assertEquals(extractorJson,TaxiiLoader.TaxiiOptions.EXTRACTOR_CONFIG.get(cli).trim());
        Assert.assertEquals(connectionConfig, TaxiiLoader.TaxiiOptions.CONNECTION_CONFIG.get(cli).trim());
        Assert.assertEquals(beginTime,TaxiiLoader.TaxiiOptions.BEGIN_TIME.get(cli).trim());
        Assert.assertEquals(enrichmentJson,TaxiiLoader.TaxiiOptions.ENRICHMENT_CONFIG.get(cli).trim());
        Assert.assertEquals(timeInteval,TaxiiLoader.TaxiiOptions.TIME_BETWEEN_POLLS.get(cli).trim());
        Assert.assertEquals(log4jProperty, TaxiiLoader.TaxiiOptions.LOG4J_PROPERTIES.get(cli).trim());
    }

    @Test
    public void testTaxii() throws Exception {
        // delete any existing records
        FakeHBaseClient fakeHBaseClient = new FakeHBaseClient();
        fakeHBaseClient.deleteAll();

        // setup the handler
        final Configuration config = HBaseConfiguration.create();
        Extractor extractor = new TransformFilterExtractorDecorator(new StixExtractor());
        TaxiiHandler handler = new TaxiiHandler(
                TaxiiConnectionConfig.load(taxiiConnectionConfig),
                extractor,
                new FakeHBaseClientFactory(),
                config);
        handler.run();

        // retrieve the data written to HBase by the taxii handler
        Map<EnrichmentKey, EnrichmentValue> results = new HashMap<>();
        List<FakeHBaseClient.Mutation> mutations = fakeHBaseClient.getAllPersisted();
        for(FakeHBaseClient.Mutation mutation: mutations) {

            // build the enrichment key
            EnrichmentKey key = new EnrichmentKey("taxii", "et");
            key.fromBytes(mutation.rowKey);

            // expect only 1 column
            List<ColumnList.Column> columns = mutation.columnList.getColumns();
            Assert.assertEquals(1, columns.size());
            ColumnList.Column column = columns.get(0);

            // build the enrichment value
            EnrichmentValue value = new EnrichmentValue();
            value.fromColumn(column.getQualifier(), column.getValue());
            results.put(key, value);
        }

        // validate the extracted taxii data
        Assert.assertEquals(2, results.size());
        {
            EnrichmentKey key = new EnrichmentKey("address:IPV_4_ADDR", "94.102.53.142");
            Assert.assertTrue(results.containsKey(key));
            EnrichmentValue value = results.get(key);
            Assert.assertEquals(6, value.getMetadata().size());
            Assert.assertEquals("guest.Abuse_ch", value.getMetadata().get("taxii_collection"));
            Assert.assertEquals("STIX", value.getMetadata().get("source-type"));
            Assert.assertEquals("address:IPV_4_ADDR", value.getMetadata().get("indicator-type"));
            Assert.assertEquals("taxii", value.getMetadata().get("source_type"));
            Assert.assertEquals("http://localhost:8282/taxii-data", value.getMetadata().get("taxii_url"));
        }
        {
            EnrichmentKey key = new EnrichmentKey("domainname:FQDN", "www.office-112.com");
            Assert.assertTrue(results.containsKey(key));
            EnrichmentValue value = results.get(key);
            Assert.assertEquals(6, value.getMetadata().size());
            Assert.assertEquals("guest.Abuse_ch", value.getMetadata().get("taxii_collection"));
            Assert.assertEquals("STIX", value.getMetadata().get("source-type"));
            Assert.assertEquals("domainname:FQDN", value.getMetadata().get("indicator-type"));
            Assert.assertEquals("taxii", value.getMetadata().get("source_type"));
            Assert.assertEquals("http://localhost:8282/taxii-data", value.getMetadata().get("taxii_url"));
        }

        // Ensure that the handler can be run multiple times without connection issues.
        handler.run();
    }
}
