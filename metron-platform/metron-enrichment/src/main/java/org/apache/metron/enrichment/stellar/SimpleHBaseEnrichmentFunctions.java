package org.apache.metron.enrichment.stellar;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.log4j.Logger;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.dsl.StellarFunction;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.EnrichmentLookup;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.enrichment.lookup.accesstracker.AccessTracker;
import org.apache.metron.enrichment.lookup.accesstracker.AccessTrackers;
import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.TableProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class SimpleHBaseEnrichmentFunctions {
  private static final Logger LOG = Logger.getLogger(SimpleHBaseEnrichmentFunctions.class);
  public static final String ACCESS_TRACKER_TYPE_CONF = "accessTracker";
  public static final String TABLE_PROVIDER_TYPE_CONF = "tableProviderImpl";
  private static AccessTracker tracker;
  private static TableProvider provider;


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


  private static synchronized void initializeTracker(Map<String, Object> config, TableProvider provider) throws IOException {
    if(tracker == null) {
      String accessTrackerType = (String) config.getOrDefault(ACCESS_TRACKER_TYPE_CONF, AccessTrackers.NOOP.toString());
      AccessTrackers trackers = AccessTrackers.valueOf(accessTrackerType);
      tracker = trackers.create(config, provider);
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
    private static Cache<Table, EnrichmentLookup> enrichmentCollateralCache = CacheBuilder.newBuilder()
                                                                                        .build();
    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(!initialized) {
        return false;
      }
      if(args.size() < 2) {
        throw new IllegalStateException("Requires at least an enrichment type and indicator");
      }
      int i = 0;
      String enrichmentType = (String) args.get(i++);
      String indicator = (String) args.get(i++);
      String table = (String) args.get(i++);
      String cf = (String) args.get(i++);
      if(enrichmentType == null || indicator == null) {
        return false;
      }
      final Table key = new Table(table, cf);
      EnrichmentLookup lookup = null;
      try {
        lookup = enrichmentCollateralCache.get(key, () -> {
            HTableInterface hTable = provider.getTable(HBaseConfiguration.create(), key.name);
            return new EnrichmentLookup(hTable, key.columnFamily, tracker);
          }
        );
      } catch (ExecutionException e) {
        LOG.error("Unable to retrieve enrichmentLookup: " + e.getMessage(), e);
        return false;
      }
      EnrichmentLookup.HBaseContext hbaseContext = new EnrichmentLookup.HBaseContext(lookup.getTable(), cf);
      try {
        return lookup.exists(new EnrichmentKey(enrichmentType, indicator), hbaseContext, true);
      } catch (IOException e) {
        LOG.error("Unable to call exists: " + e.getMessage(), e);
        return false;
      }
    }

    @Override
    public void initialize(Context context) {
      try {
        Map<String, Object> config = getConfig(context);
        initializeProvider(config);
        initializeTracker(config, provider);
      } catch (IOException e) {
        LOG.error("Unable to initialize ENRICHMENT.EXISTS: " + e.getMessage(), e);
      }
      finally{
        initialized = true;
      }

    }

    @Override
    public boolean isInitialized() {
      return initialized;
    }

  }

  private static TableProvider createProvider(String tableProviderClass) {
    try {
      Class<? extends TableProvider> providerClazz = (Class<? extends TableProvider>) Class.forName(tableProviderClass);
      return providerClazz.newInstance();
    } catch (Exception e) {
      return new HTableProvider();
    }
  }

  private static Map<String, Object> getConfig(Context context) {
    return (Map<String, Object>) context.getCapability(Context.Capabilities.GLOBAL_CONFIG).orElse(new HashMap<>());
  }

  private static synchronized void initializeProvider( Map<String, Object> config) {
    if(provider != null) {
      return ;
    }
    else {
      String tableProviderClass = (String) config.getOrDefault(TABLE_PROVIDER_TYPE_CONF, HTableProvider.class.getName());
      provider = createProvider(tableProviderClass);
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
    private static Cache<Table, EnrichmentLookup> enrichmentCollateralCache = CacheBuilder.newBuilder()
                                                                                        .build();
    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      if(!initialized) {
        return false;
      }
      if(args.size() < 2) {
        throw new IllegalStateException("Requires at least an enrichment type and indicator");
      }
      int i = 0;
      String enrichmentType = (String) args.get(i++);
      String indicator = (String) args.get(i++);
      String table = (String) args.get(i++);
      String cf = (String) args.get(i++);
      if(enrichmentType == null || indicator == null) {
        return new HashMap<String, Object>();
      }
      final Table key = new Table(table, cf);
      EnrichmentLookup lookup = null;
      try {
        lookup = enrichmentCollateralCache.get(key, () -> {
                  HTableInterface hTable = provider.getTable(HBaseConfiguration.create(), key.name);
                  return new EnrichmentLookup(hTable, key.columnFamily, tracker);
                }
        );
      } catch (ExecutionException e) {
        LOG.error("Unable to retrieve enrichmentLookup: " + e.getMessage(), e);
        return new HashMap<String, Object>();
      }
      EnrichmentLookup.HBaseContext hbaseContext = new EnrichmentLookup.HBaseContext(lookup.getTable(), cf);
      try {
        LookupKV<EnrichmentKey, EnrichmentValue> kv = lookup.get(new EnrichmentKey(enrichmentType, indicator), hbaseContext, true);
        if (kv != null && kv.getValue() != null && kv.getValue().getMetadata() != null) {
          return kv.getValue().getMetadata();
        }
        return new HashMap<String, Object>();
      } catch (IOException e) {
        LOG.error("Unable to call exists: " + e.getMessage(), e);
        return new HashMap<String, Object>();
      }
    }

    @Override
    public void initialize(Context context) {
      try {
        Map<String, Object> config = getConfig(context);
        initializeProvider(config);
        initializeTracker(config, provider);
      } catch (IOException e) {
        LOG.error("Unable to initialize ENRICHMENT.GET: " + e.getMessage(), e);
      }
      finally{
        initialized = true;
      }
    }

    @Override
    public boolean isInitialized() {
      return initialized;
    }
  }
}
