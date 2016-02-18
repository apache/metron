package org.apache.metron.integration.util.threatintel;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelKey;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelValue;
import org.apache.metron.reference.lookup.LookupKV;
import org.apache.metron.threatintel.ThreatIntelResults;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelConverter;

import java.io.IOException;

/**
 * Created by cstella on 2/11/16.
 */
public enum ThreatIntelHelper {
    INSTANCE;
    ThreatIntelConverter converter = new ThreatIntelConverter();

    public void load(HTableInterface table, String cf, Iterable<LookupKV<ThreatIntelKey, ThreatIntelValue>> results) throws IOException {
        for(LookupKV<ThreatIntelKey, ThreatIntelValue> result : results) {
            Put put = converter.toPut(cf, result.getKey(), result.getValue());
            table.put(put);
        }
    }
}
