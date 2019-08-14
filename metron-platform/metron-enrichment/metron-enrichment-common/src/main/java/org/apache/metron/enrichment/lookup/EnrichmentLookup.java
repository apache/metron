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

import java.io.Closeable;
import java.io.IOException;

/**
 * Performs a lookup to find the value of an enrichment.
 */
public interface EnrichmentLookup extends Closeable {

  /**
   * @return True, if initialization has been completed.  Otherwise, false.
   */
  boolean isInitialized();

  /**
   * Does an enrichment exist for the given key?
   *
   * @param key The enrichment key.
   * @return True, if an enrichment exists. Otherwise, false.
   * @throws IOException
   */
  boolean exists(EnrichmentKey key) throws IOException;

  /**
   * Does an enrichment exist for the given keys?
   *
   * @param keys The enrichment keys.
   * @return True, if an enrichment exists. Otherwise, false.
   * @throws IOException
   */
  Iterable<Boolean> exists(Iterable<EnrichmentKey> keys) throws IOException;

  /**
   * Retrieve the value of an enrichment.
   *
   * @param key The enrichment key.
   * @return The value of the enrichment.
   * @throws IOException
   */
  LookupKV<EnrichmentKey, EnrichmentValue> get(EnrichmentKey key) throws IOException;

  /**
   * Retrieves the value of multiple enrichments.
   *
   * @param keys The enrichment keys.
   * @return The value of the enrichments.
   * @throws IOException
   */
  Iterable<LookupKV<EnrichmentKey, EnrichmentValue>> get(Iterable<EnrichmentKey> keys) throws IOException;
}
