package org.apache.metron.dataloads.extractor;

import org.apache.metron.dataloads.hbase.ThreatIntelKey;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by cstella on 2/3/16.
 */
public class ExtractorResults {
    private ThreatIntelKey key;
    private Map<String, String> value;
    public ExtractorResults() {
        key = new ThreatIntelKey();
        value = new HashMap<>();
    }
    public ExtractorResults(ThreatIntelKey key, Map<String, String> value) {
        this.key = key;
        this.value = value;
    }

    public ThreatIntelKey getKey() {
        return key;
    }

    public Map<String, String> getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExtractorResults that = (ExtractorResults) o;

        if (getKey() != null ? !getKey().equals(that.getKey()) : that.getKey() != null) return false;
        return getValue() != null ? getValue().equals(that.getValue()) : that.getValue() == null;

    }

    @Override
    public int hashCode() {
        int result = getKey() != null ? getKey().hashCode() : 0;
        result = 31 * result + (getValue() != null ? getValue().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ExtractorResults{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }
}
