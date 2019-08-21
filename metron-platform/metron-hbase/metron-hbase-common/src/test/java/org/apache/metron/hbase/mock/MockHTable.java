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
package org.apache.metron.hbase.mock;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.Service;
import com.google.protobuf.ServiceException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Append;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Row;
import org.apache.hadoop.hbase.client.RowMutations;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.client.metrics.ScanMetrics;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.ipc.CoprocessorRpcChannel;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * MockHTable.
 *
 * This implementation is a selected excerpt from https://gist.github.com/agaoglu/613217 and
 * https://github.com/rayokota/hgraphdb/blob/07c551f39a92b7ee2c8b48edcc7c0b314f6c3e33/src/main/java/org/apache/hadoop/hbase/client/mock/MockHTable.java.
 */
public class MockHTable implements Table {

    private final TableName tableName;
    private final List<String> columnFamilies = new ArrayList<>();
    private Configuration config;
    private List<Put> putLog;
    private final NavigableMap<byte[], NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>>> data
            = new ConcurrentSkipListMap<>(Bytes.BYTES_COMPARATOR);

    private static List<Cell> toKeyValue(byte[] row, NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> rowdata, int maxVersions) {
        return toKeyValue(row, rowdata, 0, Long.MAX_VALUE, maxVersions);
    }

    private static List<Cell> toKeyValue(byte[] row, NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> rowdata, long timestampStart, long timestampEnd, int maxVersions) {
        List<Cell> ret = new ArrayList<>();
        for (byte[] family : rowdata.keySet()) {
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
        }
        return ret;
    }

    @SuppressWarnings("WeakerAccess")
    public MockHTable(TableName tableName) {
        this.tableName = tableName;
        this.putLog = new ArrayList<>();
    }

    public MockHTable(TableName tableName, String... columnFamilies) {
        this.tableName = tableName;
        this.columnFamilies.addAll(Arrays.asList(columnFamilies));
        this.putLog = new ArrayList<>();
    }

    @SuppressWarnings("WeakerAccess")
    public MockHTable(TableName tableName, List<String> columnFamilies) {
        this.tableName = tableName;
        this.columnFamilies.addAll(columnFamilies);
        this.putLog = new ArrayList<>();
    }

    public int size() {
        return data.size();
    }

    public byte[] getTableName() {
        return getName().getName();
    }

    @Override
    public TableName getName() {
        return tableName;
    }

    @Override
    public Configuration getConfiguration() {
        return config;
    }

    public MockHTable setConfiguration(Configuration config) {
        this.config = config;
        return this;
    }

    @Override
    public HTableDescriptor getTableDescriptor() throws IOException {
        HTableDescriptor table = new HTableDescriptor(tableName);
        for (String columnFamily : columnFamilies) {
            table.addFamily(new HColumnDescriptor(columnFamily));
        }
        return table;
    }

    @Override
    public TableDescriptor getDescriptor() throws IOException {
        return getTableDescriptor();
    }

    @Override
    public boolean exists(Get get) throws IOException {
        Result result = get(get);
        return result != null && !result.isEmpty();
    }

    /**
     * Test for the existence of columns in the table, as specified by the Gets.
     *
     * <p>This will return an array of booleans. Each value will be true if the related Get matches
     * one or more keys, false if not.
     *
     * <p>This is a server-side call so it prevents any data from being transferred to
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
    public boolean[] exists(List<Get> list) throws IOException {
        boolean[] ret = new boolean[list.size()];
        int i = 0;
        for(Get g : list) {
            ret[i++] = exists(g);
        }
        return ret;
    }

    @Override
    public void batch(List<? extends Row> actions, Object[] results) throws IOException, InterruptedException {
        Object[] rows = batch(actions);
        System.arraycopy(rows, 0, results, 0, rows.length);
    }

    public Object[] batch(List<? extends Row> actions) throws IOException, InterruptedException {
        Object[] results = new Object[actions.size()]; // same size.
        for (int i = 0; i < actions.size(); i++) {
            Row r = actions.get(i);
            if (r instanceof Delete) {
                delete((Delete) r);
                results[i] = new Result();
            }
            if (r instanceof Put) {
                put((Put) r);
                results[i] = new Result();
            }
            if (r instanceof Get) {
                Result result = get((Get) r);
                results[i] = result;
            }
            if (r instanceof Increment) {
                Result result = increment((Increment) r);
                results[i] = result;
            }
            if (r instanceof Append) {
                Result result = append((Append) r);
                results[i] = result;
            }
        }
        return results;
    }

    @Deprecated
    @Override
    public <R> void batchCallback(final List<? extends Row> actions, final Object[] results, final Batch.Callback<R> callback) throws IOException, InterruptedException {
        throw new RuntimeException(this.getClass() + " does NOT implement this method.");
    }

    @Override
    public Result get(Get get) throws IOException {
        if (!data.containsKey(get.getRow()))
            return new Result();
        byte[] row = get.getRow();
        List<Cell> kvs = new ArrayList<>();
        Filter filter = get.getFilter();
        int maxResults = get.getMaxResultsPerColumnFamily();

        if (!get.hasFamilies()) {
            kvs = toKeyValue(row, data.get(row), get.getMaxVersions());
            if (filter != null) {
                kvs = filter(filter, kvs);
            }
            if (maxResults >= 0 && kvs.size() > maxResults) {
                kvs = kvs.subList(0, maxResults);
            }
        } else {
            for (byte[] family : get.getFamilyMap().keySet()) {
                if (data.get(row).get(family) == null)
                    continue;
                NavigableSet<byte[]> qualifiers = get.getFamilyMap().get(family);
                if (qualifiers == null || qualifiers.isEmpty())
                    qualifiers = data.get(row).get(family).navigableKeySet();
                List<Cell> familyKvs = new ArrayList<>();
                for (byte[] qualifier : qualifiers) {
                    if (qualifier == null)
                        qualifier = "".getBytes(StandardCharsets.UTF_8);
                    if (!data.get(row).containsKey(family) ||
                            !data.get(row).get(family).containsKey(qualifier) ||
                            data.get(row).get(family).get(qualifier).isEmpty())
                        continue;
                    Map.Entry<Long, byte[]> timestampAndValue = data.get(row).get(family).get(qualifier).lastEntry();
                    familyKvs.add(new KeyValue(row, family, qualifier, timestampAndValue.getKey(), timestampAndValue.getValue()));
                }
                if (filter != null) {
                    familyKvs = filter(filter, familyKvs);
                }
                if (maxResults >= 0 && familyKvs.size() > maxResults) {
                    familyKvs = familyKvs.subList(0, maxResults);
                }
                kvs.addAll(familyKvs);
            }
        }
        return Result.create(kvs);
    }

    @Override
    public Result[] get(List<Get> gets) throws IOException {
        List<Result> results = new ArrayList<>();
        for (Get g : gets) {
            results.add(get(g));
        }
        return results.toArray(new Result[results.size()]);
    }

    @Override
    public ResultScanner getScanner(Scan scan) throws IOException {
        final List<Result> ret = new ArrayList<>();
        byte[] st = scan.getStartRow();
        byte[] sp = scan.getStopRow();
        Filter filter = scan.getFilter();
        int maxResults = scan.getMaxResultsPerColumnFamily();

        Set<byte[]> dataKeySet = scan.isReversed() ? data.descendingKeySet() : data.keySet();
        for (byte[] row : dataKeySet) {
            // if row is equal to startRow emit it. When startRow (inclusive) and
            // stopRow (exclusive) is the same, it should not be excluded which would
            // happen w/o this control.
            if (st != null && st.length > 0 &&
                    Bytes.BYTES_COMPARATOR.compare(st, row) != 0) {
                if (scan.isReversed()) {
                    // if row is before startRow do not emit, pass to next row
                    //noinspection ConstantConditions
                    if (st != null && st.length > 0 &&
                            Bytes.BYTES_COMPARATOR.compare(st, row) <= 0)
                        continue;
                    // if row is equal to stopRow or after it do not emit, stop iteration
                    if (sp != null && sp.length > 0 &&
                            Bytes.BYTES_COMPARATOR.compare(sp, row) > 0)
                        break;
                } else {
                    // if row is before startRow do not emit, pass to next row
                    //noinspection ConstantConditions
                    if (st != null && st.length > 0 &&
                            Bytes.BYTES_COMPARATOR.compare(st, row) > 0)
                        continue;
                    // if row is equal to stopRow or after it do not emit, stop iteration
                    if (sp != null && sp.length > 0 &&
                            Bytes.BYTES_COMPARATOR.compare(sp, row) <= 0)
                        break;
                }
            }

            List<Cell> kvs;
            if (!scan.hasFamilies()) {
                kvs = toKeyValue(row, data.get(row), scan.getTimeRange().getMin(), scan.getTimeRange().getMax(), scan.getMaxVersions());
                if (filter != null) {
                    kvs = filter(filter, kvs);
                }
                if (maxResults >= 0 && kvs.size() > maxResults) {
                    kvs = kvs.subList(0, maxResults);
                }
            } else {
                kvs = new ArrayList<>();
                for (byte[] family : scan.getFamilyMap().keySet()) {
                    if (data.get(row).get(family) == null)
                        continue;
                    NavigableSet<byte[]> qualifiers = scan.getFamilyMap().get(family);
                    if (qualifiers == null || qualifiers.isEmpty())
                        qualifiers = data.get(row).get(family).navigableKeySet();
                    List<Cell> familyKvs = new ArrayList<>();
                    for (byte[] qualifier : qualifiers) {
                        if (data.get(row).get(family).get(qualifier) == null)
                            continue;
                        List<KeyValue> tsKvs = new ArrayList<>();
                        for (Long timestamp : data.get(row).get(family).get(qualifier).descendingKeySet()) {
                            if (timestamp < scan.getTimeRange().getMin())
                                continue;
                            if (timestamp > scan.getTimeRange().getMax())
                                continue;
                            byte[] value = data.get(row).get(family).get(qualifier).get(timestamp);
                            tsKvs.add(new KeyValue(row, family, qualifier, timestamp, value));
                            if (tsKvs.size() == scan.getMaxVersions()) {
                                break;
                            }
                        }
                        familyKvs.addAll(tsKvs);
                    }
                    if (filter != null) {
                        familyKvs = filter(filter, familyKvs);
                    }
                    if (maxResults >= 0 && familyKvs.size() > maxResults) {
                        familyKvs = familyKvs.subList(0, maxResults);
                    }
                    kvs.addAll(familyKvs);
                }
            }
            if (!kvs.isEmpty()) {
                ret.add(Result.create(kvs));
            }
            // Check for early out optimization
            if (filter != null && filter.filterAllRemaining()) {
                break;
            }
        }

        return new ResultScanner() {
            private final Iterator<Result> iterator = ret.iterator();

            @Override
            public Iterator<Result> iterator() {
                return iterator;
            }

            @Override
            public Result[] next(int nbRows) throws IOException {
                ArrayList<Result> resultSets = new ArrayList<>(nbRows);
                for (int i = 0; i < nbRows; i++) {
                    Result next = next();
                    if (next != null) {
                        resultSets.add(next);
                    } else {
                        break;
                    }
                }
                return resultSets.toArray(new Result[resultSets.size()]);
            }

            @Override
            public Result next() throws IOException {
                try {
                    return iterator().next();
                } catch (NoSuchElementException e) {
                    return null;
                }
            }

            @Override
            public void close() {
            }

            public ScanMetrics getScanMetrics() {
                return null;
            }

            public boolean renewLease() {
                return false;
            }
        };
    }

    @Override
    public ResultScanner getScanner(byte[] family) throws IOException {
        Scan scan = new Scan();
        scan.addFamily(family);
        return getScanner(scan);
    }

    @Override
    public ResultScanner getScanner(byte[] family, byte[] qualifier) throws IOException {
        Scan scan = new Scan();
        scan.addColumn(family, qualifier);
        return getScanner(scan);
    }

    public List<Put> getPutLog() {
        synchronized (putLog) {
            return ImmutableList.copyOf(putLog);
        }
    }

    public void addToPutLog(Put put) {
        synchronized(putLog) {
            putLog.add(put);
        }
    }

    public void clear() {
        synchronized (putLog) {
            putLog.clear();
        }
        data.clear();
    }

    @Override
    public void put(Put put) throws IOException {
        addToPutLog(put);

        byte[] row = put.getRow();
        NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> rowData = forceFind(data, row, new ConcurrentSkipListMap<>(Bytes.BYTES_COMPARATOR));
        for (byte[] family : put.getFamilyCellMap().keySet()) {
            if (!columnFamilies.contains(new String(family, StandardCharsets.UTF_8))) {
                throw new RuntimeException("Not Exists columnFamily : " + new String(family, StandardCharsets.UTF_8));
            }
            NavigableMap<byte[], NavigableMap<Long, byte[]>> familyData = forceFind(rowData, family, new ConcurrentSkipListMap<>(Bytes.BYTES_COMPARATOR));
            for (Cell kv : put.getFamilyCellMap().get(family)) {
                long ts = put.getTimeStamp();
                if (ts == HConstants.LATEST_TIMESTAMP) ts = System.currentTimeMillis();
                CellUtil.updateLatestStamp(kv, ts);
                byte[] qualifier = CellUtil.cloneQualifier(kv);
                NavigableMap<Long, byte[]> qualifierData = forceFind(familyData, qualifier, new ConcurrentSkipListMap<>());
                qualifierData.put(kv.getTimestamp(), CellUtil.cloneValue(kv));
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
    private <K, V> V forceFind(NavigableMap<K, V> map, K key, V newObject) {
        V data = map.putIfAbsent(key, newObject);
        if (data == null) {
            data = newObject;
        }
        return data;
    }

    @Override
    public void put(List<Put> puts) throws IOException {
        for (Put put : puts) {
            put(put);
        }
    }

    @Override
    public boolean checkAndPut(byte[] row, byte[] family, byte[] qualifier, byte[] value, Put put) throws IOException {
        return checkAndPut(row, family, qualifier, CompareFilter.CompareOp.EQUAL, value, put);
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
        if (check(row, family, qualifier, compareOp, value)) {
            put(put);
            return true;
        }
        return false;
    }

    @Override
    public void delete(Delete delete) throws IOException {
        byte[] row = delete.getRow();
        if (data.containsKey(row)) {
            data.remove(row);
        } else {
            throw new IOException("Nothing to delete");
        }
    }

    @Override
    public void delete(List<Delete> deletes) throws IOException {
        for (Delete delete : deletes) {
            delete(delete);
        }
    }

    @Override
    public boolean checkAndDelete(byte[] row, byte[] family, byte[] qualifier, byte[] value, Delete delete) throws IOException {
        return checkAndDelete(row, family, qualifier, CompareFilter.CompareOp.EQUAL, value, delete);
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
        if (check(row, family, qualifier, compareOp, value)) {
            delete(delete);
            return true;
        }
        return false;
    }

    @Override
    public void mutateRow(RowMutations rm) throws IOException {
        // currently only support Put and Delete
        long maxTs = System.currentTimeMillis();
        for (Mutation mutation : rm.getMutations()) {
            if (mutation instanceof Put) {
                put((Put) mutation);
            } else if (mutation instanceof Delete) {
                delete((Delete) mutation);
            }
            long ts = mutation.getTimeStamp();
            if (ts != HConstants.LATEST_TIMESTAMP && ts > maxTs) maxTs = ts;
        }
        long now = System.currentTimeMillis();
        if (now <= maxTs) {
            // we have intentionally set the ts in the future, so wait
            try {
                Thread.sleep(maxTs - now + 1);
            } catch (InterruptedException ignored) { }
        }
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
    public long incrementColumnValue(byte[] row, byte[] family, byte[] qualifier, long amount) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long incrementColumnValue(byte[] row, byte[] family, byte[] qualifier, long amount, Durability durability) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Follows the logical flow through the filter methods for a single row.
     *
     * @param filter HBase filter.
     * @param kvs    List of a row's KeyValues
     * @return List of KeyValues that were not filtered.
     */
    private List<Cell> filter(Filter filter, List<Cell> kvs) throws IOException {
        filter.reset();

        List<Cell> tmp = new ArrayList<>(kvs.size());
        tmp.addAll(kvs);

      /*
       * Note. Filter flow for a single row. Adapted from
       * "HBase: The Definitive Guide" (p. 163) by Lars George, 2011.
       * See Figure 4-2 on p. 163.
       */
        boolean filteredOnRowKey = false;
        List<Cell> nkvs = new ArrayList<>(tmp.size());
        for (Cell kv : tmp) {
            if (filter.filterRowKey(kv.getRowArray(), kv.getRowOffset(), kv.getRowLength())) {
                filteredOnRowKey = true;
                break;
            }
            Filter.ReturnCode filterResult = filter.filterKeyValue(kv);
            if (filterResult == Filter.ReturnCode.INCLUDE || filterResult == Filter.ReturnCode.INCLUDE_AND_NEXT_COL) {
                nkvs.add(filter.transformCell(kv));
            } else if (filterResult == Filter.ReturnCode.NEXT_ROW) {
                break;
            } else if (filterResult == Filter.ReturnCode.NEXT_COL || filterResult == Filter.ReturnCode.SKIP) {
                //noinspection UnnecessaryContinue
                continue;
            }
          /*
           * Ignoring next key hint which is a optimization to reduce file
           * system IO
           */
        }
        if (filter.hasFilterRow() && !filteredOnRowKey) {
            filter.filterRowCells(nkvs);
        }
        if (filter.filterRow() || filteredOnRowKey) {
            nkvs.clear();
        }
        tmp = nkvs;
        return tmp;
    }

    private boolean check(byte[] row, byte[] family, byte[] qualifier, CompareFilter.CompareOp compareOp, byte[] value) {
        if (value == null)
            return !data.containsKey(row) ||
                    !data.get(row).containsKey(family) ||
                    !data.get(row).get(family).containsKey(qualifier);
        else if (data.containsKey(row) &&
                data.get(row).containsKey(family) &&
                data.get(row).get(family).containsKey(qualifier) &&
                !data.get(row).get(family).get(qualifier).isEmpty()) {

            byte[] oldValue = data.get(row).get(family).get(qualifier).lastEntry().getValue();
            int compareResult = Bytes.compareTo(value, oldValue);
            switch (compareOp) {
                case LESS:
                    return compareResult < 0;
                case LESS_OR_EQUAL:
                    return compareResult <= 0;
                case EQUAL:
                    return compareResult == 0;
                case NOT_EQUAL:
                    return compareResult != 0;
                case GREATER_OR_EQUAL:
                    return compareResult >= 0;
                case GREATER:
                    return compareResult > 0;
                default:
                    throw new RuntimeException("Unknown Compare op " + compareOp.name());
            }
        } else {
            return false;
        }
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public CoprocessorRpcChannel coprocessorService(byte[] row) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Service, R> Map<byte[], R> coprocessorService(final Class<T> service, byte[] startKey, byte[] endKey, final Batch.Call<T, R> callable) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Service, R> void coprocessorService(final Class<T> service, byte[] startKey, byte[] endKey, final Batch.Call<T, R> callable, final Batch.Callback<R> callback) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOperationTimeout(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getOperationTimeout() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRpcTimeout(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getReadRpcTimeout() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setReadRpcTimeout(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getWriteRpcTimeout() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setWriteRpcTimeout(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getRpcTimeout() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R extends Message> Map<byte[], R> batchCoprocessorService(Descriptors.MethodDescriptor methodDescriptor, Message request, byte[] startKey, byte[] endKey, R responsePrototype) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R extends Message> void batchCoprocessorService(Descriptors.MethodDescriptor methodDescriptor, Message request, byte[] startKey, byte[] endKey, R responsePrototype, Batch.Callback<R> callback) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean checkAndMutate(byte[] row, byte[] family, byte[] qualifier, CompareFilter.CompareOp compareOp, byte[] value, RowMutations rm) throws IOException {
        if (check(row, family, qualifier, compareOp, value)) {
            mutateRow(rm);
            return true;
        }
        return false;
    }
}
