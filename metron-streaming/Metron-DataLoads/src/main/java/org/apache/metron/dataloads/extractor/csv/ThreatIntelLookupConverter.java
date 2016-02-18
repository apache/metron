package org.apache.metron.dataloads.extractor.csv;

import org.apache.metron.hbase.converters.threatintel.ThreatIntelConverter;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelKey;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelValue;
import org.apache.metron.reference.lookup.LookupKey;
import org.apache.metron.reference.lookup.LookupValue;

import java.util.Map;

public class ThreatIntelLookupConverter implements LookupConverter {
    static ThreatIntelConverter converter = new ThreatIntelConverter();
    @Override
    public LookupKey toKey(String indicator) {
        return new ThreatIntelKey(indicator);
    }

    @Override
    public LookupValue toValue(Map<String, String> metadata) {
        return new ThreatIntelValue(metadata);
    }
}
