package org.apache.metron.hbase.converters.enrichment;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.hbase.converters.AbstractConverter;
import org.apache.metron.reference.lookup.LookupKV;

import java.io.IOException;

public class EnrichmentConverter extends AbstractConverter<EnrichmentKey, EnrichmentValue> {

    @Override
    public LookupKV<EnrichmentKey, EnrichmentValue> fromPut(Put put, String columnFamily) throws IOException {
        return fromPut(put, columnFamily, new EnrichmentKey(), new EnrichmentValue());
    }

    @Override
    public LookupKV<EnrichmentKey, EnrichmentValue> fromResult(Result result, String columnFamily) throws IOException {
        return fromResult(result, columnFamily, new EnrichmentKey(), new EnrichmentValue());
    }
}
