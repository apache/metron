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
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

/**
 * Responsible for creating an {@link EnrichmentLookup}.
 */
public interface EnrichmentLookupFactory extends Serializable {

  EnrichmentLookup create(HBaseConnectionFactory connectionFactory,
                          String tableName,
                          String columnFamily,
                          AccessTracker accessTracker) throws IOException;

  /**
   * Creates an {@link EnrichmentLookupFactory} based on a fully-qualified class name.
   *
   * @param className The fully-qualified class name to instantiate.
   * @return A {@link EnrichmentLookupFactory}.
   */
  static EnrichmentLookupFactory byName(String className) {
      try {
        Class<? extends EnrichmentLookupFactory> clazz = (Class<? extends EnrichmentLookupFactory>) Class.forName(className);
        return clazz.getConstructor().newInstance();

      } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException | InvocationTargetException e) {
        throw new IllegalStateException("Unable to instantiate EnrichmentLookupCreator.", e);
      }
  }
}
