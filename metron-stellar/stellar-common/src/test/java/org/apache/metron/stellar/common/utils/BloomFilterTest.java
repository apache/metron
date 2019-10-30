/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.metron.stellar.common.utils;

import com.google.common.collect.ImmutableMap;
import org.apache.metron.stellar.dsl.ParseException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;
import static org.junit.jupiter.api.Assertions.*;

public class BloomFilterTest {
  private Map<String, Object> variables = new HashMap<String, Object>() {{
    put("string", "casey");
    put("double", 1.0);
    put("integer", 1);
    put("map", ImmutableMap.of("key1", "value1", "key2", "value2"));
  }};

  @Test
  @SuppressWarnings("unchecked")
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
    assertNotNull(merged);
    for(Object val : variables.values()) {
      assertTrue(merged.mightContain(val));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAdd() {
    BloomFilter result = (BloomFilter)run("BLOOM_ADD(BLOOM_INIT(), string, double, integer, map)", variables);
    for(Object val : variables.values()) {
      assertTrue(result.mightContain(val));
    }
    assertTrue(result.mightContain(ImmutableMap.of("key1", "value1", "key2", "value2")));
  }

  @Test
  public void testExists() {
    {
      Boolean result = (Boolean) run("BLOOM_EXISTS(BLOOM_ADD(BLOOM_INIT(), string, double, integer, map), 'casey')", variables);
      assertTrue(result);
    }
    {
      Boolean result = (Boolean) run("BLOOM_EXISTS(BLOOM_ADD(BLOOM_INIT(), string, double, integer, map), double)", variables);
      assertTrue(result);
    }
    {
      Boolean result = (Boolean) run("BLOOM_EXISTS(BLOOM_ADD(BLOOM_INIT(), string, double, integer, map), integer)", variables);
      assertTrue(result);
    }
    {
      Boolean result = (Boolean) run("BLOOM_EXISTS(BLOOM_ADD(BLOOM_INIT(), string, double, integer, map), map)", variables);
      assertTrue(result);
    }
    {
      Boolean result = (Boolean) run("BLOOM_EXISTS(BLOOM_ADD(BLOOM_INIT(), string, double, integer, map), 'samantha')", variables);
      assertFalse(result);
    }
    {
      boolean thrown = false;
      try{
        run("BLOOM_EXISTS(BLOOM_ADD(BLOOM_INIT(), string, double, integer, map), sam)", variables);
      }catch(ParseException pe){
        thrown = true;
      }
      assertTrue(thrown);
    }
  }
}
