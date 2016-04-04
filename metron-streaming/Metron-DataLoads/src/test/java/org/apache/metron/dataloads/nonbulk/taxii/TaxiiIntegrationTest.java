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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.metron.dataloads.extractor.stix.StixExtractor;
import org.apache.metron.hbase.converters.enrichment.EnrichmentConverter;
import org.apache.metron.hbase.converters.enrichment.EnrichmentKey;
import org.apache.metron.hbase.converters.enrichment.EnrichmentValue;
import org.apache.metron.integration.util.mock.MockHTable;
import org.apache.metron.reference.lookup.LookupKV;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class TaxiiIntegrationTest {

    @Before
    public void setup() throws IOException {
        MockTaxiiService.start(8282);
    }

    @After
    public void teardown() {
        MockTaxiiService.shutdown();
        MockHTable.Provider.clear();
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

    @Test
    public void testTaxii() throws Exception {

        final MockHTable.Provider provider = new MockHTable.Provider();
        final Configuration config = HBaseConfiguration.create();
        TaxiiHandler handler = new TaxiiHandler(TaxiiConnectionConfig.load(taxiiConnectionConfig), new StixExtractor(), config ) {
            @Override
            protected synchronized HTableInterface createHTable(String tableInfo) throws IOException {
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
        Assert.assertTrue(maliciousDomains.contains("www.office-112.com"));
        Assert.assertEquals(numStringsMatch(MockTaxiiService.pollMsg, "DomainNameObj:Value condition=\"Equals\""), maliciousDomains.size());
        Set<String> maliciousAddresses;
        {
            MockHTable table = (MockHTable) provider.getTable(config, "threat_intel");
            maliciousAddresses= getIndicators("address:IPV_4_ADDR", table.getPutLog(), "cf");
        }
        Assert.assertTrue(maliciousAddresses.contains("94.102.53.142"));
        Assert.assertEquals(numStringsMatch(MockTaxiiService.pollMsg, "AddressObj:Address_Value condition=\"Equal\""), maliciousAddresses.size());
        MockHTable.Provider.clear();
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
