package org.apache.metron.integration.util.threatintel;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.metron.threatintel.ThreatIntelResults;
import org.apache.metron.threatintel.hbase.Converter;

import java.io.IOException;

/**
 * Created by cstella on 2/11/16.
 */
public enum ThreatIntelHelper {
    INSTANCE;

    public void load(HTableInterface table, String cf, Iterable<ThreatIntelResults> results, long ts) throws IOException {
        for(ThreatIntelResults result : results) {
            Put put = Converter.INSTANCE.toPut(cf, result.getKey(), result.getValue(), ts);
            table.put(put);
        }
    }
}
