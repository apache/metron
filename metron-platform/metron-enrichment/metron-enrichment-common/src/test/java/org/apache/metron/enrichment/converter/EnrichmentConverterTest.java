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
package org.apache.metron.enrichment.converter;

import org.apache.hadoop.hbase.client.Put;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

public class EnrichmentConverterTest {
  @Test
  public void testKeyConversion() {
    EnrichmentKey k1 = new EnrichmentKey("type", "indicator1");
    byte[] serialized = k1.toBytes();
    EnrichmentKey k2 = new EnrichmentKey();
    k2.fromBytes(serialized);
    Assert.assertEquals(k1, k2);
  }

  @Test
  public void testValueConversion() throws IOException {
    EnrichmentConverter converter = new EnrichmentConverter();
    EnrichmentKey k1 = new EnrichmentKey("type", "indicator");
    EnrichmentValue v1 = new EnrichmentValue(new HashMap<String, Object>() {{
      put("k1", "v1");
      put("k2", "v2");
    }});
    Put serialized = converter.toPut("cf", k1, v1);
    LookupKV<EnrichmentKey, EnrichmentValue> kv = converter.fromPut(serialized,"cf");
    Assert.assertEquals(k1, kv.getKey());
    Assert.assertEquals(v1, kv.getValue());
  }
}
