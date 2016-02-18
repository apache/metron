package org.apache.metron.dataloads.extractor.csv;

public enum LookupConverters {

    THREAT_INTEL(new ThreatIntelLookupConverter())
    ;
    LookupConverter converter;
    LookupConverters(LookupConverter converter) {
        this.converter = converter;
    }
    public LookupConverter getConverter() {
        return converter;
    }

}
