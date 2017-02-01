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
  }

  @Override
  public void initialize(Map<String, Object> config) {
    super.initialize(config);
    if (config.containsKey(VALUE_TRANSFORM)) {
      this.valueTransforms = getTransforms(config.get(VALUE_TRANSFORM));
    } else {
      this.valueTransforms = new HashMap<>();
    }
    if (config.containsKey(INDICATOR_TRANSFORM)) {
      this.indicatorTransforms = getTransforms(config.get(INDICATOR_TRANSFORM));
    } else {
      this.indicatorTransforms = new HashMap<>();
    }
    if (config.containsKey(VALUE_FILTER)) {
      this.valueFilter = config.get(VALUE_FILTER).toString();
    }
    if (config.containsKey(INDICATOR_FILTER)) {
      this.indicatorFilter = config.get(INDICATOR_FILTER).toString();
    }
    String zkClientUrl = "";
    if (config.containsKey(ZK_QUORUM)) {
      zkClientUrl = config.get(ZK_QUORUM).toString();
    }
    Optional<CuratorFramework> zkClient = createClient(zkClientUrl);
    this.globalConfig = getGlobalConfig(zkClient);
    this.stellarContext = createContext(zkClient);
    StellarFunctions.initialize(stellarContext);
    this.transformProcessor = new StellarProcessor();
    this.filterProcessor = new StellarPredicateProcessor();
  }

  private Map<String, String> getTransforms(Object transformsConfig) {
    Map<String, String> transforms = new HashMap<>();
    if (transformsConfig instanceof Map) {
      Map<Object, Object> map = (Map<Object, Object>) transformsConfig;
      for (Map.Entry<Object, Object> e : map.entrySet()) {
        transforms.put(e.getKey().toString(), e.getValue().toString());
      }
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

  private boolean updateLookupKV(LookupKV lkv) {
    Map<String, Object> ret = lkv.getValue().getMetadata();
    MapVariableResolver metadataResolver = new MapVariableResolver(ret, globalConfig);
    for (Map.Entry<String, String> entry : valueTransforms.entrySet()) {
      Object o = transformProcessor.parse(entry.getValue(), metadataResolver, StellarFunctions.FUNCTION_RESOLVER(), stellarContext);
      if (o == null) {
        ret.remove(entry.getKey());
      } else {
        ret.put(entry.getKey(), o);
      }
    }
    // update key
    // transform
    String indicator = lkv.getKey().getIndicator();
    // add indicator as a resolvable variable. Also enable using resolved/transformed variables and values from operating on the value metadata
    Map<String, Object> ind = new HashMap<>();
    ind.putAll(ret);
    ind.put("indicator", indicator);
    MapVariableResolver indicatorResolver = new MapVariableResolver(ind, globalConfig);
    for (Map.Entry<String, String> entry : indicatorTransforms.entrySet()) {
      Object o = transformProcessor.parse(entry.getValue(), indicatorResolver, StellarFunctions.FUNCTION_RESOLVER(), stellarContext);
      if (o == null) {
        ind.remove(entry.getKey());
      } else {
        ind.put(entry.getKey(), o);
      }
    }
    // update indicator
    if (ind.get("indicator") != null) {
      lkv.getKey().setIndicator(ind.get("indicator").toString());
    }
    // filter on indicator not being empty and both filters passing muster
    return (ind.get("indicator") != null)
            && filterProcessor.parse(indicatorFilter, metadataResolver, StellarFunctions.FUNCTION_RESOLVER(), stellarContext)
            && filterProcessor.parse(valueFilter, metadataResolver, StellarFunctions.FUNCTION_RESOLVER(), stellarContext);
  }

}
