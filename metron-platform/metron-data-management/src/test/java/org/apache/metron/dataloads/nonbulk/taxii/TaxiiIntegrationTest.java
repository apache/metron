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

import com.google.common.base.Splitter;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.PosixParser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.dataloads.extractor.TransformFilterExtractorDecorator;
import org.apache.metron.dataloads.extractor.stix.StixExtractor;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.hbase.mock.MockHTable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaxiiIntegrationTest {

    @BeforeAll
    public static void setup() throws IOException {
        MockTaxiiService.start(8282);
    }

    @AfterAll
    public static void teardown() {
        MockTaxiiService.shutdown();
        MockHBaseTableProvider.clear();
    }

    /**
         {
            "endpoint" : "http://localhost:8282/taxii-discovery-service"
           ,"type" : "DISCOVER"
           ,"collection" : "guest.Abuse_ch"
           ,"table" : "threat_intel"
           ,"columnFamily" : "cf"
           ,"allowedIndicatorTypes" : [ "domainname:FQDN", "address:IPV_4_ADDR" ]
         }
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
        assertEquals(extractorJson,TaxiiLoader.TaxiiOptions.EXTRACTOR_CONFIG.get(cli).trim());
        assertEquals(connectionConfig, TaxiiLoader.TaxiiOptions.CONNECTION_CONFIG.get(cli).trim());
        assertEquals(beginTime,TaxiiLoader.TaxiiOptions.BEGIN_TIME.get(cli).trim());
        assertEquals(enrichmentJson,TaxiiLoader.TaxiiOptions.ENRICHMENT_CONFIG.get(cli).trim());
        assertEquals(timeInteval,TaxiiLoader.TaxiiOptions.TIME_BETWEEN_POLLS.get(cli).trim());
        assertEquals(log4jProperty, TaxiiLoader.TaxiiOptions.LOG4J_PROPERTIES.get(cli).trim());
    }

    @Test
    public void testTaxii() throws Exception {

        final MockHBaseTableProvider provider = new MockHBaseTableProvider();
        final Configuration config = HBaseConfiguration.create();
        Extractor extractor = new TransformFilterExtractorDecorator(new StixExtractor());
        TaxiiHandler handler = new TaxiiHandler(TaxiiConnectionConfig.load(taxiiConnectionConfig), extractor, config ) {
            @Override
            protected synchronized Table createHTable(String tableInfo) throws IOException {
                return provider.addToCache("threat_intel", "cf");
            }
        };
        //UnitTestHelper.verboseLogging();
        handler.run();
        Set<String> maliciousDomains;
        {
            MockHTable table = (MockHTable) provider.getTable(config, "threat_intel");
            maliciousDomains = getIndicators("domainname:FQDN", table.getPutLog(), "cf");
        }
        assertTrue(maliciousDomains.contains("www.office-112.com"));
        assertEquals(numStringsMatch(MockTaxiiService.pollMsg, "DomainNameObj:Value condition=\"Equals\""), maliciousDomains.size());
        Set<String> maliciousAddresses;
        {
            MockHTable table = (MockHTable) provider.getTable(config, "threat_intel");
            maliciousAddresses= getIndicators("address:IPV_4_ADDR", table.getPutLog(), "cf");
        }
        assertTrue(maliciousAddresses.contains("94.102.53.142"));
        assertEquals(numStringsMatch(MockTaxiiService.pollMsg, "AddressObj:Address_Value condition=\"Equal\""), maliciousAddresses.size());
        MockHBaseTableProvider.clear();

        // Ensure that the handler can be run multiple times without connection issues.
        handler.run();
    }

    private static int numStringsMatch(String xmlBundle, String text) {
        int cnt = 0;
        for(String line : Splitter.on("\n").split(xmlBundle)) {
            if(line.contains(text)) {
                cnt++;
            }
        }
        return cnt;
    }

    private static Set<String> getIndicators(String indicatorType, Iterable<Put> puts, String cf) throws IOException {
        EnrichmentConverter converter = new EnrichmentConverter();
        Set<String> ret = new HashSet<>();
        for(Put p : puts) {
            LookupKV<EnrichmentKey, EnrichmentValue> kv = converter.fromPut(p, cf);
            if (kv.getKey().type.equals(indicatorType)) {
                ret.add(kv.getKey().indicator);
            }
        }
        return ret;
    }
}
