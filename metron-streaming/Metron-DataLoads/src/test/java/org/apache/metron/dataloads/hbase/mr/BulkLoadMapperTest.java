package org.apache.metron.dataloads.hbase.mr;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelValue;
import org.apache.metron.reference.lookup.LookupKV;
import org.apache.metron.threatintel.ThreatIntelResults;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelConverter;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelKey;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by cstella on 2/3/16.
 */
public class BulkLoadMapperTest {
    @Test
    public void testMapper() throws IOException, InterruptedException {
        /**
         {
            "config" : {
                        "columns" : {
                                "host" : 0
                                ,"meta" : 2
                                    }
                       ,"indicator_column" : "host"
                       ,"separator" : ","
                       }
            ,"extractor" : "CSV"
         }
         */
        final String extractorConfig = "{\n" +
                "            \"config\" : {\n" +
                "                        \"columns\" : [\"host:0\",\"meta:2\"]\n" +
                "                       ,\"indicator_column\" : \"host\"\n" +
                "                       ,\"separator\" : \",\"\n" +
                "                       }\n" +
                "            ,\"extractor\" : \"CSV\"\n" +
                "         }";

        final Map<ImmutableBytesWritable, Put> puts = new HashMap<>();
        BulkLoadMapper mapper = new BulkLoadMapper() {
            @Override
            protected void write(ImmutableBytesWritable key, Put value, Context context) throws IOException, InterruptedException {
                puts.put(key, value);
            }
        };
        mapper.initialize(new Configuration() {{
            set(BulkLoadMapper.COLUMN_FAMILY_KEY, "cf");
            set(BulkLoadMapper.CONFIG_KEY, extractorConfig);
            set(BulkLoadMapper.LAST_SEEN_KEY, "0");
            set(BulkLoadMapper.CONVERTER_KEY, ThreatIntelConverter.class.getName());
        }});
        {
            mapper.map(null, new Text("#google.com,1,foo"), null);
            Assert.assertTrue(puts.size() == 0);
        }
        {
            mapper.map(null, new Text("google.com,1,foo"), null);
            Assert.assertTrue(puts.size() == 1);
            ThreatIntelKey expectedKey = new ThreatIntelKey() {{
                indicator = "google.com";
            }};
            ThreatIntelConverter converter = new ThreatIntelConverter();
            Put put = puts.get(new ImmutableBytesWritable(expectedKey.toBytes()));
            Assert.assertNotNull(puts);
            LookupKV<ThreatIntelKey, ThreatIntelValue> results = converter.fromPut(put, "cf");
            Assert.assertEquals(results.getKey().indicator, "google.com");
            Assert.assertEquals(results.getValue().getMetadata().size(), 2);
            Assert.assertEquals(results.getValue().getMetadata().get("meta"), "foo");
            Assert.assertEquals(results.getValue().getMetadata().get("host"), "google.com");
        }

    }
}
