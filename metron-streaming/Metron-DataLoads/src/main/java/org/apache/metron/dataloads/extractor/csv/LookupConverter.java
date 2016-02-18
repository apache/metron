package org.apache.metron.dataloads.extractor.csv;

import org.apache.metron.hbase.converters.HbaseConverter;
import org.apache.metron.reference.lookup.LookupKey;
import org.apache.metron.reference.lookup.LookupValue;

import java.util.Map;

public interface LookupConverter {
    LookupKey toKey(String indicator);
    LookupValue toValue(Map<String, String> metadata);
}
