package org.apache.metron.reference.lookup;

import java.io.Serializable;

public class LookupKV<KEY_T extends LookupKey, VALUE_T extends LookupValue> implements Serializable {
    private KEY_T key;
    private VALUE_T value;
    public LookupKV(KEY_T key, VALUE_T value) {
        this.key = key;
        this.value = value;
    }

    public KEY_T getKey() {
        return key;
    }

    public VALUE_T getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LookupKV<?, ?> lookupKV = (LookupKV<?, ?>) o;

        if (key != null ? !key.equals(lookupKV.key) : lookupKV.key != null) return false;
        return value != null ? value.equals(lookupKV.value) : lookupKV.value == null;

    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "LookupKV{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }
}
