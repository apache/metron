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
package org.apache.metron.stellar.dsl.functions;

import com.google.common.collect.Iterables;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.common.utils.BloomFilter;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.common.utils.SerDeUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataStructureFunctions {

  @Stellar(name="ADD"
          , namespace="BLOOM"
          , description="Adds an element to the bloom filter passed in"
          , params = { "bloom - The bloom filter"
                     , "value(s) - The value(s) to add"
                     }
          , returns = "Bloom Filter"
          )
  public static class BloomAdd extends BaseStellarFunction {

    @Override
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> args) {
      BloomFilter<Object> filter = (BloomFilter)args.get(0);
      for (int i = 1;i < args.size();++i) {
        Object arg = args.get(i);
        if (arg != null) {
          filter.add(args.get(i));
        }
      }
      return filter;
    }
  }

  @Stellar(name = "EXISTS",
            namespace = "BLOOM",
            description = "If the bloom filter contains the value",
            params = { "bloom - The bloom filter",
                       "value - The value to check"
                     },
            returns = "True if the filter might contain the value and false otherwise"
          )
  public static class BloomExists extends BaseStellarFunction {

    @Override
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> args) {
      if (args.size() == 0) {
        return false;
      }
      BloomFilter<Object> filter = (BloomFilter)args.get(0);
      if (args.size() > 1) {
        Object arg = args.get(1);
        if (arg == null) {
          return false;
        }
        return filter.mightContain(arg);
      }
      return false;
    }
  }

  @Stellar(name = "INIT",
           namespace = "BLOOM",
           description = "Returns an empty bloom filter",
           params = { "expectedInsertions - The expected insertions",
                      "falsePositiveRate - The false positive rate you are willing to tolerate"
                     },
           returns = "Bloom Filter"
          )
  public static class BloomInit extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      int expectedInsertions = 100000;
      float falsePositiveRate = 0.01f;
      if (args.size() > 1) {
        expectedInsertions = ConversionUtils.convert(args.get(0), Integer.class);
      }
      if (args.size() > 2) {
        falsePositiveRate = ConversionUtils.convert(args.get(1), Float.class);
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
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> args) {
      if (args.size() > 0) {
        Object firstArg = args.get(0);
        if (firstArg instanceof List) {
          BloomFilter ret = null;
          for (Object bf : (List)firstArg) {
            if (bf instanceof BloomFilter) {
              if (ret == null) {
                ret = (BloomFilter)bf;
              } else {
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
      else if(o instanceof Map) {
        return (((Map)o).isEmpty());
      }
      else {
        return o == null;
      }
    }
  }
  @Stellar(name="ADD"
          ,namespace="LIST"
          , description="Adds an element to a list."
          , params = { "list - List to add element to."
                     , "element - Element to add to list"
                     }
          , returns = "Resulting list with the item added at the end."
  )

  public static class ListAdd extends BaseStellarFunction {
    @Override
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> list) {
      if (list.size() == 0) {
        return null;
      }
      Object o = list.get(0);
      if (list.size() == 1) {
        return o;
      }
      if (o instanceof List) {
        List l = (List)o;
        Object arg = list.get(1);
        l.add(arg);
        return l;
      } else {
        return o;
      }
    }
  }

  @Stellar(name = "LENGTH",
            description = "Returns the length of a string or size of a collection. Returns 0 for empty or null Strings",
            params = { "input - Object of string or collection type (e.g. list)"},
            returns = "Integer"
  )
  public static class Length extends BaseStellarFunction {
    @Override
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> list) {
      if (list.size() == 0) {
        return 0;
      }
      Object o = list.get(0);
      if (o instanceof Collection) {
        return ((Collection)o).size();
      } else if (o instanceof Map) {
        return ((Map)o).size();
      } else if (o instanceof String) {
        String val = (String) list.get(0);
        return val == null || val.isEmpty() ? 0 : val.length();
      } else {
        return 0;
      }
    }
  }
}
