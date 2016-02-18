package org.apache.metron.hbase.converters;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.reference.lookup.LookupKV;
import org.apache.metron.reference.lookup.LookupKey;
import org.apache.metron.reference.lookup.LookupValue;

import java.io.IOException;

/**
 * Created by cstella on 2/17/16.
 */
public interface HbaseConverter<KEY_T extends LookupKey, VALUE_T extends LookupValue> {
    Put toPut(String columnFamily, KEY_T key, VALUE_T values) throws IOException;

    LookupKV<KEY_T, VALUE_T> fromPut(Put put, String columnFamily) throws IOException;

    Result toResult(String columnFamily, KEY_T key, VALUE_T values) throws IOException;

    LookupKV<KEY_T, VALUE_T> fromResult(Result result, String columnFamily) throws IOException;

    Get toGet(String columnFamily, KEY_T key);
}
