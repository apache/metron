/*
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

import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performs a lookup for enrichment values using a collection stored in memory.
 */
public class InMemoryEnrichmentLookup implements EnrichmentLookup {
  private Map<EnrichmentKey, EnrichmentValue> enrichments;

  public InMemoryEnrichmentLookup() {
    this.enrichments = new HashMap<>();
  }

  /**
   * Add an enrichment.
   * @param key The enrichment key.
   * @param value The enrichment value.
   * @return
   */
  public InMemoryEnrichmentLookup withEnrichment(EnrichmentKey key, EnrichmentValue value) {
    this.enrichments.put(key, value);
    return this;
  }

  @Override
  public boolean isInitialized() {
    return true;
  }

  @Override
  public boolean exists(EnrichmentKey key) throws IOException {
    return enrichments.containsKey(key);
  }

  @Override
  public Iterable<Boolean> exists(Iterable<EnrichmentKey> keys) throws IOException {
    List<Boolean> results = new ArrayList<>();
    for(EnrichmentKey key: keys) {
      results.add(enrichments.containsKey(key));
    }
    return results;
  }

  @Override
  public EnrichmentResult get(EnrichmentKey key) throws IOException {
    EnrichmentValue enrichmentValue = enrichments.get(key);
    return new EnrichmentResult(key, enrichmentValue);
  }

  @Override
  public Iterable<EnrichmentResult> get(Iterable<EnrichmentKey> keys) throws IOException {
    List<EnrichmentResult> results = new ArrayList<>();
    for(EnrichmentKey key: keys) {
      if(enrichments.containsKey(key)) {
        results.add(get(key));
      }
    }

    return results;
  }

  @Override
  public void close() throws IOException {
    // nothing to do
  }

}
