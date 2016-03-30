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
package org.apache.metron.hbase.lookup;

import com.google.common.collect.ImmutableList;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.hbase.converters.HbaseConverter;
import org.apache.metron.hbase.converters.enrichment.EnrichmentConverter;
import org.apache.metron.hbase.converters.enrichment.EnrichmentKey;
import org.apache.metron.hbase.converters.enrichment.EnrichmentValue;
import org.apache.metron.reference.lookup.Lookup;
import org.apache.metron.reference.lookup.LookupKV;
import org.apache.metron.reference.lookup.accesstracker.AccessTracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class EnrichmentLookup extends Lookup<HTableInterface, EnrichmentKey, LookupKV<EnrichmentKey,EnrichmentValue>> implements AutoCloseable {

  public static class Handler implements org.apache.metron.reference.lookup.handler.Handler<HTableInterface,EnrichmentKey,LookupKV<EnrichmentKey,EnrichmentValue>> {
    String columnFamily;
    HbaseConverter<EnrichmentKey, EnrichmentValue> converter = new EnrichmentConverter();
    public Handler(String columnFamily) {
      this.columnFamily = columnFamily;
    }
    @Override
    public boolean exists(EnrichmentKey key, HTableInterface table, boolean logAccess) throws IOException {
      return table.exists(converter.toGet(columnFamily, key));
    }

    @Override
    public LookupKV<EnrichmentKey, EnrichmentValue> get(EnrichmentKey key, HTableInterface table, boolean logAccess) throws IOException {
      return converter.fromResult(table.get(converter.toGet(columnFamily, key)), columnFamily);
    }

    private List<Get> keysToGets(Iterable<EnrichmentKey> keys) {
      List<Get> ret = new ArrayList<>();
      for(EnrichmentKey key : keys) {
        ret.add(converter.toGet(columnFamily, key));
      }
      return ret;
    }

    @Override
    public Iterable<Boolean> exists(Iterable<EnrichmentKey> key, HTableInterface table, boolean logAccess) throws IOException {
      List<Boolean> ret = new ArrayList<>();
      for(boolean b : table.existsAll(keysToGets(key))) {
        ret.add(b);
      }
      return ret;
    }

    @Override
    public Iterable<LookupKV<EnrichmentKey, EnrichmentValue>> get( Iterable<EnrichmentKey> keys
                                                                 , HTableInterface table
                                                                 , boolean logAccess
                                                                 ) throws IOException
    {
      List<LookupKV<EnrichmentKey, EnrichmentValue>> ret = new ArrayList<>();
      for(Result result : table.get(keysToGets(keys))) {
        ret.add(converter.fromResult(result, columnFamily));
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
