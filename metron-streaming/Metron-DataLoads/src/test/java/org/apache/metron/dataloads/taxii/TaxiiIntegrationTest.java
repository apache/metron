package org.apache.metron.dataloads.taxii;

import com.google.common.base.Splitter;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.metron.dataloads.extractor.stix.StixExtractor;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelConverter;
import org.apache.metron.integration.util.UnitTestHelper;
import org.apache.metron.integration.util.mock.MockHTable;
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
    }

    @Test
    public void testTaxii() throws Exception {
        /**
         {
            "endpoint" : "http://localhost:8282/taxii-discovery-service"
           ,"type" : "DISCOVER"
           ,"collection" : "guest.Abuse_ch"
           ,"tableMap" : {
                    "DomainName:FQDN" : "malicious_domain:cf"
                   ,"Address:IPV_4_ADDR" : "malicious_address:cf"
                         }
         }
         */
        String taxiiConnectionConfig = "{\n" +
                "            \"endpoint\" : \"http://localhost:8282/taxii-discovery-service\"\n" +
                "           ,\"type\" : \"DISCOVER\"\n" +
                "           ,\"collection\" : \"guest.Abuse_ch\"\n" +
                "           ,\"tableMap\" : {\n" +
                "                    \"DomainName:FQDN\" : \"malicious_domain:cf\"\n" +
                "                   ,\"Address:IPV_4_ADDR\" : \"malicious_address:cf\"\n" +
                "                         }\n" +
                "         }";
        final MockHTable.Provider provider = new MockHTable.Provider();
        final Configuration config = HBaseConfiguration.create();
        TaxiiHandler handler = new TaxiiHandler(TaxiiConnectionConfig.load(taxiiConnectionConfig), new StixExtractor(), config ) {
            @Override
            protected synchronized HTableInterface createHTable(TableInfo tableInfo) throws IOException {
                return provider.addToCache(tableInfo.getTableName(), tableInfo.getColumnFamily());
            }
        };
        UnitTestHelper.verboseLogging();
        handler.run();
        Set<String> maliciousDomains;
        {
            MockHTable table = (MockHTable) provider.getTable(config, "malicious_domain");
            maliciousDomains = getIndicators(table.getPutLog(), "cf");
        }
        Assert.assertTrue(maliciousDomains.contains("www.office-112.com"));
        Assert.assertEquals(numStringsMatch(MockTaxiiService.pollMsg, "DomainNameObj:Value condition=\"Equals\""), maliciousDomains.size());
        Set<String> maliciousAddresses;
        {
            MockHTable table = (MockHTable) provider.getTable(config, "malicious_address");
            maliciousAddresses= getIndicators(table.getPutLog(), "cf");
        }
        Assert.assertTrue(maliciousAddresses.contains("94.102.53.142"));
        Assert.assertEquals(numStringsMatch(MockTaxiiService.pollMsg, "AddressObj:Address_Value condition=\"Equal\""), maliciousAddresses.size());
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

    private static Set<String> getIndicators(Iterable<Put> puts, String cf) throws IOException {
        ThreatIntelConverter converter = new ThreatIntelConverter();
        Set<String> ret = new HashSet<>();
        for(Put p : puts) {
            ret.add(converter.fromPut(p, cf).getKey().indicator);
        }
        return ret;
    }
}
