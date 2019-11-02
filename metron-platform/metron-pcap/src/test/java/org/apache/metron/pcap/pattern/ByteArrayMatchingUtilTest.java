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

import org.apache.metron.stellar.common.utils.StellarProcessorUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ByteArrayMatchingUtilTest {
  public static byte[] DEADBEEF = new byte[] {(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef};
  public static byte[] DEADBEEF_DONUTHOLE = new byte[] {(byte) 0xde, (byte) 0xad, (byte)0x00, (byte)0x00, (byte) 0xbe, (byte) 0xef};
  public static byte[] ALLFS = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
  public static byte[] REALPACKET = DatatypeConverter.parseHexBinary("d4c3b2a1020004000000000000000000ffff0000010000000dfbea560923090062000000620000000a002700000008002796a47a0800451000547cdf40004006b7e9c0a84279c0a842010016d9ef129035948b92f427801801f5061200000101080a0014f5d511bdf9915bf6b70140db6d4fb551229ef07d2f56abd814bc56420489ca38e7faf8cec3d4");

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

  private static List<EvaluationStrategy[]> strategies() {
    List<EvaluationStrategy[]> strategies = new ArrayList<>();
    for(EvaluationStrategy s : EvaluationStrategy.values()) {
      strategies.add(new EvaluationStrategy[] { s });
    }
    return strategies;
  }

  @ParameterizedTest
  @MethodSource("strategies")
  public void testStringMatch(EvaluationStrategy strategy) {
    assertTrue(strategy.evaluate("`metron`", "metron".getBytes(StandardCharsets.UTF_8)));
    assertTrue(strategy.evaluate("`metron`", "metron example".getBytes(StandardCharsets.UTF_8)));
    assertTrue(strategy.evaluate("`metron`", "edward metron example".getBytes(StandardCharsets.UTF_8)));
    assertFalse(strategy.evaluate("`metron`", "apache".getBytes(StandardCharsets.UTF_8)));
  }

  @ParameterizedTest
  @MethodSource("strategies")
  public void testBytesMatch(EvaluationStrategy strategy) {
    assertTrue(strategy.evaluate("2f56abd814bc56420489ca38e7faf8cec3d4", REALPACKET));
    assertTrue(strategy.evaluate("2f56..14bc56420489ca38e7faf8cec3d4", REALPACKET));
    assertTrue(strategy.evaluate("(2f56)(.){2}(14bc56420489ca38e7faf8cec3d4)", REALPACKET));
    assertFalse(strategy.evaluate("(3f56)(.){2}(14bc56420489ca38e7faf8cec3d4)", REALPACKET));
    assertFalse(strategy.evaluate("3f56abd814bc56420489ca38e7faf8cec3d4", REALPACKET));
    assertTrue(strategy.evaluate("deadbeef", join(DEADBEEF, "metron".getBytes(StandardCharsets.UTF_8))));
    assertTrue(strategy.evaluate("deadbeef", join(DEADBEEF, "metron".getBytes(StandardCharsets.UTF_8))));
    assertTrue(strategy.evaluate("deadbeef `metron`", join(DEADBEEF, "metron".getBytes(StandardCharsets.UTF_8))));
    assertTrue(strategy.evaluate("deadbeef `metron`", join(DEADBEEF, "metronjones".getBytes(StandardCharsets.UTF_8))));
    assertTrue(strategy.evaluate("deadbeef `metron`", join(DEADBEEF, "metronjones".getBytes(StandardCharsets.UTF_8), DEADBEEF)));
    assertTrue(strategy.evaluate("([ff]){4}", ALLFS));
    assertFalse(strategy.evaluate("([ff]){6}", ALLFS));
    assertTrue(strategy.evaluate("[^ff]", new byte[] { (byte)0x00 }));
    assertTrue(strategy.evaluate("&01", new byte[] { (byte)0x07 }));
    assertFalse(strategy.evaluate("&01", new byte[] { (byte)0x00 }));
    assertTrue(strategy.evaluate("&01", new byte[] { (byte)0x00, (byte)0x01 }));
    assertTrue(strategy.evaluate("(dead).{2}(beef)", DEADBEEF_DONUTHOLE));
  }

  public byte[] join(byte[]... array) {
    byte[] ret;
    int size = 0;
    for (byte[] bytes : array) {
      size += bytes.length;
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
