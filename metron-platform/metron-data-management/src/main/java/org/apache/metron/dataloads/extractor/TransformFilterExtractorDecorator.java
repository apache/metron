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
package org.apache.metron.dataloads.extractor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.ref.Reference;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.MapVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.stellar.common.StellarPredicateProcessor;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.metron.dataloads.extractor.TransformFilterExtractorDecorator.ExtractorOptions.*;

public class TransformFilterExtractorDecorator extends ExtractorDecorator implements StatefulExtractor {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String STATE_KEY = "state";
  public static final String STATES_KEY = "states";


  protected enum ExtractorOptions {
    VALUE_TRANSFORM("value_transform"),
    VALUE_FILTER("value_filter"),
    INDICATOR_TRANSFORM("indicator_transform"),
    INDICATOR_FILTER("indicator_filter"),
    ZK_QUORUM("zk_quorum"),
    INDICATOR("indicator"),
    STATE_INIT("state_init"),
    STATE_UPDATE("state_update"),
    STATE_MERGE("state_merge");

    private String key;

    ExtractorOptions(String key) {
      this.key = key;
    }

    @Override
    public String toString() {
      return key;
    }

    public boolean existsIn(Map<String, Object> config) {
      return config == null?false:config.containsKey(key);
    }
  }

  private Optional<CuratorFramework> zkClient;
  private Map<String, String> valueTransforms;
  private Map<String, String> indicatorTransforms;
  private String valueFilter;
  private String indicatorFilter;
  private Context stellarContext;
  private StellarProcessor transformProcessor;
  private StellarPredicateProcessor filterProcessor;
  private Map<String, Object> globalConfig;
  private Map<String, String> stateUpdate;
  private String stateMerge;
  private Set<ExtractorCapabilities> capabilities;

  public TransformFilterExtractorDecorator(Extractor decoratedExtractor) {
    super(decoratedExtractor);
    this.zkClient = Optional.empty();
    this.valueTransforms = new LinkedHashMap<>();
    this.indicatorTransforms = new LinkedHashMap<>();
    this.valueFilter = "";
    this.indicatorFilter = "";
    this.stateUpdate = new LinkedHashMap<>();
    this.stateMerge = "";
    this.transformProcessor = new StellarProcessor();
    this.filterProcessor = new StellarPredicateProcessor();
    this.capabilities = EnumSet.noneOf(ExtractorCapabilities.class);
  }

  @Override
  public void initialize(Map<String, Object> config) {
    super.initialize(config);
    if (VALUE_TRANSFORM.existsIn(config)) {
      this.valueTransforms = getTransforms(config, VALUE_TRANSFORM.toString());
    }
    if (INDICATOR_TRANSFORM.existsIn(config)) {
      this.indicatorTransforms = getTransforms(config, INDICATOR_TRANSFORM.toString());
    }
    if (VALUE_FILTER.existsIn(config)) {
      this.valueFilter = getFilter(config, VALUE_FILTER.toString());
    }
    if (INDICATOR_FILTER.existsIn(config)) {
      this.indicatorFilter = getFilter(config, INDICATOR_FILTER.toString());
    }
    if (STATE_UPDATE.existsIn(config)) {
      capabilities.add(ExtractorCapabilities.STATEFUL);
      this.stateUpdate = getTransforms(config, STATE_UPDATE.toString());
    }
    if(STATE_INIT.existsIn(config)) {
      capabilities.add(ExtractorCapabilities.STATEFUL);
    }
    if (STATE_MERGE.existsIn(config)) {
      capabilities.add(ExtractorCapabilities.MERGEABLE);
      this.stateMerge = getFilter(config, STATE_MERGE.toString());
    }
    String zkClientUrl = "";
    if (ZK_QUORUM.existsIn(config)) {
      zkClientUrl = ConversionUtils.convert(config.get(ZK_QUORUM.toString()), String.class);
    }
    zkClient = setupClient(zkClient, zkClientUrl);
    this.globalConfig = getGlobalConfig(zkClient);
    this.stellarContext = createContext(zkClient);
    StellarFunctions.initialize(stellarContext);
    this.transformProcessor = new StellarProcessor();
    this.filterProcessor = new StellarPredicateProcessor();

  }

  @Override
  public Object initializeState(Map<String, Object> config) {
    if(STATE_INIT.existsIn(config)) {
      MapVariableResolver resolver = new MapVariableResolver(globalConfig);
      return transformProcessor.parse( config.get(STATE_INIT.toString()).toString()
                                      , resolver
                                      , StellarFunctions.FUNCTION_RESOLVER()
                                      , stellarContext
                                      );
    }
    return null;
  }

  @Override
  public Object mergeStates(List<? extends Object> states) {
    return transformProcessor.parse( stateMerge
                            , new MapVariableResolver(new HashMap<String, Object>() {{ put(STATES_KEY, states); }}, globalConfig)
                            , StellarFunctions.FUNCTION_RESOLVER()
                            , stellarContext
                            );
  }

  private String getFilter(Map<String, Object> config, String valueFilter) {
    return (String) config.get(valueFilter);
  }

  /**
   * Get a map of the transformations from the config of the specified type
   * @param config main config map
   * @param type the transformation type to get from config
   * @return map of transformations.
   */
  private Map<String, String> getTransforms(Map<String, Object> config, String type) {
    // If this isn't a Map of Strings, let an exception be thrown
    @SuppressWarnings("unchecked") Map<Object, Object> transformsConfig = (Map) config.get(type);
    Map<String, String> transforms = new LinkedHashMap<>();
    for (Map.Entry<Object, Object> e : transformsConfig.entrySet()) {
      transforms.put((String) e.getKey(), (String) e.getValue());
    }
    return transforms;
  }

  /**
   * Creates a Zookeeper client if it doesn't exist and a url for zk is provided.
   * @param zookeeperUrl The Zookeeper URL.
   */
  private Optional<CuratorFramework> setupClient(Optional<CuratorFramework> zkClient, String zookeeperUrl) {
    // can only create client if we have a valid zookeeper URL
    if (!zkClient.isPresent()) {
      if (StringUtils.isNotBlank(zookeeperUrl)) {
        CuratorFramework client = ConfigurationsUtils.getClient(zookeeperUrl);
        client.start();
        return Optional.of(client);
      } else {
        LOG.warn("Unable to setup zookeeper client - zk_quorum url not provided. **This will limit some Stellar functionality**");
        return Optional.empty();
      }
    } else {
      return zkClient;
    }
  }

  private Map<String, Object> getGlobalConfig(Optional<CuratorFramework> zkClient) {
    if (zkClient.isPresent()) {
      try {
        return JSONUtils.INSTANCE.load(
                new ByteArrayInputStream(ConfigurationsUtils.readGlobalConfigBytesFromZookeeper(zkClient.get())),
                JSONUtils.MAP_SUPPLIER);
      } catch (Exception e) {
        LOG.warn("Exception thrown while attempting to get global config from Zookeeper.", e);
      }
    }
    return new LinkedHashMap<>();
  }

  private Context createContext(Optional<CuratorFramework> zkClient) {
    Context.Builder builder = new Context.Builder();
    if (zkClient.isPresent()) {
      builder.with(Context.Capabilities.ZOOKEEPER_CLIENT, zkClient::get);
    }
    builder.with(Context.Capabilities.GLOBAL_CONFIG, () -> globalConfig);
    builder.with(Context.Capabilities.STELLAR_CONFIG, () -> globalConfig);
    return builder.build();
  }

  @Override
  public Set<ExtractorCapabilities> getCapabilities() {
    return capabilities;
  }

  @Override
  public Iterable<LookupKV> extract(String line) throws IOException {
    return extract(line, new AtomicReference<>(null));
  }

  @Override
  public Iterable<LookupKV> extract(String line, AtomicReference<Object> state) throws IOException {
    List<LookupKV> lkvs = new ArrayList<>();
    for (LookupKV lkv : super.extract(line)) {
      if (updateLookupKV(lkv, state)) {
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
  private boolean updateLookupKV(LookupKV lkv, AtomicReference<Object> state) {
    Map<String, Object> ret = lkv.getValue().getMetadata();
    Map<String, Object> ind = new LinkedHashMap<>();
    String indicator = lkv.getKey().getIndicator();
    // add indicator as a resolvable variable. Also enable using resolved/transformed variables and values from operating on the value metadata
    ind.put(INDICATOR.toString(), indicator);
    Map<String, Object> stateMap = new LinkedHashMap<>();
    stateMap.put(STATE_KEY, state.get());
    MapVariableResolver resolver = new MapVariableResolver(ret, ind, globalConfig, stateMap);
    transform(valueTransforms, ret, resolver);
    transform(indicatorTransforms, ind, resolver);
    // update indicator
    Object updatedIndicator = ind.get(INDICATOR.toString());
    if (updatedIndicator != null || getCapabilities().contains(ExtractorCapabilities.STATEFUL)) {
      if (!(updatedIndicator instanceof String)) {
        throw new UnsupportedOperationException("Indicator transform must return String type");
      }
      lkv.getKey().setIndicator((String) updatedIndicator);
      boolean update = filter(indicatorFilter, resolver) && filter(valueFilter, resolver);
      if(update && !stateUpdate.isEmpty()) {
        transform(stateUpdate, stateMap, resolver);
        state.set(stateMap.get(STATE_KEY));
      }
      return update;
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
    if(StringUtils.isEmpty(filterPredicate)) {
      return true;
    }
    return filterProcessor.parse(filterPredicate, variableResolver, StellarFunctions.FUNCTION_RESOLVER(), stellarContext);
  }

  protected void setZkClient(Optional<CuratorFramework> zkClient) {
    this.zkClient = zkClient;
  }

}
