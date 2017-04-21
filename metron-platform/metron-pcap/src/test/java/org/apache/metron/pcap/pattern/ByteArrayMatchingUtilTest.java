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
package org.apache.metron.pcap.pattern;

import com.google.common.collect.ImmutableMap;
import org.apache.metron.common.utils.StellarProcessorUtils;
import org.apache.metron.pcap.pattern.ByteArrayMatchingUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

@RunWith(Parameterized.class)
public class ByteArrayMatchingUtilTest {
  public static byte[] DEADBEEF = new byte[] {(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef};
  public static byte[] DEADBEEF_DONUTHOLE = new byte[] {(byte) 0xde, (byte) 0xad, (byte)0x00, (byte)0x00, (byte) 0xbe, (byte) 0xef};
  public static byte[] ALLFS = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

  interface Evaluator {
    boolean evaluate(String pattern, byte[] data);
  }

  public enum EvaluationStrategy implements Evaluator{
     STELLAR_WITH_VARIABLES((pattern, data) -> {
       Map<String, Object> args = new HashMap<>();
       args.put("pattern", pattern);
       args.put("data", data);
       return (boolean) StellarProcessorUtils.run("BYTEARRAY_MATCHER(pattern, data)" , args);
     }
            ),
    STELLAR_WITH_PATTERN_STRING((pattern, data) -> {
       Map<String, Object> args = new HashMap<>();
       args.put("data", data);
       return (boolean) StellarProcessorUtils.run(String.format("BYTEARRAY_MATCHER('%s', data)", pattern) , args);
     }
            )
    , UTIL((pattern, data) -> {
      try {
        return ByteArrayMatchingUtil.INSTANCE.match(pattern, data);
      } catch (ExecutionException e) {
        throw new IllegalArgumentException(e);
      }
    })
    ;
    Evaluator evaluator;
    EvaluationStrategy(Evaluator evaluator) {
      this.evaluator = evaluator;
    }
    @Override
    public boolean evaluate(String pattern, byte[] data) {
      return evaluator.evaluate(pattern, data);
    }
  }
  private EvaluationStrategy strategy = null;
  public ByteArrayMatchingUtilTest(EvaluationStrategy strategy) {
    this.strategy = strategy;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> strategies() {
    List<Object[]> strategies = new ArrayList<>();
    for(EvaluationStrategy s : EvaluationStrategy.values()) {
      strategies.add(new Object[] { s });
    }
    return strategies;
  }

  @Test
  public void testStringMatch() throws ExecutionException {
    Assert.assertTrue(strategy.evaluate("`metron`", "metron".getBytes()));
    Assert.assertTrue(strategy.evaluate("`metron`", "metron example".getBytes()));
    Assert.assertTrue(strategy.evaluate("`metron`", "edward metron example".getBytes()));
    Assert.assertFalse(strategy.evaluate("`metron`", "apache".getBytes()));
  }

  @Test
  public void testBytesMatch() throws ExecutionException {
    Assert.assertTrue(strategy.evaluate("deadbeef", join(DEADBEEF, "metron".getBytes())));
    Assert.assertTrue(strategy.evaluate("deadbeef `metron`", join(DEADBEEF, "metron".getBytes())));
    Assert.assertTrue(strategy.evaluate("deadbeef `metron`", join(DEADBEEF, "metronjones".getBytes())));
    Assert.assertTrue(strategy.evaluate("deadbeef `metron`", join(DEADBEEF, "metronjones".getBytes(), DEADBEEF)));
    Assert.assertTrue(strategy.evaluate("([ff]){4}", ALLFS));
    Assert.assertFalse(strategy.evaluate("([ff]){6}", ALLFS));
    Assert.assertTrue(strategy.evaluate("[^ff]", new byte[] { (byte)0x00 }));
    Assert.assertTrue(strategy.evaluate("&01", new byte[] { (byte)0x07 }));
    Assert.assertFalse(strategy.evaluate("&01", new byte[] { (byte)0x00 }));
    Assert.assertTrue(strategy.evaluate("&01", new byte[] { (byte)0x00, (byte)0x01 }));
    Assert.assertTrue(strategy.evaluate("(dead).{2}(beef)", DEADBEEF_DONUTHOLE));
  }

  public byte[] join(byte[]... array) {
    byte[] ret;
    int size = 0;
    for(int i = 0;i < array.length;++i) {
      size += array[i].length;
    }
    ret = new byte[size];
    int j = 0;
    for(int i = 0;i < array.length;++i) {
      for(int k = 0;k < array[i].length;++k,++j) {
        ret[j] = array[i][k];
      }
    }
    return ret;
  }
}
