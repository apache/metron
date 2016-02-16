package org.apache.metron.dataloads.extractor.stix.types;

import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.threatintel.ThreatIntelResults;
import org.mitre.cybox.common_2.ObjectPropertiesType;

import java.io.IOException;
import java.util.Map;

/**
 * Created by cstella on 2/9/16.
 */
public interface ObjectTypeHandler<T extends ObjectPropertiesType> {
    Iterable<ThreatIntelResults> extract(T type, Map<String, Object> config) throws IOException;
    Class<T> getTypeClass();
}
