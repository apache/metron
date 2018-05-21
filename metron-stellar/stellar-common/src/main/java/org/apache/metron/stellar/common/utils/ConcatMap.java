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

package org.apache.metron.stellar.common.utils;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConcatMap implements Map<String, Object>, Serializable, KryoSerializable {
  List<Map> variableMappings = new ArrayList<>();
  public ConcatMap(List<Map> variableMappings) {
    this.variableMappings = variableMappings;
  }

  @Override
  public int size() {
    int size = 0;
    for(Map m : variableMappings) {
      size += m.size();
    }
    return size;
  }

  @Override
  public boolean isEmpty() {
    boolean isEmpty = true;
    for(Map m : variableMappings) {
      isEmpty &= m.isEmpty();
    }
    return isEmpty;
  }

  @Override
  public boolean containsKey(Object key) {
    for(Map m : variableMappings) {
      if(m.containsKey(key)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsValue(Object value) {
    for(Map m : variableMappings) {
      if(m.containsValue(value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Object get(Object key) {
    Object ret = null;
    for(Map m : variableMappings) {
      ret = m.get(key);
      if(ret != null) {
        break;
      }
    }
    return ret;
  }

  @Override
  public Object put(String key, Object value) {
    throw new UnsupportedOperationException("Merged map is immutable.");
  }

  @Override
  public Object remove(Object key) {
    throw new UnsupportedOperationException("Merged map is immutable.");
  }

  @Override
  public void putAll(Map<? extends String, ?> m) {
    throw new UnsupportedOperationException("Merged map is immutable.");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("Merged map is immutable.");
  }

  @Override
  public Set<String> keySet() {
    Set<String> ret = null;
    for(Map m : variableMappings) {
      if(ret == null) {
        ret = m.keySet();
      }
      else {
        ret = Sets.union(ret, m.keySet());
      }
    }
    return ret;
  }

  @Override
  public Collection<Object> values() {
    Collection<Object> ret = new ArrayList<>(size());
    for(Map m : variableMappings) {
      ret.addAll(m.values());
    }
    return ret;
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    Set<Entry<String, Object>> ret = null;
    for(Map m : variableMappings) {
      if(ret == null) {
        ret = m.entrySet();
      }
      else {
        ret = Sets.union(ret, m.entrySet());
      }
    }
    return ret;
  }

  @Override
  public String toString() {
    Iterable<Iterable<Map.Entry<Object, Object>>> transformed =
            Iterables.transform(variableMappings, x -> x.entrySet());
    Iterable<Map.Entry<Object, Object>> it = Iterables.filter( Iterables.concat(transformed)
                                                             , x -> x.getValue() != null
                                                             );
    return "{" + Joiner.on(", ").join(it) + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConcatMap concatMap = (ConcatMap) o;

    return variableMappings != null ? variableMappings.equals(concatMap.variableMappings) : concatMap.variableMappings == null;

  }

  @Override
  public int hashCode() {
    return variableMappings != null ? variableMappings.hashCode() : 0;
  }

  @Override
  public void write(Kryo kryo, Output output) {
    int numVariableMappings = variableMappings.isEmpty()?0:variableMappings.size();
    output.writeShort(numVariableMappings);
    for(Map m : variableMappings) {
      byte[] b = m == null?new byte[]{}:SerDeUtils.toBytes(m);
      output.writeInt(b.length);
      if(b.length > 0) {
        output.writeBytes(b);
      }
    }
  }

  @Override
  public void read(Kryo kryo, Input input) {
    int numVariableMappings = input.readShort();
    variableMappings = new ArrayList<>(numVariableMappings);
    for(int i = 0;i < numVariableMappings;++i) {
      int size = input.readInt();
      if(size > 0) {
        byte[] bytes = input.readBytes(size);
        Map m = SerDeUtils.fromBytes(bytes, Map.class);
        variableMappings.add(m);
      }
    }
  }
}
