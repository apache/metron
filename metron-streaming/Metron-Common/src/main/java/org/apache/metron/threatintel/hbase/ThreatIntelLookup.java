package org.apache.metron.threatintel.hbase;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.metron.reference.lookup.Lookup;
import org.apache.metron.reference.lookup.LookupKey;
import org.apache.metron.reference.lookup.accesstracker.AccessTracker;
import org.apache.metron.reference.lookup.handler.Handler;
import org.apache.metron.threatintel.ThreatIntelKey;
import org.apache.metron.threatintel.ThreatIntelResults;

import java.io.IOException;
import java.util.Map;

/**
 * Created by cstella on 2/5/16.
 */
public class ThreatIntelLookup extends Lookup<HTable, ThreatIntelKey, Map.Entry<ThreatIntelResults, Long>> {
    public static class Handler implements org.apache.metron.reference.lookup.handler.Handler<HTable, ThreatIntelKey, Map.Entry<ThreatIntelResults, Long>> {
        String columnFamily;
        public Handler(String columnFamily) {
            this.columnFamily = columnFamily;
        }
        @Override
        public boolean exists(ThreatIntelKey key, HTable table) throws IOException {
            return table.exists(Converter.INSTANCE.toGet(columnFamily, key));
        }

        @Override
        public Map.Entry<ThreatIntelResults,Long> get(ThreatIntelKey key, HTable table) throws IOException {
            return Converter.INSTANCE.fromResult(table.get(Converter.INSTANCE.toGet(columnFamily, key)), columnFamily);
        }
    }
    private HTable table;
    public ThreatIntelLookup(HTable table, String columnFamily, AccessTracker tracker) {
        this.table = table;
        this.setLookupHandler(new Handler(columnFamily));
        this.setAccessTracker(tracker);
    }
}
