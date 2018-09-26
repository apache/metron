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
package org.apache.metron.parsers;

import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.FieldTransformer;
import org.apache.metron.common.configuration.FieldValidator;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.error.MetronError;
import org.apache.metron.common.message.metadata.RawMessage;
import org.apache.metron.common.utils.ReflectionUtils;
import org.apache.metron.parsers.filters.Filters;
import org.apache.metron.parsers.interfaces.MessageFilter;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.topology.ParserComponent;
import org.apache.metron.stellar.common.CachingStellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ParserRunner implements Serializable {

  protected transient Consumer<ParserResult> onSuccess;
  protected transient Consumer<MetronError> onError;

  private HashSet<String> sensorTypes;
  private Map<String, ParserComponent> sensorToParserComponentMap;

  // Stellar variables
  private transient Context stellarContext;

  public ParserRunner(HashSet<String> sensorTypes) {
    this.sensorTypes = sensorTypes;
  }

  public Map<String, ParserComponent> getSensorToParserComponentMap() {
    return sensorToParserComponentMap;
  }

  public void setSensorToParserComponentMap(Map<String, ParserComponent> sensorToParserComponentMap) {
    this.sensorToParserComponentMap = sensorToParserComponentMap;
  }

  public Context getStellarContext() {
    return stellarContext;
  }

  public void setOnSuccess(Consumer<ParserResult> onSuccess) {
    this.onSuccess = onSuccess;
  }

  public void setOnError(Consumer<MetronError> onError) {
    this.onError = onError;
  }

  public void init(CuratorFramework client, Supplier<ParserConfigurations> parserConfigSupplier) {
    if (parserConfigSupplier == null) {
      throw new IllegalStateException("A parser config supplier must be set before initializing the ParserRunner.");
    }
    initializeStellar(client, parserConfigSupplier);
    initializeParsers(parserConfigSupplier);
  }

  protected void initializeStellar(CuratorFramework client, Supplier<ParserConfigurations> parserConfigSupplier) {
    Map<String, Object> cacheConfig = new HashMap<>();
    for (String sensorType: sensorTypes) {
      SensorParserConfig config = parserConfigSupplier.get().getSensorParserConfig(sensorType);

      if (config != null) {
        cacheConfig.putAll(config.getCacheConfig());
      }
    }
    Cache<CachingStellarProcessor.Key, Object> cache = CachingStellarProcessor.createCache(cacheConfig);

    Context.Builder builder = new Context.Builder()
            .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
            .with(Context.Capabilities.GLOBAL_CONFIG, () -> parserConfigSupplier.get().getGlobalConfig())
            .with(Context.Capabilities.STELLAR_CONFIG, () -> parserConfigSupplier.get().getGlobalConfig())
            ;
    if(cache != null) {
      builder = builder.with(Context.Capabilities.CACHE, () -> cache);
    }
    this.stellarContext = builder.build();
    StellarFunctions.initialize(stellarContext);
  }

  private void initializeParsers(Supplier<ParserConfigurations> parserConfigSupplier) {
    sensorToParserComponentMap = new HashMap<>();
    for(String sensorType: sensorTypes) {
      if (parserConfigSupplier.get().getSensorParserConfig(sensorType) == null) {
        throw new IllegalStateException(String.format("Could not initialize parsers.  Cannot find config for sensor %s.",
                sensorType));
      }

      SensorParserConfig parserConfig = parserConfigSupplier.get().getSensorParserConfig(sensorType);
      // create message parser
      MessageParser<JSONObject> parser = ReflectionUtils
              .createInstance(parserConfig.getParserClassName());

      // create message filter
      MessageFilter<JSONObject> filter = null;
      parserConfig.getParserConfig().putIfAbsent("stellarContext", stellarContext);
      if (!StringUtils.isEmpty(parserConfig.getFilterClassName())) {
        filter = Filters.get(
                parserConfig.getFilterClassName(),
                parserConfig.getParserConfig()
        );
      }
      parser.configure(parserConfig.getParserConfig());
      parser.init();
      sensorToParserComponentMap.put(sensorType, new ParserComponent(parser, filter));
    }
  }

  public void execute(String sensorType, RawMessage rawMessage, ParserConfigurations parserConfigurations) {
    if (onSuccess == null) {
      throw new IllegalStateException("An onSuccess function must be set before parsing a message.");
    }
    if (onError == null) {
      throw new IllegalStateException("An onError function must be set before parsing a message.");
    }
    SensorParserConfig sensorParserConfig = parserConfigurations.getSensorParserConfig(sensorType);
    if (sensorParserConfig != null) {
      MessageParser<JSONObject> parser = sensorToParserComponentMap.get(sensorType).getMessageParser();
      List<JSONObject> messages = parser.parseOptional(rawMessage.getMessage()).orElse(Collections.emptyList());
      messages.forEach(message -> processMessage(sensorType, message, rawMessage, parser, parserConfigurations));
    }
  }

  @SuppressWarnings("unchecked")
  protected void processMessage(String sensorType, JSONObject message, RawMessage rawMessage, MessageParser<JSONObject> parser, ParserConfigurations parserConfigurations) {
    SensorParserConfig sensorParserConfig = parserConfigurations.getSensorParserConfig(sensorType);
    sensorParserConfig.getRawMessageStrategy().mergeMetadata(
            message,
            rawMessage.getMetadata(),
            sensorParserConfig.getMergeMetadata(),
            sensorParserConfig.getRawMessageStrategyConfig()
    );
    message.put(Constants.SENSOR_TYPE, sensorType);
    applyFieldTransformations(message, rawMessage.getMetadata(), sensorParserConfig);
    if (!message.containsKey(Constants.GUID)) {
      message.put(Constants.GUID, UUID.randomUUID().toString());
    }
    MessageFilter<JSONObject> filter = sensorToParserComponentMap.get(sensorType).getFilter();
    if (filter == null || filter.emit(message, stellarContext)) {
      boolean isInvalid = !parser.validate(message);
      List<FieldValidator> failedValidators = null;
      if (!isInvalid) {
        failedValidators = getFailedValidators(message, parserConfigurations);
        isInvalid = !failedValidators.isEmpty();
      }
      if (isInvalid) {
        MetronError error = new MetronError()
                .withErrorType(Constants.ErrorType.PARSER_INVALID)
                .withSensorType(Collections.singleton(sensorType))
                .addRawMessage(message);
        Set<String> errorFields = failedValidators == null ? null : failedValidators.stream()
                .flatMap(fieldValidator -> fieldValidator.getInput().stream())
                .collect(Collectors.toSet());
        if (errorFields != null && !errorFields.isEmpty()) {
          error.withErrorFields(errorFields);
        }
        onError.accept(error);
      } else {
        onSuccess.accept(new ParserResult(sensorType, message, rawMessage.getMessage()));
      }
    }
  }

  private void applyFieldTransformations(JSONObject message, Map<String, Object> metadata, SensorParserConfig sensorParserConfig) {
    for (FieldTransformer handler : sensorParserConfig.getFieldTransformations()) {
      if (handler != null) {
        if (!sensorParserConfig.getMergeMetadata()) {
          //if we haven't merged metadata, then we need to pass them along as configuration params.
          handler.transformAndUpdate(
                  message,
                  stellarContext,
                  sensorParserConfig.getParserConfig(),
                  metadata
          );
        } else {
          handler.transformAndUpdate(
                  message,
                  stellarContext,
                  sensorParserConfig.getParserConfig()
          );
        }
      }
    }
  }

  private List<FieldValidator> getFailedValidators(JSONObject message, ParserConfigurations parserConfigurations) {
    List<FieldValidator> fieldValidations = parserConfigurations.getFieldValidations();
    List<FieldValidator> failedValidators = new ArrayList<>();
    for(FieldValidator validator : fieldValidations) {
      if(!validator.isValid(message, parserConfigurations.getGlobalConfig(), stellarContext)) {
        failedValidators.add(validator);
      }
    }
    return failedValidators;
  }
}
