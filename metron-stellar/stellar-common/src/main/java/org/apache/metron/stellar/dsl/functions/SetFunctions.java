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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class SetFunctions {
  @Stellar(name="INIT"
          , namespace="SET"
          , description="Creates a new set"
          , params = { "input (optional) - An initialization of the set"}
          , returns = "A Set"
  )
  public static class SetInit extends BaseStellarFunction {
    @Override
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> list) {
      LinkedHashSet<Object> ret = new LinkedHashSet<>();
      if (list.size() == 1) {
        Object o = list.get(0);
        if (o != null) {
          if (o instanceof Iterable) {
            Iterables.addAll(ret, (Iterable) o);
          } else {
            throw new IllegalArgumentException("Expected an Iterable, but " + o + " is of type " + o.getClass());
          }
        }

      }
      return ret;
    }
  }

  @Stellar(name = "ADD",
            namespace = "SET",
            description = "Adds to a set",
            params = {"set - The set to add to",
                     "o - object to add to set"
                     },
            returns = "A Set"
  )
  public static class SetAdd extends BaseStellarFunction {
    @Override
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> list) {
      if (list.size() < 1) {
        return null;
      }
      LinkedHashSet<Object> ret = (LinkedHashSet<Object>)list.get(0);
      if (ret == null) {
        ret = new LinkedHashSet<>();
      }
      for (int i = 1;i < list.size();++i) {
        Object o = list.get(i);
        if (o != null) {
          ret.add(o);
        }
      }
      return ret;
    }
  }

  @Stellar(name="REMOVE"
          , namespace="SET"
          , description="Removes from a set"
          , params = {"set - The set to add to"
                     ,"o - object to add to set"
                     }
          , returns = "A Set"
  )
  public static class SetRemove extends BaseStellarFunction {
    @Override
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> list) {
      if (list.size() < 1) {
        return null;
      }
      LinkedHashSet<Object> ret = (LinkedHashSet<Object>)list.get(0);
      if (ret == null) {
        ret = new LinkedHashSet<>();
      }
      for (int i = 1;i < list.size();++i) {
        Object o = list.get(i);
        if (o != null) {
          ret.remove(o);
        }
      }
      return ret;
    }
  }

  @Stellar(name = "MERGE",
            namespace = "SET",
            description = "Merges a list of sets",
            params = {"sets - A collection of sets to merge"},
            returns = "A Set"
  )
  public static class SetMerge extends BaseStellarFunction {
    @Override
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> list) {
      if (list.size() < 1) {
        return null;
      }
      LinkedHashSet<Object> ret = new LinkedHashSet<>();
      Object o = list.get(0);
      if (o != null) {
        if (!(o instanceof Iterable)) {
          throw new IllegalArgumentException("Expected an Iterable, but " + o + " is of type " + o.getClass());
        }
        Iterable<? extends Iterable> sets = (Iterable<? extends Iterable>) o;

        for (Iterable s : sets) {
          if (s != null) {
            Iterables.addAll(ret, s);
          }
        }
      }
      return ret;
    }
  }

  @Stellar(name = "INIT",
            namespace = "MULTISET",
            description = "Creates an empty multiset, which is a map associating objects to their instance counts.",
            params = { "input (optional) - An initialization of the multiset"},
            returns = "A multiset"
  )
  public static class MultiSetInit extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> list) {
      LinkedHashMap<Object, Integer> ret = new LinkedHashMap<>();
      if (list.size() >= 1) {
        Object o = list.get(0);
        if (o != null) {
          if (!(o instanceof Iterable)) {
            throw new IllegalArgumentException("Expected an Iterable, but " + o + " is of type " + o.getClass());
          }
          for (Object obj : (Iterable) o) {
            ret.merge(obj, 1, (k, one) -> k + one);
          }
        }
      }
      return ret;
    }
  }

  @Stellar(name="ADD"
          , namespace="MULTISET"
          , description="Adds to a multiset, which is a map associating objects to their instance counts."
          , params = {"set - The multiset to add to"
                     ,"o - object to add to multiset"
                     }
          , returns = "A multiset"
  )
  public static class MultiSetAdd extends BaseStellarFunction {
    @Override
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> list) {
      if (list.size() < 1) {
        return null;
      }
      LinkedHashMap<Object, Integer> ret = (LinkedHashMap<Object, Integer>)list.get(0);
      if (ret == null) {
        ret = new LinkedHashMap<>();
      }
      for (int i = 1;i < list.size();++i) {
        Object o = list.get(i);
        if (o != null) {
          ret.merge(o, 1, (k, one) -> k + one);
        }
      }
      return ret;
    }
  }

  @Stellar(name = "REMOVE",
            namespace = "MULTISET",
            description = "Removes from a multiset, which is a map associating objects to their instance counts.",
            params = {"set - The multiset to add to",
                      "o - object to remove from multiset"
                     },
            returns = "A multiset"
  )
  public static class MultiSetRemove extends BaseStellarFunction {
    @Override
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> list) {
      if(list.size() < 1) {
        return null;
      }
      LinkedHashMap<Object, Integer> ret = (LinkedHashMap<Object, Integer>)list.get(0);
      if (ret == null) {
        ret = new LinkedHashMap<>();
      }
      for (int i = 1;i < list.size();++i) {
        Object o = list.get(i);
        if (o != null) {
          Integer cnt = ret.get(o);
          if (cnt == null) {
            continue;
          }
          if (cnt == 1) {
            ret.remove(o);
          } else {
            ret.put(o, cnt - 1);
          }
        }
      }
      return ret;
    }
  }

  @Stellar(name = "MERGE",
            namespace = "MULTISET",
            description = "Merges a list of multisets, which is a map associating objects to their instance counts.",
            params = {"sets - A collection of multisets to merge"},
            returns = "A multiset"
  )
  public static class MultiSetMerge extends BaseStellarFunction {
    @Override
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> list) {
      if (list.size() < 1) {
        return null;
      }
      LinkedHashMap<Object, Integer> ret = new LinkedHashMap<>();
      Iterable<Map<Object, Integer>> maps = (Iterable<Map<Object, Integer>>) list.get(0);
      for (Map<Object, Integer> s : maps) {
        if (s != null) {
          for (Map.Entry<Object, Integer> kv : s.entrySet()) {
            ret.merge(kv.getKey(), kv.getValue(), (k, cnt) -> k + cnt);
          }
        }
      }
      return ret;
    }
  }


  @Stellar(name="TO_SET"
          , namespace="MULTISET"
          , description="Create a set out of a multiset, which is a map associating objects to their instance counts."
          , params = {"multiset - The multiset to convert."
                     }
          , returns = "The set of objects in the multiset ignoring multiplicity"
  )
  public static class MultiSetToSet extends BaseStellarFunction {
    @Override
    @SuppressWarnings("unchecked")
    public Object apply(List<Object> list) {
      if (list.size() < 1) {
        return null;
      }
      LinkedHashSet<Object> ret = new LinkedHashSet<>();
      if (list.size() == 1) {
        Map<Object, Integer> multiset = (Map<Object, Integer>)list.get(0);
        if (multiset != null) {
          ret.addAll(multiset.keySet());
        }
      }
      return ret;
    }
  }
}
