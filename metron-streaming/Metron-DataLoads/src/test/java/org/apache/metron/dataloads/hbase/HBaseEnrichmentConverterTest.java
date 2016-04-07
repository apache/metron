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
import org.apache.metron.hbase.converters.HbaseConverter;
import org.apache.metron.hbase.converters.enrichment.EnrichmentConverter;
import org.apache.metron.hbase.converters.enrichment.EnrichmentKey;
import org.apache.metron.hbase.converters.enrichment.EnrichmentValue;
import org.apache.metron.reference.lookup.LookupKV;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;


public class HBaseEnrichmentConverterTest {
    EnrichmentKey key = new EnrichmentKey("domain", "google");
    EnrichmentValue value = new EnrichmentValue(
            new HashMap<String, String>() {{
                put("foo", "bar");
                put("grok", "baz");
            }});
    LookupKV<EnrichmentKey, EnrichmentValue> results = new LookupKV(key, value);
    @Test
    public void testKeySerialization() {
        byte[] serialized = key.toBytes();

        EnrichmentKey deserialized = new EnrichmentKey();
        deserialized.fromBytes(serialized);
        Assert.assertEquals(key, deserialized);
    }

    @Test
    public void testPut() throws IOException {
        HbaseConverter<EnrichmentKey, EnrichmentValue> converter = new EnrichmentConverter();
        Put put = converter.toPut("cf", key, value);
        LookupKV<EnrichmentKey, EnrichmentValue> converted= converter.fromPut(put, "cf");
        Assert.assertEquals(results, converted);
    }
    @Test
    public void testResult() throws IOException {
        HbaseConverter<EnrichmentKey, EnrichmentValue> converter = new EnrichmentConverter();
        Result r = converter.toResult("cf", key, value);
        LookupKV<EnrichmentKey, EnrichmentValue> converted= converter.fromResult(r, "cf");
        Assert.assertEquals(results, converted);
    }

    @Test
    public void testGet() throws Exception {
        HbaseConverter<EnrichmentKey, EnrichmentValue> converter = new EnrichmentConverter();
        Get get = converter.toGet("cf", key);
        Assert.assertArrayEquals(key.toBytes(), get.getRow());
    }
}
