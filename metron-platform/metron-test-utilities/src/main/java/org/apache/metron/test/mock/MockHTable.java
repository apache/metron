/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.test.mock;


import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.Service;
import com.google.protobuf.ServiceException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.ipc.CoprocessorRpcChannel;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * MockHTable.
 *
 * This implementation is a selected excerpt from https://gist.github.com/agaoglu/613217
 */
public class MockHTable implements HTableInterface {

    public static class Provider implements Serializable {
        private static Map<String, HTableInterface> _cache = new HashMap<>();
        public HTableInterface getTable(Configuration config, String tableName) throws IOException {
            return _cache.get(tableName);
        }
        public static HTableInterface getFromCache(String tableName) {
            return _cache.get(tableName);
        }
        public static HTableInterface addToCache(String tableName, String... columnFamilies) {
            MockHTable ret =  new MockHTable(tableName, columnFamilies);
            _cache.put(tableName, ret);
            return ret;
        }

        public static void clear() {
            _cache.clear();
        }
    }

    private final String tableName;
    private final List<String> columnFamilies = new ArrayList<>();
    private HColumnDescriptor[] descriptors;

    private NavigableMap<byte[], NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>>> data
            = new TreeMap<>(Bytes.BYTES_COMPARATOR);

    private static List<KeyValue> toKeyValue(byte[] row, NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> rowdata, int maxVersions) {
        return toKeyValue(row, rowdata, 0, Long.MAX_VALUE, maxVersions);
    }

    private static List<KeyValue> toKeyValue(byte[] row, NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> rowdata, long timestampStart, long timestampEnd, int maxVersions) {
        List<KeyValue> ret = new ArrayList<KeyValue>();
        for (byte[] family : rowdata.keySet())
            for (byte[] qualifier : rowdata.get(family).keySet()) {
                int versionsAdded = 0;
                for (Map.Entry<Long, byte[]> tsToVal : rowdata.get(family).get(qualifier).descendingMap().entrySet()) {
                    if (versionsAdded++ == maxVersions)
                        break;
                    Long timestamp = tsToVal.getKey();
                    if (timestamp < timestampStart)
                        continue;
                    if (timestamp > timestampEnd)
                        continue;
                    byte[] value = tsToVal.getValue();
                    ret.add(new KeyValue(row, family, qualifier, timestamp, value));
                }
            }
        return ret;
    }
    public MockHTable(String tableName) {
        this.tableName = tableName;
    }

    public MockHTable(String tableName, String... columnFamilies) {
        this.tableName = tableName;
        for(String cf : columnFamilies) {
            addColumnFamily(cf);
        }
    }

    public void addColumnFamily(String columnFamily) {
        this.columnFamilies.add(columnFamily);
        descriptors = new HColumnDescriptor[columnFamilies.size()];
        int i = 0;
        for(String cf : columnFamilies) {
            descriptors[i++] = new HColumnDescriptor(cf);
        }
    }


    @Override
    public byte[] getTableName() {
        return Bytes.toBytes(tableName);
    }

    @Override
    public TableName getName() {
        return TableName.valueOf(tableName);
    }

    @Override
    public Configuration getConfiguration() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HTableDescriptor getTableDescriptor() throws IOException {
        HTableDescriptor ret = new HTableDescriptor(tableName);
        for(HColumnDescriptor c : descriptors) {
            ret.addFamily(c);
        }
        return ret;
    }

    @Override
    public boolean exists(Get get) throws IOException {
        if(get.getFamilyMap() == null || get.getFamilyMap().size() == 0) {
            return data.containsKey(get.getRow());
        } else {
            byte[] row = get.getRow();
            if(!data.containsKey(row)) {
                return false;
            }
            for(byte[] family : get.getFamilyMap().keySet()) {
                if(!data.get(row).containsKey(family)) {
                    return false;
                } else {
                    return true;
                }
            }
            return true;
        }
    }

    /**
     * Test for the existence of columns in the table, as specified by the Gets.
     * <p/>
     * <p/>
     * This will return an array of booleans. Each value will be true if the related Get matches
     * one or more keys, false if not.
     * <p/>
     * <p/>
     * This is a server-side call so it prevents any data from being transferred to
     * the client.
     *
     * @param gets the Gets
     * @return Array of boolean.  True if the specified Get matches one or more keys, false if not.
     * @throws IOException e
     */
    @Override
    public boolean[] existsAll(List<Get> gets) throws IOException {
        boolean[] ret = new boolean[gets.size()];
        int i = 0;
        for(boolean b : exists(gets)) {
           ret[i++] = b;
        }
        return ret;
    }

    @Override
    public Boolean[] exists(List<Get> list) throws IOException {
        Boolean[] ret = new Boolean[list.size()];
        int i = 0;
        for(Get g : list) {
           ret[i++] = exists(g);
        }
        return ret;
    }

    @Override
    public void batch(List<? extends Row> list, Object[] objects) throws IOException, InterruptedException {
        throw new UnsupportedOperationException();

    }

    /**
     * @param actions
     * @deprecated
     */
    @Override
    public Object[] batch(List<? extends Row> actions) throws IOException, InterruptedException {
        List<Result> results = new ArrayList<Result>();
        for (Row r : actions) {
            if (r instanceof Delete) {
                delete((Delete) r);
                continue;
            }
            if (r instanceof Put) {
                put((Put) r);
                continue;
            }
            if (r instanceof Get) {
                results.add(get((Get) r));
            }
        }
        return results.toArray();
    }

    @Override
    public <R> void batchCallback(List<? extends Row> list, Object[] objects, Batch.Callback<R> callback) throws IOException, InterruptedException {
        throw new UnsupportedOperationException();

    }

    /**
     * @param list
     * @param callback
     * @deprecated
     */
    @Override
    public <R> Object[] batchCallback(List<? extends Row> list, Batch.Callback<R> callback) throws IOException, InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result get(Get get) throws IOException {
        if (!data.containsKey(get.getRow()))
            return new Result();
        byte[] row = get.getRow();
        List<KeyValue> kvs = new ArrayList<KeyValue>();
        if (!get.hasFamilies()) {
            kvs = toKeyValue(row, data.get(row), get.getMaxVersions());
        } else {
            for (byte[] family : get.getFamilyMap().keySet()){
                if (data.get(row).get(family) == null)
                    continue;
                NavigableSet<byte[]> qualifiers = get.getFamilyMap().get(family);
                if (qualifiers == null || qualifiers.isEmpty())
                    qualifiers = data.get(row).get(family).navigableKeySet();
                for (byte[] qualifier : qualifiers){
                    if (qualifier == null)
                        qualifier = "".getBytes();
                    if (!data.get(row).containsKey(family) ||
                            !data.get(row).get(family).containsKey(qualifier) ||
                            data.get(row).get(family).get(qualifier).isEmpty())
                        continue;
                    Map.Entry<Long, byte[]> timestampAndValue = data.get(row).get(family).get(qualifier).lastEntry();
                    kvs.add(new KeyValue(row,family, qualifier, timestampAndValue.getKey(), timestampAndValue.getValue()));
                }
            }
        }
        Filter filter = get.getFilter();
        if (filter != null) {
            filter.reset();
            List<KeyValue> nkvs = new ArrayList<KeyValue>(kvs.size());
            for (KeyValue kv : kvs) {
                if (filter.filterAllRemaining()) {
                    break;
                }
                if (filter.filterRowKey(kv.getBuffer(), kv.getRowOffset(), kv.getRowLength())) {
                    continue;
                }
                if (filter.filterKeyValue(kv) == Filter.ReturnCode.INCLUDE) {
                    nkvs.add(kv);
                }
                // ignoring next key hint which is a optimization to reduce file system IO
            }
            if (filter.hasFilterRow()) {
                filter.filterRow();
            }
            kvs = nkvs;
        }

        return new Result(kvs);
    }

    @Override
    public Result[] get(List<Get> list) throws IOException {
        Result[] ret = new Result[list.size()];
        int i = 0;
        for(Get g : list) {
            ret[i++] = get(g);
        }
        return ret;
    }

    /**
     * @param bytes
     * @param bytes1
     * @deprecated
     */
    @Override
    public Result getRowOrBefore(byte[] bytes, byte[] bytes1) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResultScanner getScanner(Scan scan) throws IOException {
        final List<Result> ret = new ArrayList<Result>();
        byte[] st = scan.getStartRow();
        byte[] sp = scan.getStopRow();
        Filter filter = scan.getFilter();

        for (byte[] row : data.keySet()){
            // if row is equal to startRow emit it. When startRow (inclusive) and
            // stopRow (exclusive) is the same, it should not be excluded which would
            // happen w/o this control.
            if (st != null && st.length > 0 &&
                    Bytes.BYTES_COMPARATOR.compare(st, row) != 0) {
                // if row is before startRow do not emit, pass to next row
                if (st != null && st.length > 0 &&
                        Bytes.BYTES_COMPARATOR.compare(st, row) > 0)
                    continue;
                // if row is equal to stopRow or after it do not emit, stop iteration
                if (sp != null && sp.length > 0 &&
                        Bytes.BYTES_COMPARATOR.compare(sp, row) <= 0)
                    break;
            }

            List<KeyValue> kvs = null;
            if (!scan.hasFamilies()) {
                kvs = toKeyValue(row, data.get(row), scan.getTimeRange().getMin(), scan.getTimeRange().getMax(), scan.getMaxVersions());
            } else {
                kvs = new ArrayList<KeyValue>();
                for (byte[] family : scan.getFamilyMap().keySet()){
                    if (data.get(row).get(family) == null)
                        continue;
                    NavigableSet<byte[]> qualifiers = scan.getFamilyMap().get(family);
                    if (qualifiers == null || qualifiers.isEmpty())
                        qualifiers = data.get(row).get(family).navigableKeySet();
                    for (byte[] qualifier : qualifiers){
                        if (data.get(row).get(family).get(qualifier) == null)
                            continue;
                        for (Long timestamp : data.get(row).get(family).get(qualifier).descendingKeySet()){
                            if (timestamp < scan.getTimeRange().getMin())
                                continue;
                            if (timestamp > scan.getTimeRange().getMax())
                                continue;
                            byte[] value = data.get(row).get(family).get(qualifier).get(timestamp);
                            kvs.add(new KeyValue(row, family, qualifier, timestamp, value));
                            if(kvs.size() == scan.getMaxVersions()) {
                                break;
                            }
                        }
                    }
                }
            }
            if (filter != null) {
                filter.reset();
                List<KeyValue> nkvs = new ArrayList<KeyValue>(kvs.size());
                for (KeyValue kv : kvs) {
                    if (filter.filterAllRemaining()) {
                        break;
                    }
                    if (filter.filterRowKey(kv.getBuffer(), kv.getRowOffset(), kv.getRowLength())) {
                        continue;
                    }
                    Filter.ReturnCode filterResult = filter.filterKeyValue(kv);
                    if (filterResult == Filter.ReturnCode.INCLUDE) {
                        nkvs.add(kv);
                    } else if (filterResult == Filter.ReturnCode.NEXT_ROW) {
                        break;
                    }
                    // ignoring next key hint which is a optimization to reduce file system IO
                }
                if (filter.hasFilterRow()) {
                    filter.filterRow();
                }
                kvs = nkvs;
            }
            if (!kvs.isEmpty()) {
                ret.add(new Result(kvs));
            }
        }

        return new ResultScanner() {
            private final Iterator<Result> iterator = ret.iterator();
            public Iterator<Result> iterator() {
                return iterator;
            }
            public Result[] next(int nbRows) throws IOException {
                ArrayList<Result> resultSets = new ArrayList<Result>(nbRows);
                for(int i = 0; i < nbRows; i++) {
                    Result next = next();
                    if (next != null) {
                        resultSets.add(next);
                    } else {
                        break;
                    }
                }
                return resultSets.toArray(new Result[resultSets.size()]);
            }
            public Result next() throws IOException {
                try {
                    return iterator().next();
                } catch (NoSuchElementException e) {
                    return null;
                }
            }
            public void close() {}
        };
    }
    @Override
    public ResultScanner getScanner(byte[] family) throws IOException {
        Scan scan = new Scan();
        scan.addFamily(family);
        return getScanner(scan);
    }

    @Override
    public ResultScanner getScanner(byte[] family, byte[] qualifier)
            throws IOException {
        Scan scan = new Scan();
        scan.addColumn(family, qualifier);
        return getScanner(scan);
    }

    List<Put> putLog = new ArrayList<>();

    public List<Put> getPutLog() {
        return putLog;
    }

    @Override
    public void put(Put put) throws IOException {
        putLog.add(put);
        byte[] row = put.getRow();
        NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> rowData = forceFind(data, row, new TreeMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>>(Bytes.BYTES_COMPARATOR));
        for (byte[] family : put.getFamilyMap().keySet()){
            NavigableMap<byte[], NavigableMap<Long, byte[]>> familyData = forceFind(rowData, family, new TreeMap<byte[], NavigableMap<Long, byte[]>>(Bytes.BYTES_COMPARATOR));
            for (KeyValue kv : put.getFamilyMap().get(family)){
                kv.updateLatestStamp(Bytes.toBytes(System.currentTimeMillis()));
                byte[] qualifier = kv.getQualifier();
                NavigableMap<Long, byte[]> qualifierData = forceFind(familyData, qualifier, new TreeMap<Long, byte[]>());
                qualifierData.put(kv.getTimestamp(), kv.getValue());
            }
        }
    }

    /**
     * Helper method to find a key in a map. If key is not found, newObject is
     * added to map and returned
     *
     * @param map
     *          map to extract value from
     * @param key
     *          key to look for
     * @param newObject
     *          set key to this if not found
     * @return found value or newObject if not found
     */
    private <K, V> V forceFind(NavigableMap<K, V> map, K key, V newObject){
        V data = map.get(key);
        if (data == null){
            data = newObject;
            map.put(key, data);
        }
        return data;
    }

    @Override
    public void put(List<Put> puts) throws IOException {
        for (Put put : puts)
            put(put);
    }

    @Override
    public boolean checkAndPut(byte[] bytes, byte[] bytes1, byte[] bytes2, byte[] bytes3, Put put) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Atomically checks if a row/family/qualifier value matches the expected
     * value. If it does, it adds the put.  If the passed value is null, the check
     * is for the lack of column (ie: non-existance)
     *
     * @param row       to check
     * @param family    column family to check
     * @param qualifier column qualifier to check
     * @param compareOp comparison operator to use
     * @param value     the expected value
     * @param put       data to put if check succeeds
     * @return true if the new put was executed, false otherwise
     * @throws IOException e
     */
    @Override
    public boolean checkAndPut(byte[] row, byte[] family, byte[] qualifier, CompareFilter.CompareOp compareOp, byte[] value, Put put) throws IOException {
        return false;
    }

    @Override
    public void delete(Delete delete) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(List<Delete> list) throws IOException {
        throw new UnsupportedOperationException();

    }

    @Override
    public boolean checkAndDelete(byte[] bytes, byte[] bytes1, byte[] bytes2, byte[] bytes3, Delete delete) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Atomically checks if a row/family/qualifier value matches the expected
     * value. If it does, it adds the delete.  If the passed value is null, the
     * check is for the lack of column (ie: non-existance)
     *
     * @param row       to check
     * @param family    column family to check
     * @param qualifier column qualifier to check
     * @param compareOp comparison operator to use
     * @param value     the expected value
     * @param delete    data to delete if check succeeds
     * @return true if the new delete was executed, false otherwise
     * @throws IOException e
     */
    @Override
    public boolean checkAndDelete(byte[] row, byte[] family, byte[] qualifier, CompareFilter.CompareOp compareOp, byte[] value, Delete delete) throws IOException {
        return false;
    }

    @Override
    public void mutateRow(RowMutations rowMutations) throws IOException {
        throw new UnsupportedOperationException();

    }

    @Override
    public Result append(Append append) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result increment(Increment increment) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long incrementColumnValue(byte[] bytes, byte[] bytes1, byte[] bytes2, long l) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long incrementColumnValue(byte[] bytes, byte[] bytes1, byte[] bytes2, long l, Durability durability) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * @param bytes
     * @param bytes1
     * @param bytes2
     * @param l
     * @param b
     * @deprecated
     */
    @Override
    public long incrementColumnValue(byte[] bytes, byte[] bytes1, byte[] bytes2, long l, boolean b) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAutoFlush() {
        return autoflush;
    }

    @Override
    public void flushCommits() throws IOException {

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public CoprocessorRpcChannel coprocessorService(byte[] bytes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Service, R> Map<byte[], R> coprocessorService(Class<T> aClass, byte[] bytes, byte[] bytes1, Batch.Call<T, R> call) throws ServiceException, Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Service, R> void coprocessorService(Class<T> aClass, byte[] bytes, byte[] bytes1, Batch.Call<T, R> call, Batch.Callback<R> callback) throws ServiceException, Throwable {
        throw new UnsupportedOperationException();
    }

    boolean autoflush = true;

    /**
     * @param b
     * @deprecated
     */
    @Override
    public void setAutoFlush(boolean b) {
        autoflush = b;
    }

    @Override
    public void setAutoFlush(boolean b, boolean b1) {
        autoflush = b;
    }

    @Override
    public void setAutoFlushTo(boolean b) {
        autoflush = b;
    }

    long writeBufferSize = 0;
    @Override
    public long getWriteBufferSize() {
        return writeBufferSize;
    }

    @Override
    public void setWriteBufferSize(long l) throws IOException {
        writeBufferSize = l;
    }

    @Override
    public <R extends Message> Map<byte[], R> batchCoprocessorService(Descriptors.MethodDescriptor methodDescriptor, Message message, byte[] bytes, byte[] bytes1, R r) throws ServiceException, Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R extends Message> void batchCoprocessorService(Descriptors.MethodDescriptor methodDescriptor, Message message, byte[] bytes, byte[] bytes1, R r, Batch.Callback<R> callback) throws ServiceException, Throwable {
        throw new UnsupportedOperationException();
    }

    /**
     * Atomically checks if a row/family/qualifier value matches the expected value.
     * If it does, it performs the row mutations.  If the passed value is null, the check
     * is for the lack of column (ie: non-existence)
     *
     * @param row       to check
     * @param family    column family to check
     * @param qualifier column qualifier to check
     * @param compareOp the comparison operator
     * @param value     the expected value
     * @param mutation  mutations to perform if check succeeds
     * @return true if the new put was executed, false otherwise
     * @throws IOException e
     */
    @Override
    public boolean checkAndMutate(byte[] row, byte[] family, byte[] qualifier, CompareFilter.CompareOp compareOp, byte[] value, RowMutations mutation) throws IOException {
        return false;
    }
}
