package org.apache.metron.threatintel.hbase;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.threatintel.ThreatIntelKey;
import org.apache.metron.threatintel.ThreatIntelResults;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.*;

/**
 * Created by cstella on 2/2/16.
 */
public enum Converter {
    INSTANCE;
    public static final String VALUE_COLUMN_NAME = "v";
    public static final byte[] VALUE_COLUMN_NAME_B = Bytes.toBytes(VALUE_COLUMN_NAME);
    public static final String LAST_SEEN_COLUMN_NAME = "t";
    public static final byte[] LAST_SEEN_COLUMN_NAME_B = Bytes.toBytes(LAST_SEEN_COLUMN_NAME);
    private static final ThreadLocal<ObjectMapper> _mapper = new ThreadLocal<ObjectMapper>() {
             @Override
             protected ObjectMapper initialValue() {
                return new ObjectMapper();
             }
    };
    public Put toPut(String columnFamily, ThreatIntelKey key, Map<String, String> value, Long lastSeenTimestamp) throws IOException {
        Put put = new Put(key.toBytes());
        byte[] cf = Bytes.toBytes(columnFamily);
        put.add(cf,VALUE_COLUMN_NAME_B, Bytes.toBytes(valueToString(value)));
        put.add(cf, LAST_SEEN_COLUMN_NAME_B, Bytes.toBytes(lastSeenTimestamp));
        return put;
    }

    public Map.Entry<ThreatIntelResults, Long> fromPut(Put put, String columnFamily) throws IOException {
        ThreatIntelKey key = ThreatIntelKey.fromBytes(put.getRow());
        Map<String, String> value = null;
        Long lastSeen = null;
        byte[] cf = Bytes.toBytes(columnFamily);
        List<Cell> cells = put.getFamilyCellMap().get(cf);
        for(Cell cell : cells) {
            if(Bytes.equals(cell.getQualifier(), VALUE_COLUMN_NAME_B)) {
                value = stringToValue(Bytes.toString(cell.getValue()));
            }
            else if(Bytes.equals(cell.getQualifier(), LAST_SEEN_COLUMN_NAME_B)) {
               lastSeen = Bytes.toLong(cell.getValue());
            }
        }
        return new AbstractMap.SimpleEntry<>(new ThreatIntelResults(key, value), lastSeen);
    }

    public Result toResult(String columnFamily, ThreatIntelKey key, Map<String, String> value, Long lastSeenTimestamp) throws IOException {
        Put put = toPut(columnFamily, key, value, lastSeenTimestamp);
        return Result.create(put.getFamilyCellMap().get(Bytes.toBytes(columnFamily)));
    }

    public Map.Entry<ThreatIntelResults, Long> fromResult(Result result, String columnFamily) throws IOException {
        ThreatIntelKey key = ThreatIntelKey.fromBytes(result.getRow());
        byte[] cf = Bytes.toBytes(columnFamily);
        NavigableMap<byte[], byte[]> cols = result.getFamilyMap(cf);
        Map<String, String> value = stringToValue(Bytes.toString(cols.get(VALUE_COLUMN_NAME_B)));
        ThreatIntelResults results = new ThreatIntelResults(key, value);
        return new AbstractMap.SimpleEntry<>(results, Bytes.toLong(cols.get(LAST_SEEN_COLUMN_NAME_B)));
    }

    public Get toGet(String columnFamily, ThreatIntelKey key) {
        Get ret = new Get(key.toBytes());
        ret.addFamily(Bytes.toBytes(columnFamily));
        return ret;
    }

    public Map<String, String> stringToValue(String s) throws IOException {
        return _mapper.get().readValue(s, new TypeReference<Map<String, String>>(){});
    }
    public String valueToString(Map<String, String> value) throws IOException {
        return _mapper.get().writeValueAsString(value);
    }
}
