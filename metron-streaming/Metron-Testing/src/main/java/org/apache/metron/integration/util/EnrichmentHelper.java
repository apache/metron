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
package org.apache.metron.integration.util;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.metron.hbase.converters.enrichment.EnrichmentConverter;
import org.apache.metron.hbase.converters.enrichment.EnrichmentKey;
import org.apache.metron.hbase.converters.enrichment.EnrichmentValue;
import org.apache.metron.reference.lookup.LookupKV;

import java.io.IOException;

public enum EnrichmentHelper {
    INSTANCE;
    EnrichmentConverter converter = new EnrichmentConverter();

    public void load(HTableInterface table, String cf, Iterable<LookupKV<EnrichmentKey, EnrichmentValue>> results) throws IOException {
        for(LookupKV<EnrichmentKey, EnrichmentValue> result : results) {
            Put put = converter.toPut(cf, result.getKey(), result.getValue());
            table.put(put);
        }
    }
}
