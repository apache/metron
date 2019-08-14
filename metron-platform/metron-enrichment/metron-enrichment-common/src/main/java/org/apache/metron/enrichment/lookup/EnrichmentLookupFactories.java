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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Table;
import org.apache.metron.enrichment.lookup.accesstracker.AccessTracker;
import org.apache.metron.hbase.client.HBaseConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;

/**
 * Enumerates the available {@link EnrichmentLookupFactory} implementations.
 */
public enum EnrichmentLookupFactories implements EnrichmentLookupFactory {

  HBASE((connFactory, conf, tableName, columnFamily, accessTracker) -> {
    Connection connection = connFactory.createConnection(conf);
    Table table = connection.getTable(TableName.valueOf(tableName));
    return new EnrichmentLookup(table, columnFamily, accessTracker);
  });

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private EnrichmentLookupFactory factory;

  EnrichmentLookupFactories(EnrichmentLookupFactory factory) {
    this.factory = factory;
  }

  @Override
  public EnrichmentLookup create(HBaseConnectionFactory connectionFactory,
                                 Configuration configuration,
                                 String tableName,
                                 String columnFamily,
                                 AccessTracker accessTracker) throws IOException {
    return factory.create(connectionFactory, configuration, tableName, columnFamily, accessTracker);
  }

  /**
   * Creates an {@link EnrichmentLookupFactory}.
   *
   * @param name Either an enum value or a fully-qualified class name.
   * @return A {@link EnrichmentLookupFactory}.
   */
  public static EnrichmentLookupFactory byName(String name) {
    // is this an enum?
    try {
      return EnrichmentLookupFactories.valueOf(name);
    } catch (IllegalArgumentException e) {
      LOG.debug("Cannot find EnrichmentLookupFactory by enum name, assuming this is a class name; name={}", name);
    }

    // is this a class name? a class name may be used during testing
    try {
      Class<? extends EnrichmentLookupFactory> clazz = (Class<? extends EnrichmentLookupFactory>) Class.forName(name);
      return clazz.getConstructor().newInstance();
    } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException | InvocationTargetException e) {
      throw new IllegalStateException("Unable to instantiate EnrichmentLookupFactory.", e);
    }
  }
}
