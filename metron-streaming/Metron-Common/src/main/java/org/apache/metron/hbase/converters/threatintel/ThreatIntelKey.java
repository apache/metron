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
package org.apache.metron.hbase.converters.threatintel;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.reference.lookup.LookupKey;

public class ThreatIntelKey implements LookupKey{
    private static final int SEED = 0xDEADBEEF;
    private static final int HASH_PREFIX_SIZE=16;
    ThreadLocal<HashFunction> hFunction= new ThreadLocal<HashFunction>() {
        @Override
        protected HashFunction initialValue() {
            return Hashing.murmur3_128(SEED);
        }
    };
    public ThreatIntelKey() {

    }
    public ThreatIntelKey(String indicator) {
        this.indicator = indicator;
    }

    public String indicator;

    @Override
    public byte[] toBytes() {
        byte[] indicatorBytes = Bytes.toBytes(indicator);
        Hasher hasher = hFunction.get().newHasher();
        hasher.putBytes(Bytes.toBytes(indicator));
        byte[] prefix = hasher.hash().asBytes();
        byte[] val = new byte[indicatorBytes.length + prefix.length];
        int pos = 0;
        for(int i = 0;pos < prefix.length;++pos,++i) {
            val[pos] = prefix[i];
        }
        for(int i = 0;i < indicatorBytes.length;++pos,++i) {
            val[pos] = indicatorBytes[i];
        }
        return val;
    }

    @Override
    public void fromBytes(byte[] row) {
        ThreatIntelKey key = this;
        key.indicator = Bytes.toString(row, HASH_PREFIX_SIZE, row.length - HASH_PREFIX_SIZE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ThreatIntelKey that = (ThreatIntelKey) o;

        return indicator != null ? indicator.equals(that.indicator) : that.indicator == null;

    }

    @Override
    public int hashCode() {
        return indicator != null ? indicator.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ThreatIntelKey{" +
                "indicator='" + indicator + '\'' +
                '}';
    }
}
