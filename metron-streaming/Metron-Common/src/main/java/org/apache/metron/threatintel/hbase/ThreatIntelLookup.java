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
package org.apache.metron.threatintel.hbase;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.hbase.converters.HbaseConverter;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelConverter;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelKey;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelValue;
import org.apache.metron.reference.lookup.Lookup;
import org.apache.metron.reference.lookup.LookupKV;
import org.apache.metron.reference.lookup.accesstracker.AccessTracker;

import java.io.IOException;


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
