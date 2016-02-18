package org.apache.metron.dataloads.hbase;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.hbase.converters.HbaseConverter;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelConverter;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelKey;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelValue;
import org.apache.metron.reference.lookup.LookupKV;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by cstella on 2/3/16.
 */
public class HBaseThreatIntelConverterTest {
    ThreatIntelKey key = new ThreatIntelKey("google");
    ThreatIntelValue value = new ThreatIntelValue(
    new HashMap<String, String>() {{
        put("foo", "bar");
        put("grok", "baz");
    }});
    LookupKV<ThreatIntelKey, ThreatIntelValue> results = new LookupKV(key, value);
    @Test
    public void testKeySerialization() {
        byte[] serialized = key.toBytes();

        ThreatIntelKey deserialized = new ThreatIntelKey();
        deserialized.fromBytes(serialized);
        Assert.assertEquals(key, deserialized);
    }

    @Test
    public void testPut() throws IOException {
        HbaseConverter<ThreatIntelKey, ThreatIntelValue> converter = new ThreatIntelConverter();
        Put put = converter.toPut("cf", key, value);
        LookupKV<ThreatIntelKey, ThreatIntelValue> converted= converter.fromPut(put, "cf");
        Assert.assertEquals(results, converted);
    }
    @Test
    public void testResult() throws IOException {
        HbaseConverter<ThreatIntelKey, ThreatIntelValue> converter = new ThreatIntelConverter();
        Result r = converter.toResult("cf", key, value);
        LookupKV<ThreatIntelKey, ThreatIntelValue> converted= converter.fromResult(r, "cf");
        Assert.assertEquals(results, converted);
    }

    @Test
    public void testGet() throws Exception {
        HbaseConverter<ThreatIntelKey, ThreatIntelValue> converter = new ThreatIntelConverter();
        Get get = converter.toGet("cf", key);
        Assert.assertArrayEquals(key.toBytes(), get.getRow());
    }
}
