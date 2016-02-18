package org.apache.metron.hbase.converters.threatintel;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.hbase.converters.AbstractConverter;
import org.apache.metron.reference.lookup.LookupKV;

import java.io.IOException;

/**
 * Created by cstella on 2/2/16.
 */
public class ThreatIntelConverter extends AbstractConverter<ThreatIntelKey, ThreatIntelValue> {

    @Override
    public LookupKV<ThreatIntelKey, ThreatIntelValue> fromPut(Put put, String columnFamily) throws IOException {
        return fromPut(put, columnFamily, new ThreatIntelKey(), new ThreatIntelValue());
    }

    @Override
    public LookupKV<ThreatIntelKey, ThreatIntelValue> fromResult(Result result, String columnFamily) throws IOException {
        return fromResult(result, columnFamily, new ThreatIntelKey(), new ThreatIntelValue());
    }
}
