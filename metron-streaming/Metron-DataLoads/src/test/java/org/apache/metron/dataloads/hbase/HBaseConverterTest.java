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
