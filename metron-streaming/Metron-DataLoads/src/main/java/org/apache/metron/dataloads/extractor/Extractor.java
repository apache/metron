package org.apache.metron.dataloads.extractor;

import org.apache.metron.threatintel.ThreatIntelResults;

import java.io.IOException;
import java.util.Map;

/**
 * Created by cstella on 2/2/16.
 */
public interface Extractor {
    Iterable<ThreatIntelResults> extract(String line) throws IOException;
    void initialize(Map<String, Object> config);
}
