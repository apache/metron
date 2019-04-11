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
package org.apache.metron.enrichment.lookup;

import com.google.common.collect.Iterables;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.enrichment.converter.HbaseConverter;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.accesstracker.AccessTracker;
import org.apache.metron.enrichment.lookup.handler.KeyWithContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class EnrichmentLookup extends Lookup<EnrichmentLookup.HBaseContext, EnrichmentKey, LookupKV<EnrichmentKey,EnrichmentValue>> implements AutoCloseable {

  public static class HBaseContext {
    private HTableInterface table;
    private String columnFamily;
    public HBaseContext(HTableInterface table, String columnFamily) {
      this.table = table;
      this.columnFamily = columnFamily;
    }

    public HTableInterface getTable() { return table; }
    public String getColumnFamily() { return columnFamily; }
  }

  public static class Handler implements org.apache.metron.enrichment.lookup.handler.Handler<HBaseContext,EnrichmentKey,LookupKV<EnrichmentKey,EnrichmentValue>> {
    String columnFamily;
    HbaseConverter<EnrichmentKey, EnrichmentValue> converter = new EnrichmentConverter();
    public Handler(String columnFamily) {
      this.columnFamily = columnFamily;
    }

    private String getColumnFamily(HBaseContext context) {
      return context.getColumnFamily() == null?columnFamily:context.getColumnFamily();
    }

    @Override
    public boolean exists(EnrichmentKey key, HBaseContext context, boolean logAccess) throws IOException {
      return context.getTable().exists(converter.toGet(getColumnFamily(context), key));
    }

    @Override
    public LookupKV<EnrichmentKey, EnrichmentValue> get(EnrichmentKey key, HBaseContext context, boolean logAccess) throws IOException {
      return converter.fromResult(context.getTable().get(converter.toGet(getColumnFamily(context), key)), getColumnFamily(context));
    }

    private List<Get> keysToGets(Iterable<KeyWithContext<EnrichmentKey, HBaseContext>> keys) {
      List<Get> ret = new ArrayList<>();
      for(KeyWithContext<EnrichmentKey, HBaseContext> key : keys) {
        ret.add(converter.toGet(getColumnFamily(key.getContext()), key.getKey()));
      }
      return ret;
    }

    @Override
    public Iterable<Boolean> exists(Iterable<KeyWithContext<EnrichmentKey, HBaseContext>> key, boolean logAccess) throws IOException {
      List<Boolean> ret = new ArrayList<>();
      if(Iterables.isEmpty(key)) {
        return Collections.emptyList();
      }
      HTableInterface table = Iterables.getFirst(key, null).getContext().getTable();
      for(boolean b : table.existsAll(keysToGets(key))) {
        ret.add(b);
      }
      return ret;
    }

    @Override
    public Iterable<LookupKV<EnrichmentKey, EnrichmentValue>> get( Iterable<KeyWithContext<EnrichmentKey, HBaseContext>> keys
                                                                 , boolean logAccess
                                                                 ) throws IOException
    {
      if(Iterables.isEmpty(keys)) {
        return Collections.emptyList();
      }
      HTableInterface table = Iterables.getFirst(keys, null).getContext().getTable();
      List<LookupKV<EnrichmentKey, EnrichmentValue>> ret = new ArrayList<>();
      Iterator<KeyWithContext<EnrichmentKey, HBaseContext>> keyWithContextIterator = keys.iterator();
      for(Result result : table.get(keysToGets(keys))) {
        HBaseContext context = keyWithContextIterator.next().getContext();
        ret.add(converter.fromResult(result, getColumnFamily(context)));
      }
      return ret;
    }


    @Override
    public void close() throws Exception {

    }
  }
  private HTableInterface table;
  public EnrichmentLookup(HTableInterface table, String columnFamily, AccessTracker tracker) {
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
