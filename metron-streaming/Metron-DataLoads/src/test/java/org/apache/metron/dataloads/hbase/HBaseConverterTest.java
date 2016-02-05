package org.apache.metron.dataloads.hbase;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.threatintel.hbase.Converter;
import org.apache.metron.threatintel.ThreatIntelKey;
import org.apache.metron.threatintel.ThreatIntelResults;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by cstella on 2/3/16.
 */
public class HBaseConverterTest {
    ThreatIntelKey key = new ThreatIntelKey("google");
    Map<String, String> value = new HashMap<String, String>() {{
        put("foo", "bar");
        put("grok", "baz");
    }};
    Long timestamp = 7L;
    ThreatIntelResults results = new ThreatIntelResults(key, value);
    @Test
    public void testKeySerialization() {
        byte[] serialized = key.toBytes();
        ThreatIntelKey deserialized = ThreatIntelKey.fromBytes(serialized);
        Assert.assertEquals(key, deserialized);
    }

    @Test
    public void testPut() throws IOException {
        Put put = Converter.INSTANCE.toPut("cf", key, value, timestamp);
        Map.Entry<ThreatIntelResults, Long> converted= Converter.INSTANCE.fromPut(put, "cf");
        Assert.assertEquals(new AbstractMap.SimpleEntry<>(results, timestamp), converted);
    }
    @Test
    public void testResult() throws IOException {
        Result r = Converter.INSTANCE.toResult("cf", key, value, timestamp);
        Map.Entry<ThreatIntelResults, Long> converted= Converter.INSTANCE.fromResult(r, "cf");
        Assert.assertEquals(new AbstractMap.SimpleEntry<>(results, timestamp), converted);
    }

    @Test
    public void testGet() throws Exception {
        Get get = Converter.INSTANCE.toGet("cf", key);
        Assert.assertArrayEquals(key.toBytes(), get.getRow());
    }
}
