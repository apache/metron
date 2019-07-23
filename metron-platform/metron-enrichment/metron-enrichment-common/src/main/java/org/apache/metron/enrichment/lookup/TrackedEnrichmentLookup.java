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
import org.apache.metron.enrichment.lookup.accesstracker.AccessTracker;

import java.io.IOException;

/**
 * Ensures all enrichment lookups are tracked using an {@link AccessTracker}.
 */
public class TrackedEnrichmentLookup implements EnrichmentLookup {

  private EnrichmentLookup lookup;
  private AccessTracker accessTracker;

  public TrackedEnrichmentLookup(EnrichmentLookup lookup, AccessTracker accessTracker) {
    this.lookup = lookup;
    this.accessTracker = accessTracker;
  }

  @Override
  public boolean isInitialized() {
    return lookup.isInitialized();
  }

  @Override
  public boolean exists(EnrichmentKey key) throws IOException {
    accessTracker.logAccess(key);
    return lookup.exists(key);
  }

  @Override
  public Iterable<Boolean> exists(Iterable<EnrichmentKey> keys) throws IOException {
    for(EnrichmentKey key: keys) {
      accessTracker.logAccess(key);
    }
    return lookup.exists(keys);
  }

  @Override
  public EnrichmentResult get(EnrichmentKey key) throws IOException {
    accessTracker.logAccess(key);
    return lookup.get(key);
  }

  @Override
  public Iterable<EnrichmentResult> get(Iterable<EnrichmentKey> keys) throws IOException {
    for(EnrichmentKey key: keys) {
      accessTracker.logAccess(key);
    }
    return lookup.get(keys);
  }

  @Override
  public void close() throws IOException {
    accessTracker.cleanup();
    lookup.close();
  }

  public AccessTracker getAccessTracker() {
    return accessTracker;
  }
}
