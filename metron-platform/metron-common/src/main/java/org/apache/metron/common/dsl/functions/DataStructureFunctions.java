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
package org.apache.metron.common.dsl.functions;

import org.apache.metron.common.dsl.BaseStellarFunction;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.utils.BloomFilter;
import org.apache.metron.common.utils.ConversionUtils;
import org.apache.metron.common.utils.SerializationUtils;

import java.util.Collection;
import java.util.List;

public class DataStructureFunctions {

  @Stellar(name="ADD"
          , namespace="BLOOM"
          , description="Adds an element to the bloom filter passed in"
          , params = { "bloom - The bloom filter"
                     , "value - The value to add"
                     }
          , returns = "Bloom Filter"
          )
  public static class BloomAdd extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      BloomFilter<Object> filter = (BloomFilter)args.get(0);
      Object arg = args.get(1);
      filter.add(arg);
      return filter;
    }
  }

  @Stellar(name="EXISTS"
          , namespace="BLOOM"
          , description="If the bloom filter contains the value"
          , params = { "bloom - The bloom filter"
                     , "value - The value to check"
                     }
          , returns = "True if the filter might contain the value and false otherwise"
          )
  public static class BloomExists extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      BloomFilter<Object> filter = (BloomFilter)args.get(0);
      Object arg = args.get(1);
      return filter.mightContain(arg);
    }
  }

  @Stellar(name="INIT"
         , namespace="BLOOM"
          , description="Returns an empty bloom filter"
          , params = { "expectedInsertions - The expected insertions"
                     , "falsePositiveRate - The false positive rate you are willing to tolerate"
                     }
          , returns = "Bloom Filter"
          )
  public static class BloomInit extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      int expectedInsertions = 100000;
      float falsePositiveRate = 0.01f;
      if(args.size() > 1) {
        expectedInsertions = ConversionUtils.convert(args.get(0), Integer.class);
      }
      if(args.size() > 2) {
        falsePositiveRate= ConversionUtils.convert(args.get(1), Float.class);
      }
      return new BloomFilter<>(SerializationUtils.INSTANCE, expectedInsertions, falsePositiveRate);
    }
  }


  @Stellar(name="IS_EMPTY"
          , description="Returns true if string or collection is empty and false otherwise"
          , params = { "input - Object of string or collection type (e.g. list)"}
          , returns = "Boolean"
          )
  public static class IsEmpty extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      if(list.size() == 0) {
        throw new IllegalStateException("IS_EMPTY expects one string arg");
      }
      Object o = list.get(0);
      if(o instanceof Collection) {
        return ((Collection)o).isEmpty();
      }
      else if(o instanceof String) {
        String val = (String) list.get(0);
        return val == null || val.isEmpty() ? true : false;
      }
      else {
        throw new IllegalStateException("IS_EMPTY expects a collection or string");
      }
    }
  }
}
