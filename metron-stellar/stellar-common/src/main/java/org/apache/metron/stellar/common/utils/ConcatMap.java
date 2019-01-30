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

/**
 * ConcatMap is a lazy concatenation of a list of Maps.  It is lazy in that it does not construct
 * a union of all of the maps, but rather keeps the maps separate.  Key/Value resolution is
 * done via a first-wins strategy (i.e. the first map which has a key will be used).
 *
 * Also, note, that this is an immutable map, so operations which require mutation will have
 * UnsupportedOperationException thrown.
 */
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

  /**
   * If any maps contains the key, then this will return true.
   * @param key
   * @return
   */
  @Override
  public boolean containsKey(Object key) {
    for(Map m : variableMappings) {
      if(m.containsKey(key)) {
        return true;
      }
    }
    return false;
  }

  /**
   *
   * If any maps contains the value, then this will return true.
   * @param value
   * @return
   */
  @Override
  public boolean containsValue(Object value) {
    for(Map m : variableMappings) {
      if(m.containsValue(value)) {
        return true;
      }
    }
    return false;
  }

  /**
   * The first map which contains the key will have the associated value returned.
   * @param key
   * @return
   */
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

  /**
   * This is an immutable map and this operation is not supported.
   * @param key
   * @param value
   * @return
   */
  @Override
  public Object put(String key, Object value) {
    throw new UnsupportedOperationException("Merged map is immutable.");
  }

  /**
   *
   * This is an immutable map and this operation is not supported.
   * @param key
   * @return
   */
  @Override
  public Object remove(Object key) {
    throw new UnsupportedOperationException("Merged map is immutable.");
  }

  /**
   *
   * This is an immutable map and this operation is not supported.
   * @param m
   */
  @Override
  public void putAll(Map<? extends String, ?> m) {
    throw new UnsupportedOperationException("Merged map is immutable.");
  }

  /**
   *
   * This is an immutable map and this operation is not supported.
   */
  @Override
  public void clear() {
    throw new UnsupportedOperationException("Merged map is immutable.");
  }

  @Override
  @SuppressWarnings("unchecked")
  public Set<String> keySet() {
    Set<String> ret = null;
    for (Map m : variableMappings) {
      if (ret == null) {
        ret = m.keySet();
      }
      else {
        ret = Sets.union(ret, m.keySet());
      }
    }
    return ret;
  }

  /**
   * Note: this makes a copy of the values, so it is not fundamentally lazy.
   * @return
   */
  @Override
  @SuppressWarnings("unchecked")
  public Collection<Object> values() {
    Collection<Object> ret = new ArrayList<>(size());
    for (Map m : variableMappings) {
      ret.addAll(m.values());
    }
    return ret;
  }

  /**
   * This is a lazy entry collection of the associated maps.  If there are duplicate keys, they will appear
   * twice here, so be careful.
   * @return
   */
  @Override
  @SuppressWarnings("unchecked")
  public Set<Entry<String, Object>> entrySet() {
    Set<Entry<String, Object>> ret = null;
    for (Map m : variableMappings) {
      if (ret == null) {
        ret = m.entrySet();
      } else {
        ret = Sets.union(ret, m.entrySet());
      }
    }
    return ret;
  }

  @Override
  @SuppressWarnings("unchecked")
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
