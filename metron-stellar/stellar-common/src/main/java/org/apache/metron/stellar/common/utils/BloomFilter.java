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

import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

import java.io.Serializable;
import java.util.function.Function;

public class BloomFilter<T> implements Serializable {

  private static class BloomFunnel<T> implements Funnel<T>, Serializable {

    Function<T, byte[]> serializer;

    public BloomFunnel(Function<T, byte[]> serializer) {
      this.serializer = serializer;
    }

    @Override
    public void funnel(T obj, PrimitiveSink primitiveSink) {
      primitiveSink.putBytes(serializer.apply(obj));
    }

    @Override
    public boolean equals(Object obj) {
      return this.getClass().equals(obj.getClass());
    }

    @Override
    public int hashCode() {
      return super.hashCode() * 31;
    }
  }

  public static class DefaultSerializer<T> implements Function<T, byte[]>, Serializable {
    @Override
    public byte[] apply(T t) {
      return SerDeUtils.toBytes(t);
    }
  }

  private com.google.common.hash.BloomFilter<T> filter;

  public BloomFilter(Function<T, byte[]> serializer, int expectedInsertions, double falsePositiveRate) {
    filter = com.google.common.hash.BloomFilter.create(new BloomFunnel<T>(serializer), expectedInsertions, falsePositiveRate);
  }

  public boolean mightContain(T key) {
    return filter.mightContain(key);
  }

  public void add(T key) {
    filter.put(key);
  }

  public void merge(BloomFilter<T> filter2) {
    filter.putAll(filter2.filter);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BloomFilter<?> that = (BloomFilter<?>) o;

    return filter != null ? filter.equals(that.filter) : that.filter == null;

  }

  @Override
  public int hashCode() {
    return filter != null ? filter.hashCode() : 0;
  }
}
