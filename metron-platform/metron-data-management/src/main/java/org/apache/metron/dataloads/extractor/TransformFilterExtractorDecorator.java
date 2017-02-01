package org.apache.metron.dataloads.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.log4j.Logger;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.MapVariableResolver;
import org.apache.metron.common.dsl.StellarFunctions;
import org.apache.metron.common.stellar.StellarPredicateProcessor;
import org.apache.metron.common.stellar.StellarProcessor;
import org.apache.metron.common.utils.ConversionUtils;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.enrichment.lookup.LookupKV;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

public class TransformFilterExtractorDecorator extends ExtractorDecorator {
  private static final Logger LOG = Logger.getLogger(TransformFilterExtractorDecorator.class);
  private static final String VALUE_TRANSFORM = "value_transform";
  private static final String VALUE_FILTER = "value_filter";
  private static final String INDICATOR_TRANSFORM = "indicator_transform";
  private static final String INDICATOR_FILTER = "indicator_filter";
  private static final String ZK_QUORUM = "zk_quorum";
  private static final String INDICATOR = "indicator";
  private Map<String, String> valueTransforms;
  private Map<String, String> indicatorTransforms;
  private String valueFilter;
  private String indicatorFilter;
  private Context stellarContext;
  private StellarProcessor transformProcessor;
  private StellarPredicateProcessor filterProcessor;
  private Map<String, Object> globalConfig;

  public TransformFilterExtractorDecorator(Extractor decoratedExtractor) {
    super(decoratedExtractor);
    this.valueTransforms = new HashMap<>();
    this.indicatorTransforms = new HashMap<>();
    this.valueFilter = "";
    this.indicatorFilter = "";
  }

  @Override
  public void initialize(Map<String, Object> config) {
    super.initialize(config);
    if (config.containsKey(VALUE_TRANSFORM)) {
      this.valueTransforms = getTransforms(config, VALUE_TRANSFORM);
    }
    if (config.containsKey(INDICATOR_TRANSFORM)) {
      this.indicatorTransforms = getTransforms(config, INDICATOR_TRANSFORM);
    }
    if (config.containsKey(VALUE_FILTER)) {
      this.valueFilter = getFilter(config, VALUE_FILTER);
    }
    if (config.containsKey(INDICATOR_FILTER)) {
      this.indicatorFilter = getFilter(config, INDICATOR_FILTER);
    }
    String zkClientUrl = "";
    if (config.containsKey(ZK_QUORUM)) {
      zkClientUrl = ConversionUtils.convert(config.get(ZK_QUORUM), String.class);
    }
    Optional<CuratorFramework> zkClient = createClient(zkClientUrl);
    this.globalConfig = getGlobalConfig(zkClient);
    this.stellarContext = createContext(zkClient);
    StellarFunctions.initialize(stellarContext);
    this.transformProcessor = new StellarProcessor();
    this.filterProcessor = new StellarPredicateProcessor();
  }

  private String getFilter(Map<String, Object> config, String valueFilter) {
    return ConversionUtils.convertOrFail(config.get(valueFilter), String.class);
  }

  /**
   * Get a map of the transformations from the config of the specified type
   * @param config main config map
   * @param type the transformation type to get from config
   * @return map of transformations.
   */
  private Map<String, String> getTransforms(Map<String, Object> config, String type) {
    Map<Object, Object> transformsConfig = ConversionUtils.convertOrFail(config.get(type), Map.class);
    Map<String, String> transforms = new HashMap<>();
    for (Map.Entry<Object, Object> e : transformsConfig.entrySet()) {
      String key = ConversionUtils.convertOrFail(e.getKey(), String.class);
      String val = ConversionUtils.convertOrFail(e.getValue(), String.class);
      transforms.put(key, val);
    }
    return transforms;
  }

  /**
   * Creates a Zookeeper client.
   * @param zookeeperUrl The Zookeeper URL.
   */
  private Optional<CuratorFramework> createClient(String zookeeperUrl) {
    // can only create client, if have valid zookeeper URL
    if (StringUtils.isNotBlank(zookeeperUrl)) {
      CuratorFramework client = ConfigurationsUtils.getClient(zookeeperUrl);
      client.start();
      return Optional.of(client);
    } else {
      LOG.warn("Unable to setup zookeeper client - zk_quorum url not provided. **This will limit some Stellar functionality**");
      return Optional.empty();
    }
  }

  private Map<String, Object> getGlobalConfig(Optional<CuratorFramework> zkClient) {
    if (zkClient.isPresent()) {
      try {
        return JSONUtils.INSTANCE.load(
                new ByteArrayInputStream(ConfigurationsUtils.readGlobalConfigBytesFromZookeeper(zkClient.get())),
                new TypeReference<Map<String, Object>>() {
                });
      } catch (Exception e) {
        LOG.warn("Exception thrown while attempting to get global config from Zookeeper.", e);
      }
    }
    return new HashMap<>();
  }

  private Context createContext(Optional<CuratorFramework> zkClient) {
    Context.Builder builder = new Context.Builder();
    if (zkClient.isPresent()) {
      builder.with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> zkClient.get())
              .with(Context.Capabilities.GLOBAL_CONFIG, () -> globalConfig);
    }
    return builder.build();
  }

  @Override
  public Iterable<LookupKV> extract(String line) throws IOException {
    List<LookupKV> lkvs = new ArrayList<>();
    for (LookupKV lkv : super.extract(line)) {
      if (updateLookupKV(lkv)) {
        lkvs.add(lkv);
      }
    }
    return lkvs;
  }

  /**
   * Returns true if lookupkv is not null after transforms and filtering on the value and indicator key
   * @param lkv LookupKV to transform and filter
   * @return true if lkv is not null after transform/filter
   */
  private boolean updateLookupKV(LookupKV lkv) {
    Map<String, Object> ret = lkv.getValue().getMetadata();
    Map<String, Object> ind = new HashMap<>();
    String indicator = lkv.getKey().getIndicator();
    // add indicator as a resolvable variable. Also enable using resolved/transformed variables and values from operating on the value metadata
    ind.put(INDICATOR, indicator);
    MapVariableResolver resolver = new MapVariableResolver(ret, ind, globalConfig);
    transform(valueTransforms, ret, resolver);
    transform(indicatorTransforms, ind, resolver);
    // update indicator
    Object updatedIndicator = ind.get(INDICATOR);
    if (updatedIndicator != null) {
      if (!(updatedIndicator instanceof String)) {
        throw new UnsupportedOperationException("Indicator transform must return String type");
      }
      lkv.getKey().setIndicator((String) updatedIndicator);
      return filter(indicatorFilter, resolver) && filter(valueFilter, resolver);
    } else {
      return false;
    }
  }

  private void transform(Map<String, String> transforms, Map<String, Object> variableMap, MapVariableResolver variableResolver) {
    for (Map.Entry<String, String> entry : transforms.entrySet()) {
      Object o = transformProcessor.parse(entry.getValue(), variableResolver, StellarFunctions.FUNCTION_RESOLVER(), stellarContext);
      if (o == null) {
        variableMap.remove(entry.getKey());
      } else {
        variableMap.put(entry.getKey(), o);
      }
    }
  }

  private Boolean filter(String filterPredicate, MapVariableResolver variableResolver) {
    return filterProcessor.parse(filterPredicate, variableResolver, StellarFunctions.FUNCTION_RESOLVER(), stellarContext);
  }

}
