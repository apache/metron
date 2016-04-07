package org.apache.metron.hbase.converters.enrichment;

import org.apache.hadoop.hbase.client.Put;
import org.apache.metron.reference.lookup.LookupKV;
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
    EnrichmentValue v1 = new EnrichmentValue(new HashMap<String, String>() {{
      put("k1", "v1");
      put("k2", "v2");
    }});
    Put serialized = converter.toPut("cf", k1, v1);
    LookupKV<EnrichmentKey, EnrichmentValue> kv = converter.fromPut(serialized,"cf");
    Assert.assertEquals(k1, kv.getKey());
    Assert.assertEquals(v1, kv.getValue());
  }
}
