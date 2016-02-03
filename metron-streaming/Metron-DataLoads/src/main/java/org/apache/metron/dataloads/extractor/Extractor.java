package org.apache.metron.dataloads.extractor;

import org.apache.metron.dataloads.hbase.ThreatIntelKey;

import java.io.IOException;
import java.util.Map;

/**
 * Created by cstella on 2/2/16.
 */
public interface Extractor {
    ExtractorResults extract(String line) throws IOException;
    void initialize(Map<String, Object> config);
}
