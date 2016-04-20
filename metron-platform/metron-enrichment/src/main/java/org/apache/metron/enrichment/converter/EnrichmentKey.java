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
package org.apache.metron.enrichment.converter;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.enrichment.lookup.LookupKey;

import java.io.*;

public class EnrichmentKey implements LookupKey {
  private static final int SEED = 0xDEADBEEF;
  private static final int HASH_PREFIX_SIZE=16;
  ThreadLocal<HashFunction> hFunction= new ThreadLocal<HashFunction>() {
    @Override
    protected HashFunction initialValue() {
      return Hashing.murmur3_128(SEED);
    }
  };

  public String indicator;
  public String type;

  public EnrichmentKey() {

  }
  public EnrichmentKey(String type, String indicator) {
    this.indicator = indicator;
    this.type = type;
  }

  private byte[] typedIndicatorToBytes() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream w = new DataOutputStream(baos);
    w.writeUTF(type);
    w.writeUTF(indicator);
    w.flush();
    return baos.toByteArray();
  }

  @Override
  public byte[] toBytes() {
    byte[] indicatorBytes = new byte[0];
    try {
      indicatorBytes = typedIndicatorToBytes();
    } catch (IOException e) {
      throw new RuntimeException("Unable to convert type and indicator to bytes", e);
    }
    Hasher hasher = hFunction.get().newHasher();
    hasher.putBytes(Bytes.toBytes(indicator));
    byte[] prefix = hasher.hash().asBytes();
    byte[] val = new byte[indicatorBytes.length + prefix.length];
    int offset = 0;
    System.arraycopy(prefix, 0, val, offset, prefix.length);
    offset += prefix.length;
    System.arraycopy(indicatorBytes, 0, val, offset, indicatorBytes.length);
    return val;
  }

  @Override
  public void fromBytes(byte[] row) {
    ByteArrayInputStream baos = new ByteArrayInputStream(row);
    baos.skip(HASH_PREFIX_SIZE);
    DataInputStream w = new DataInputStream(baos);
    try {
      type = w.readUTF();
      indicator = w.readUTF();
    } catch (IOException e) {
      throw new RuntimeException("Unable to convert type and indicator from bytes", e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    EnrichmentKey that = (EnrichmentKey) o;

    if (indicator != null ? !indicator.equals(that.indicator) : that.indicator != null) return false;
    return type != null ? type.equals(that.type) : that.type == null;

  }

  @Override
  public int hashCode() {
    int result = indicator != null ? indicator.hashCode() : 0;
    result = 31 * result + (type != null ? type.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "EnrichmentKey{" +
            "indicator='" + indicator + '\'' +
            ", type='" + type + '\'' +
            '}';
  }
}
