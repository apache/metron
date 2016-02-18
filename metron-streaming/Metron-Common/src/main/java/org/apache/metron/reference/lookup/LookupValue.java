package org.apache.metron.reference.lookup;

import java.util.Map;
import java.util.NavigableMap;

public interface LookupValue {
    Iterable<Map.Entry<byte[], byte[]>> toColumns();
    void fromColumns(Iterable<Map.Entry<byte[], byte[]>> values);
}
