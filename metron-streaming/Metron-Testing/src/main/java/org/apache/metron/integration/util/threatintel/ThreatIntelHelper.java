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
package org.apache.metron.integration.util.threatintel;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelConverter;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelKey;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelValue;
import org.apache.metron.reference.lookup.LookupKV;

import java.io.IOException;

public enum ThreatIntelHelper {
    INSTANCE;
    ThreatIntelConverter converter = new ThreatIntelConverter();

    public void load(HTableInterface table, String cf, Iterable<LookupKV<ThreatIntelKey, ThreatIntelValue>> results) throws IOException {
        for(LookupKV<ThreatIntelKey, ThreatIntelValue> result : results) {
            Put put = converter.toPut(cf, result.getKey(), result.getValue());
            table.put(put);
        }
    }
}
