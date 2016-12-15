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
import org.apache.metron.common.utils.HyperLogLogPlus;
import org.apache.metron.common.utils.SerDeUtils;

import java.util.Collection;
import java.util.List;

public class DataStructureFunctions {

  @Stellar(name="ADD"
          , namespace="BLOOM"
          , description="Adds an element to the bloom filter passed in"
          , params = { "bloom - The bloom filter"
                     , "value* - The values to add"
                     }
          , returns = "Bloom Filter"
          )
  public static class BloomAdd extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      BloomFilter<Object> filter = (BloomFilter)args.get(0);
      for(int i = 1;i < args.size();++i) {
        Object arg = args.get(i);
        if(arg != null) {
          filter.add(args.get(i));
        }
      }
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
      if(args.size() == 0) {
        return false;
      }
      BloomFilter<Object> filter = (BloomFilter)args.get(0);
      if(args.size() > 1) {
        Object arg = args.get(1);
        if(arg == null) {
          return false;
        }
        return filter.mightContain(arg);
      }
      return false;
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
      return new BloomFilter<>(SerDeUtils.SERIALIZER, expectedInsertions, falsePositiveRate);
    }
  }

  @Stellar( name="MERGE"
          , namespace="BLOOM"
          , description="Returns a merged bloom filter"
          , params = { "bloomfilters - A list of bloom filters to merge"
                     }
          , returns = "Bloom Filter or null if the list is empty"
          )
  public static class BloomMerge extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      if(args.size() > 0) {
        Object firstArg = args.get(0);
        if(firstArg instanceof List) {
          BloomFilter ret = null;
          for(Object bf : (List)firstArg) {
            if(bf instanceof BloomFilter) {
              if(ret == null) {
                ret = (BloomFilter)bf;
              }
              else {
                ret.merge((BloomFilter)bf);
              }
            }
          }
          return ret;
        }
        else {
          return null;
        }
      }
      return null;
    }
  }

  @Stellar( namespace="HLLP"
          , name="CARDINALITY"
          , description="Returns HyperLogLogPlus-estimated cardinality for this set"
          , params = { "hyperLogLogPlus - the hllp set" }
          , returns = "Long value representing the cardinality for this set"
  )
  public static class HLLPCardinality extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      if (args.size() < 1) {
        throw new IllegalArgumentException("Must pass an hllp set to get the cardinality for");
      }
      return ((HyperLogLogPlus) args.get(0)).cardinality();
    }
  }

  @Stellar( namespace="HLLP"
          , name="INIT"
          , description="Initializes the set"
          , params = {
                      "p (required) - the precision value for the normal set"
                     ,"sp - the precision value for the sparse set. If sp is not specified the sparse set will be disabled."
                     }
          , returns = "A new HyperLogLogPlus set"
  )
  public static class HLLPInit extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      if (args.size() == 0) {
        throw new IllegalArgumentException("Normal set precision is required");
      } else if (args.size() == 1) {
        int p = ConversionUtils.convert(args.get(0), Integer.class);
        return new HyperLogLogPlus(p);
      } else {
        int p = ConversionUtils.convert(args.get(0), Integer.class);
        int sp = ConversionUtils.convert(args.get(1), Integer.class);
        return new HyperLogLogPlus(p, sp);
      }
    }
  }

  @Stellar( namespace="HLLP"
          , name="MERGE"
          , description="Merge hllp sets together"
          , params = {
                      "hllp1 - first hllp set"
                     ,"hllp2 - second hllp set"
                     ,"hllpn - additional sets to merge"
          }
          , returns = "A new merged HyperLogLogPlus estimator set"
  )
  public static class HLLPMerge extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      if (args.size() < 1) {
        throw new IllegalArgumentException("Must pass 1..n hllp sets to merge");
      } else {
        HyperLogLogPlus hllp = ConversionUtils.convert(args.get(0), HyperLogLogPlus.class);
        if (args.size() > 1) {
          hllp = hllp.merge(getEstimatorsFromIndex(args, 1));
        }
        return hllp;
      }
    }

    /**
     * Get sublist starting at index and convert types
     */
    private List<HyperLogLogPlus> getEstimatorsFromIndex(List<Object> args, int index) {
      return ConversionUtils.convertList(args.subList(index, args.size()), HyperLogLogPlus.class);
    }
  }

  @Stellar( namespace="HLLP"
          , name="OFFER"
          , description="Add value to the set"
          , params = {
                      "hyperLogLogPlus - the hllp set"
                     ,"o - Object to add to the set"
                     }
          , returns = "The HyperLogLogPlus set with a new object added"
  )
  public static class HLLPOffer extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      if (args.size() < 2) {
        throw new IllegalArgumentException("Must pass an hllp set and a value to add to the set");
      } else {
        HyperLogLogPlus hllp = ConversionUtils.convert(args.get(0), HyperLogLogPlus.class);
        hllp.offer(args.get(1));
        return hllp;
      }
    }
  }

  @Stellar(name="IS_EMPTY"
          , description="Returns true if string or collection is empty or null and false if otherwise."
          , params = { "input - Object of string or collection type (for example, list)"}
          , returns = "True if the string or collection is empty or null and false if otherwise."
          )
  public static class IsEmpty extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      if(null == list || list.size() == 0) {
        return true;
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
        return o == null;
      }
    }
  }

  @Stellar(name="LENGTH"
          , description="Returns the length of a string or size of a collection. Returns 0 for empty or null Strings"
          , params = { "input - Object of string or collection type (e.g. list)"}
          , returns = "Integer"
  )
  public static class Length extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> list) {
      if(list.size() == 0) {
        return 0;
      }
      Object o = list.get(0);
      if(o instanceof Collection) {
        return ((Collection)o).size();
      }
      else if(o instanceof String) {
        String val = (String) list.get(0);
        return val == null || val.isEmpty() ? 0 : val.length();
      }
      else {
        return 0;
      }
    }
  }
}
