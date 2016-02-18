package org.apache.metron.dataloads.extractor.csv;

import org.apache.metron.hbase.converters.threatintel.ThreatIntelKey;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelValue;
import org.apache.metron.reference.lookup.LookupKey;
import org.apache.metron.reference.lookup.LookupValue;

import java.util.Map;

public enum LookupConverters {

    THREAT_INTEL(new LookupConverter() {
        @Override
        public LookupKey toKey(String indicator) {
            return new ThreatIntelKey(indicator);
        }

        @Override
        public LookupValue toValue(Map<String, String> metadata) {
            return new ThreatIntelValue(metadata);
        }
    })
    ;
    LookupConverter converter;
    LookupConverters(LookupConverter converter) {
        this.converter = converter;
    }
    public LookupConverter getConverter() {
        return converter;
    }

    public static LookupConverter getConverter(String name) {
        try {
            return LookupConverters.valueOf(name).getConverter();
        }
        catch(Throwable t) {
            try {
                return (LookupConverter) Class.forName(name).newInstance();
            } catch (InstantiationException e) {
                throw new IllegalStateException("Unable to parse " + name, e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unable to parse " + name, e);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unable to parse " + name, e);
            }
        }
    }

}
