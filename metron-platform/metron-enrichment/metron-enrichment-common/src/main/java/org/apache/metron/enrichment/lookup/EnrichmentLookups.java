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

import org.apache.metron.enrichment.lookup.accesstracker.AccessTracker;
import org.apache.metron.hbase.client.HBaseConnectionFactory;

import java.io.IOException;

public enum EnrichmentLookups implements EnrichmentLookupFactory {

  HBASE((connFactory, table, columnFamily, accessTracker) -> {
    return new HBaseEnrichmentLookup(connFactory, table, columnFamily);
  }),

  TRACKED((connFactory, table, columnFamily, accessTracker) -> {
    EnrichmentLookup lookup = new HBaseEnrichmentLookup(connFactory, table, columnFamily);
    return new TrackedEnrichmentLookup(lookup, accessTracker);
  }),

  MEMORY((connFactory, table, columnFamily, accessTracker) -> {
    return new InMemoryEnrichmentLookup();
  });

  private EnrichmentLookupFactory factory;

  EnrichmentLookups(EnrichmentLookupFactory factory) {
    this.factory = factory;
  }

  public EnrichmentLookup create(HBaseConnectionFactory connFactory,
                                 String tableName,
                                 String columnFamily,
                                 AccessTracker accessTracker) throws IOException {
    return factory.create(connFactory, tableName, columnFamily, accessTracker);
  }
}
