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

package org.apache.metron.enrichment.converter;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.enrichment.lookup.LookupKey;
import org.apache.metron.enrichment.lookup.LookupValue;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;


public abstract class AbstractConverter<KEY_T extends LookupKey, VALUE_T extends LookupValue> implements HbaseConverter<KEY_T,VALUE_T> {
  public static Function<Cell, Map.Entry<byte[], byte[]>> CELL_TO_ENTRY  = new Function<Cell, Map.Entry<byte[], byte[]>>() {

    @Nullable
    @Override
    public Map.Entry<byte[], byte[]> apply(@Nullable Cell cell) {
      return new AbstractMap.SimpleEntry<>(cell.getQualifier(), cell.getValue());
    }
  };
  @Override
  public Put toPut(String columnFamily, KEY_T key, VALUE_T values) throws IOException {
    Put put = new Put(key.toBytes());
    byte[] cf = Bytes.toBytes(columnFamily);
    for(Map.Entry<byte[], byte[]> kv : values.toColumns()) {
      put.add(cf, kv.getKey(), kv.getValue());
    }
    return put;
  }

  public LookupKV<KEY_T, VALUE_T> fromPut(Put put, String columnFamily, KEY_T key, VALUE_T value) throws IOException {
    key.fromBytes(put.getRow());
    byte[] cf = Bytes.toBytes(columnFamily);
    value.fromColumns(Iterables.transform(put.getFamilyCellMap().get(cf), CELL_TO_ENTRY));
    return new LookupKV<>(key, value);
  }

  @Override
  public Result toResult(String columnFamily, KEY_T key, VALUE_T values) throws IOException {
    Put put = toPut(columnFamily, key, values);
    return Result.create(put.getFamilyCellMap().get(Bytes.toBytes(columnFamily)));
  }

  public LookupKV<KEY_T, VALUE_T> fromResult(Result result, String columnFamily, KEY_T key, VALUE_T value) throws IOException {
    if(result == null || result.getRow() == null) {
      return null;
    }
    key.fromBytes(result.getRow());
    byte[] cf = Bytes.toBytes(columnFamily);
    NavigableMap<byte[], byte[]> cols = result.getFamilyMap(cf);
    value.fromColumns(cols.entrySet());
    return new LookupKV<>(key, value);
  }
  @Override
  public Get toGet(String columnFamily, KEY_T key) {
    Get ret = new Get(key.toBytes());
    ret.addFamily(Bytes.toBytes(columnFamily));
    return ret;
  }

  public static Iterable<Map.Entry<byte[], byte[]>> toEntries(byte[]... kvs) {
    if(kvs.length % 2 != 0)  {
      throw new IllegalStateException("Must be an even size");
    }
    List<Map.Entry<byte[], byte[]>> ret = new ArrayList<>(kvs.length/2);
    for(int i = 0;i < kvs.length;i += 2) {
      ret.add(new AbstractMap.SimpleImmutableEntry<>(kvs[i], kvs[i+1])) ;
    }
    return ret;
  }
}
