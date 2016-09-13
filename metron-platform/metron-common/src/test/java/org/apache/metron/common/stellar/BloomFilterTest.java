package org.apache.metron.common.stellar;

import com.google.common.collect.ImmutableMap;
import org.apache.metron.common.utils.BloomFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.apache.metron.common.stellar.StellarTest.run;

public class BloomFilterTest {
  private Map<String, Object> variables = new HashMap<String, Object>() {{
    put("string", "casey");
    put("double", 1.0);
    put("integer", 1);
    put("map", ImmutableMap.of("key1", "value1", "key2", "value2"));
  }};

  @Test
  public void testMerge() {

    BloomFilter bloomString = (BloomFilter)run("BLOOM_ADD(BLOOM_INIT(), string)", variables);
    BloomFilter bloomDouble = (BloomFilter)run("BLOOM_ADD(BLOOM_INIT(), double)", variables);
    BloomFilter bloomInteger= (BloomFilter)run("BLOOM_ADD(BLOOM_INIT(), integer)", variables);
    BloomFilter bloomMap= (BloomFilter)run("BLOOM_ADD(BLOOM_INIT(), map)", variables);
    BloomFilter merged = (BloomFilter)run("BLOOM_MERGE([stringFilter, doubleFilter, integerFilter, mapFilter])"
                                         , ImmutableMap.of("stringFilter", bloomString
                                                          ,"doubleFilter", bloomDouble
                                                          ,"integerFilter", bloomInteger
                                                          ,"mapFilter", bloomMap
                                                          )
                                         );
    Assert.assertNotNull(merged);
    for(Object val : variables.values()) {
      Assert.assertTrue(merged.mightContain(val));
    }
  }

  @Test
  public void testAdd() {
    BloomFilter result = (BloomFilter)run("BLOOM_ADD(BLOOM_INIT(), string, double, integer, map)", variables);
    for(Object val : variables.values()) {
      Assert.assertTrue(result.mightContain(val));
    }
    Assert.assertTrue(result.mightContain(ImmutableMap.of("key1", "value1", "key2", "value2")));
  }

  @Test
  public void testExists() {
    {
      Boolean result = (Boolean) run("BLOOM_EXISTS(BLOOM_ADD(BLOOM_INIT(), string, double, integer, map), 'casey')", variables);
      Assert.assertTrue(result);
    }
    {
      Boolean result = (Boolean) run("BLOOM_EXISTS(BLOOM_ADD(BLOOM_INIT(), string, double, integer, map), double)", variables);
      Assert.assertTrue(result);
    }
    {
      Boolean result = (Boolean) run("BLOOM_EXISTS(BLOOM_ADD(BLOOM_INIT(), string, double, integer, map), integer)", variables);
      Assert.assertTrue(result);
    }
    {
      Boolean result = (Boolean) run("BLOOM_EXISTS(BLOOM_ADD(BLOOM_INIT(), string, double, integer, map), map)", variables);
      Assert.assertTrue(result);
    }
    {
      Boolean result = (Boolean) run("BLOOM_EXISTS(BLOOM_ADD(BLOOM_INIT(), string, double, integer, map), 'samantha')", variables);
      Assert.assertFalse(result);
    }
    {
      Boolean result = (Boolean) run("BLOOM_EXISTS(BLOOM_ADD(BLOOM_INIT(), string, double, integer, map), sam)", variables);
      Assert.assertFalse(result);
    }
  }
}
