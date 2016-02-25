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

package org.apache.metron.hbase.converters.threatintel;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.hbase.converters.AbstractConverter;
import org.apache.metron.reference.lookup.LookupKV;

import java.io.IOException;

public class ThreatIntelConverter extends AbstractConverter<ThreatIntelKey, ThreatIntelValue> {

    @Override
    public LookupKV<ThreatIntelKey, ThreatIntelValue> fromPut(Put put, String columnFamily) throws IOException {
        return fromPut(put, columnFamily, new ThreatIntelKey(), new ThreatIntelValue());
    }

    @Override
    public LookupKV<ThreatIntelKey, ThreatIntelValue> fromResult(Result result, String columnFamily) throws IOException {
        return fromResult(result, columnFamily, new ThreatIntelKey(), new ThreatIntelValue());
    }
}
