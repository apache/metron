package org.apache.metron.threatintel.hbase;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.hbase.converters.HbaseConverter;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelConverter;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelValue;
import org.apache.metron.reference.lookup.Lookup;
import org.apache.metron.reference.lookup.LookupKV;
import org.apache.metron.reference.lookup.accesstracker.AccessTracker;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelKey;

import java.io.IOException;

/**
 * Created by cstella on 2/5/16.
 */
public class ThreatIntelLookup extends Lookup<HTableInterface, ThreatIntelKey, LookupKV<ThreatIntelKey,ThreatIntelValue>> implements AutoCloseable {



    public static class Handler implements org.apache.metron.reference.lookup.handler.Handler<HTableInterface,ThreatIntelKey,LookupKV<ThreatIntelKey,ThreatIntelValue>> {
        String columnFamily;
        HbaseConverter<ThreatIntelKey, ThreatIntelValue> converter = new ThreatIntelConverter();
        public Handler(String columnFamily) {
            this.columnFamily = columnFamily;
        }
        @Override
        public boolean exists(ThreatIntelKey key, HTableInterface table, boolean logAccess) throws IOException {
            return table.exists(converter.toGet(columnFamily, key));
        }

        @Override
        public LookupKV<ThreatIntelKey, ThreatIntelValue> get(ThreatIntelKey key, HTableInterface table, boolean logAccess) throws IOException {
            return converter.fromResult(table.get(converter.toGet(columnFamily, key)), columnFamily);
        }


        @Override
        public void close() throws Exception {

        }
    }
    private HTableInterface table;
    public ThreatIntelLookup(HTableInterface table, String columnFamily, AccessTracker tracker) {
        this.table = table;
        this.setLookupHandler(new Handler(columnFamily));
        this.setAccessTracker(tracker);
    }

    public HTableInterface getTable() {
        return table;
    }

    @Override
    public void close() throws Exception {
        super.close();
        table.close();
    }
}
