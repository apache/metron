package org.apache.metron.threatintel;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.reference.lookup.LookupKey;

/**
 * Created by cstella on 2/2/16.
 */
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

    public static ThreatIntelKey fromBytes(byte[] row) {
        ThreatIntelKey key = new ThreatIntelKey();
        key.indicator = Bytes.toString(row, HASH_PREFIX_SIZE, row.length - HASH_PREFIX_SIZE);
        return key;
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
