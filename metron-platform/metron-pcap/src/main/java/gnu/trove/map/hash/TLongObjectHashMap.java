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
package gnu.trove.map.hash;

import gnu.trove.map.TLongObjectMap;
import org.apache.mahout.math.map.OpenLongObjectHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This is a dependency of byteseek, a library we use for our binary pattern matching.
 * Unfortunately, Trove is LGPL, so we cannot use it.  We must, therefore, exclude it from
 * our build and replace it with something equivalent.
 *
 * The use is minimal, so we need only a few functions and we're going to use mahout's math library
 * which is based around colt, another primitive collections library.  This should give similar performance
 * and a more permissive license for our purposes.
 *
 * @param <T>
 */
public class TLongObjectHashMap<T> extends OpenLongObjectHashMap implements TLongObjectMap<T> {
  /**
   * Returns <tt>true</tt> if this map contains a mapping for the specified
   * key.  More formally, returns <tt>true</tt> if and only if
   * this map contains a mapping for a key <tt>k</tt> such that
   * <tt>(key==null ? k==null : key.equals(k))</tt>.  (There can be
   * at most one such mapping.)
   *
   * @param key key whose presence in this map is to be tested
   * @return <tt>true</tt> if this map contains a mapping for the specified
   * key
   * @throws ClassCastException   if the key is of an inappropriate type for
   *                              this map
   *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
   * @throws NullPointerException if the specified key is null and this map
   *                              does not permit null keys
   *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
   */
  @Override
  public boolean containsKey(Object key) {
    return super.containsKey((long)key);
  }

  /**
   * Returns the value to which the specified key is mapped,
   * or {@code null} if this map contains no mapping for the key.
   * <p>
   * <p>More formally, if this map contains a mapping from a key
   * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
   * key.equals(k))}, then this method returns {@code v}; otherwise
   * it returns {@code null}.  (There can be at most one such mapping.)
   * <p>
   * <p>If this map permits null values, then a return value of
   * {@code null} does not <i>necessarily</i> indicate that the map
   * contains no mapping for the key; it's also possible that the map
   * explicitly maps the key to {@code null}.  The {@link #containsKey
   * containsKey} operation may be used to distinguish these two cases.
   *
   * @param key the key whose associated value is to be returned
   * @return the value to which the specified key is mapped, or
   * {@code null} if this map contains no mapping for the key
   * @throws ClassCastException   if the key is of an inappropriate type for
   *                              this map
   *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
   * @throws NullPointerException if the specified key is null and this map
   *                              does not permit null keys
   *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
   */
  @Override
  public T get(Object key) {
    return (T)super.get((long)key);
  }

  /**
   * Associates the specified value with the specified key in this map
   * (optional operation).  If the map previously contained a mapping for
   * the key, the old value is replaced by the specified value.  (A map
   * <tt>m</tt> is said to contain a mapping for a key <tt>k</tt> if and only
   * if {@link #containsKey(Object) m.containsKey(k)} would return
   * <tt>true</tt>.)
   *
   * @param key   key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   * @return the previous value associated with <tt>key</tt>, or
   * <tt>null</tt> if there was no mapping for <tt>key</tt>.
   * (A <tt>null</tt> return can also indicate that the map
   * previously associated <tt>null</tt> with <tt>key</tt>,
   * if the implementation supports <tt>null</tt> values.)
   * @throws UnsupportedOperationException if the <tt>put</tt> operation
   *                                       is not supported by this map
   * @throws ClassCastException            if the class of the specified key or value
   *                                       prevents it from being stored in this map
   * @throws NullPointerException          if the specified key or value is null
   *                                       and this map does not permit null keys or values
   * @throws IllegalArgumentException      if some property of the specified key
   *                                       or value prevents it from being stored in this map
   */
  @Override
  public T put(Long key, T value) {
    throw new UnsupportedOperationException("Unsupported operation.");
  }

  /**
   * Removes the mapping for a key from this map if it is present
   * (optional operation).   More formally, if this map contains a mapping
   * from key <tt>k</tt> to value <tt>v</tt> such that
   * <code>(key==null ?  k==null : key.equals(k))</code>, that mapping
   * is removed.  (The map can contain at most one such mapping.)
   * <p>
   * <p>Returns the value to which this map previously associated the key,
   * or <tt>null</tt> if the map contained no mapping for the key.
   * <p>
   * <p>If this map permits null values, then a return value of
   * <tt>null</tt> does not <i>necessarily</i> indicate that the map
   * contained no mapping for the key; it's also possible that the map
   * explicitly mapped the key to <tt>null</tt>.
   * <p>
   * <p>The map will not contain a mapping for the specified key once the
   * call returns.
   *
   * @param key key whose mapping is to be removed from the map
   * @return the previous value associated with <tt>key</tt>, or
   * <tt>null</tt> if there was no mapping for <tt>key</tt>.
   * @throws UnsupportedOperationException if the <tt>remove</tt> operation
   *                                       is not supported by this map
   * @throws ClassCastException            if the key is of an inappropriate type for
   *                                       this map
   *                                       (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
   * @throws NullPointerException          if the specified key is null and this
   *                                       map does not permit null keys
   *                                       (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
   */
  @Override
  public T remove(Object key) {
    throw new UnsupportedOperationException("Unsupported operation.");
  }

  /**
   * Copies all of the mappings from the specified map to this map
   * (optional operation).  The effect of this call is equivalent to that
   * of calling {@link #put(Object, Object) put(k, v)} on this map once
   * for each mapping from key <tt>k</tt> to value <tt>v</tt> in the
   * specified map.  The behavior of this operation is undefined if the
   * specified map is modified while the operation is in progress.
   *
   * @param m mappings to be stored in this map
   * @throws UnsupportedOperationException if the <tt>putAll</tt> operation
   *                                       is not supported by this map
   * @throws ClassCastException            if the class of a key or value in the
   *                                       specified map prevents it from being stored in this map
   * @throws NullPointerException          if the specified map is null, or if
   *                                       this map does not permit null keys or values, and the
   *                                       specified map contains null keys or values
   * @throws IllegalArgumentException      if some property of a key or value in
   *                                       the specified map prevents it from being stored in this map
   */
  @Override
  public void putAll(Map<? extends Long, ? extends T> m) {
    throw new UnsupportedOperationException("Unsupported operation.");
  }

  /**
   * Returns a {@link Set} view of the keys contained in this map.
   * The set is backed by the map, so changes to the map are
   * reflected in the set, and vice-versa.  If the map is modified
   * while an iteration over the set is in progress (except through
   * the iterator's own <tt>remove</tt> operation), the results of
   * the iteration are undefined.  The set supports element removal,
   * which removes the corresponding mapping from the map, via the
   * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
   * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
   * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
   * operations.
   *
   * @return a set view of the keys contained in this map
   */
  @Override
  public Set<Long> keySet() {
    throw new UnsupportedOperationException("Unsupported operation.");
  }

  /**
   * Returns a {@link Set} view of the mappings contained in this map.
   * The set is backed by the map, so changes to the map are
   * reflected in the set, and vice-versa.  If the map is modified
   * while an iteration over the set is in progress (except through
   * the iterator's own <tt>remove</tt> operation, or through the
   * <tt>setValue</tt> operation on a map entry returned by the
   * iterator) the results of the iteration are undefined.  The set
   * supports element removal, which removes the corresponding
   * mapping from the map, via the <tt>Iterator.remove</tt>,
   * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
   * <tt>clear</tt> operations.  It does not support the
   * <tt>add</tt> or <tt>addAll</tt> operations.
   *
   * @return a set view of the mappings contained in this map
   */
  @Override
  public Set<Entry<Long, T>> entrySet() {
    throw new UnsupportedOperationException("Unsupported operation.");
  }
}
