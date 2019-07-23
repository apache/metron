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
package org.apache.metron.enrichment.stellar;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.lookup.EnrichmentLookup;
import org.apache.metron.enrichment.lookup.EnrichmentLookupFactory;
import org.apache.metron.enrichment.lookup.EnrichmentLookups;
import org.apache.metron.enrichment.lookup.EnrichmentResult;
import org.apache.metron.enrichment.lookup.HBaseEnrichmentLookup;
import org.apache.metron.enrichment.lookup.accesstracker.AccessTracker;
import org.apache.metron.enrichment.lookup.accesstracker.AccessTrackers;
import org.apache.metron.hbase.client.HBaseConnectionFactory;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class SimpleHBaseEnrichmentFunctions {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String ACCESS_TRACKER_TYPE_CONF = "accessTracker";
  public static final String CONNECTION_FACTORY_IMPL_CONF = "connectionFactoryImpl";
  private static AccessTracker tracker;
  private static HBaseConnectionFactory connectionFactory;

  /**
   * Serves as a key for the cache of {@link EnrichmentLookup} objects used
   * to lookup the enrichment values.
   */
  private static class Table {
    String name;
    String columnFamily;

    public Table(String name, String columnFamily) {
      this.name = name;
      this.columnFamily = columnFamily;
    }

    @Override
    public String toString() {
      return "Table{" +
              "name='" + name + '\'' +
              ", columnFamily='" + columnFamily + '\'' +
              '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Table table = (Table) o;

      if (name != null ? !name.equals(table.name) : table.name != null) return false;
      return columnFamily != null ? columnFamily.equals(table.columnFamily) : table.columnFamily == null;

    }

    @Override
    public int hashCode() {
      int result = name != null ? name.hashCode() : 0;
      result = 31 * result + (columnFamily != null ? columnFamily.hashCode() : 0);
      return result;
    }
  }

  private static Map<String, Object> getConfig(Context context) {
    return (Map<String, Object>) context.getCapability(Context.Capabilities.GLOBAL_CONFIG).orElse(new HashMap<>());
  }

  private static synchronized void initializeTracker(Map<String, Object> config, HBaseConnectionFactory connectionFactory) throws IOException {
    if(tracker == null) {
      String accessTrackerType = (String) config.getOrDefault(ACCESS_TRACKER_TYPE_CONF, AccessTrackers.NOOP.toString());
      LOG.debug("Initializing tracker; type={}", accessTrackerType);
      AccessTrackers trackers = AccessTrackers.valueOf(accessTrackerType);
      tracker = trackers.create(config, connectionFactory);
    }
  }

  private static synchronized void initializeConnectionFactory(Map<String, Object> config) {
    if(connectionFactory == null) {
      String connectionFactoryImpl = (String) config.getOrDefault(CONNECTION_FACTORY_IMPL_CONF, HBaseConnectionFactory.class.getName());
      connectionFactory = HBaseConnectionFactory.byName(connectionFactoryImpl);
    }
  }

  private static Cache<Table, EnrichmentLookup> createCache() {
    return CacheBuilder
            .newBuilder()
            .removalListener(new CloseEnrichmentLookup())
            .build();
  }

  /**
   * Closes the {@link HBaseEnrichmentLookup} after it has been removed from the cache.  This
   * ensures that the underlying resources are cleaned up.
   */
  private static class CloseEnrichmentLookup implements RemovalListener<Table, EnrichmentLookup> {
    @Override
    public void onRemoval(RemovalNotification<Table, EnrichmentLookup> notification) {
      try {
        notification.getValue().close();
      } catch(Throwable e) {
        LOG.error("Failed to close EnrichmentLookup; cause={}", notification.getCause(), e);
      }
    }
  }

  @Stellar(name="EXISTS"
          ,namespace="ENRICHMENT"
          ,description="Interrogates the HBase table holding the simple hbase enrichment data and returns whether the" +
          " enrichment type and indicator are in the table."
          ,params = {
                      "enrichment_type - The enrichment type"
                     ,"indicator - The string indicator to look up"
                     ,"nosql_table - The NoSQL Table to use"
                     ,"column_family - The Column Family to use"
                    }
          ,returns = "True if the enrichment indicator exists and false otherwise"
          )
  public static class EnrichmentExists implements StellarFunction {

    boolean initialized = false;
    private static Cache<Table, EnrichmentLookup> lookupCache = createCache();
    private EnrichmentLookupFactory creator;

    public EnrichmentExists() {
      this.creator = EnrichmentLookups.HBASE;
    }

    public EnrichmentExists withEnrichmentLookupCreator(EnrichmentLookupFactory creator) {
      this.creator = creator;
      return this;
    }

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if (!initialized) {
        LOG.debug("ENRICHMENT_EXISTS not initialized");
        return false;
      }
      if (args.size() != 4) {
        throw new IllegalStateException("All parameters are mandatory, submit 'enrichment type', 'indicator', 'nosql_table' and 'column_family'");
      }
      int i = 0;
      String enrichmentType = (String) args.get(i++);
      String indicator = (String) args.get(i++);
      String table = (String) args.get(i++);
      String columnFamily = (String) args.get(i++);

      if (enrichmentType == null || indicator == null || table == null || columnFamily == null) {
        LOG.error("ENRICHMENT_EXISTS; got null for required argument");
        return false;
      }

      EnrichmentLookup lookup = fromCache(table, columnFamily);
      try {
        EnrichmentKey enrichmentKey = new EnrichmentKey(enrichmentType, indicator);
        return lookup.exists(enrichmentKey);

      } catch (IOException e) {
        LOG.error("Unable to call exists: {}", e.getMessage(), e);
        return false;
      }
    }

    private EnrichmentLookup fromCache(String table, String columnFamily) {
      EnrichmentLookup lookup = null;
      try {
        Table cacheKey = new Table(table, columnFamily);
        lookup = lookupCache.get(cacheKey,
                () -> creator.create(connectionFactory, table, columnFamily, tracker));

      } catch (ExecutionException e) {
        LOG.error("Unable to initialize an enrichment lookup: {}", e.getMessage(), e);
      }
      return lookup;
    }

    @Override
    public void initialize(Context context) {
      try {
        Map<String, Object> config = getConfig(context);
        initializeConnectionFactory(config);
        initializeTracker(config, connectionFactory);

      } catch (IOException e) {
        LOG.error("Unable to initialize ENRICHMENT_EXISTS: {}", e.getMessage(), e);

      } finally{
        initialized = true;
      }
    }

    @Override
    public boolean isInitialized() {
      return initialized;
    }

    @Override
    public void close() {
      lookupCache.invalidateAll();
      lookupCache.cleanUp();
    }
  }

  @Stellar(name="GET"
          ,namespace="ENRICHMENT"
          ,description="Interrogates the HBase table holding the simple hbase enrichment data and retrieves the " +
          "tabular value associated with the enrichment type and indicator."
          ,params = {
                      "enrichment_type - The enrichment type"
                     ,"indicator - The string indicator to look up"
                     ,"nosql_table - The NoSQL Table to use"
                     ,"column_family - The Column Family to use"
                    }
          ,returns = "A Map associated with the indicator and enrichment type.  Empty otherwise."
          )
  public static class EnrichmentGet implements StellarFunction {
    boolean initialized = false;
    private static Cache<Table, EnrichmentLookup> lookupCache = createCache();
    private EnrichmentLookupFactory creator;

    public EnrichmentGet() {
      this.creator = EnrichmentLookups.HBASE;
    }

    public EnrichmentGet withEnrichmentLookupCreator(EnrichmentLookupFactory creator) {
      this.creator = creator;
      return this;
    }

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(!initialized) {
        LOG.debug("ENRICHMENT_GET not initialized");
        return false;
      }
      if(args.size() != 4) {
        throw new IllegalStateException("All parameters are mandatory, submit 'enrichment type', 'indicator', 'nosql_table' and 'column_family'");
      }
      int i = 0;
      String enrichmentType = (String) args.get(i++);
      String indicator = (String) args.get(i++);
      String table = (String) args.get(i++);
      String columnFamily = (String) args.get(i++);
      if(enrichmentType == null || indicator == null) {
        LOG.error("ENRICHMENT_EXISTS; got null for required argument");
        return new HashMap<String, Object>();
      }

      EnrichmentLookup lookup = fromCache(table, columnFamily);
      try {
        EnrichmentResult result = lookup.get(new EnrichmentKey(enrichmentType, indicator));
        if(result != null && result.getValue() != null && result.getValue().getMetadata() != null) {
          return result.getValue().getMetadata();
        } else {
          return new HashMap<>();
        }
      } catch (IOException e) {
        LOG.error("Unable to call exists: {}", e.getMessage(), e);
        return new HashMap<String, Object>();
      }
    }

    private EnrichmentLookup fromCache(String table, String columnFamily) {
      EnrichmentLookup lookup = null;
      try {
        Table cacheKey = new Table(table, columnFamily);
        lookup = lookupCache.get(cacheKey,
                () -> creator.create(connectionFactory, table, columnFamily, tracker));

      } catch (ExecutionException e) {
        LOG.error("Unable to initialize an enrichment lookup: {}", e.getMessage(), e);
      }
      return lookup;
    }

    @Override
    public void initialize(Context context) {
      try {
        Map<String, Object> config = getConfig(context);
        initializeConnectionFactory(config);
        initializeTracker(config, connectionFactory);

      } catch (IOException e) {
        LOG.error("Unable to initialize ENRICHMENT_GET: {}", e.getMessage(), e);

      } finally {
        initialized = true;
      }
    }

    @Override
    public void close() {
      lookupCache.invalidateAll();
      lookupCache.cleanUp();
    }

    @Override
    public boolean isInitialized() {
      return initialized;
    }
  }
}
