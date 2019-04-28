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

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
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
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implemention of a ParserRunner.
 */
public class ParserRunnerImpl implements ParserRunner<JSONObject>, Serializable {

  class ProcessResult {

    private JSONObject message;
    private MetronError error;

    public ProcessResult(JSONObject message) {
      this.message = message;
    }

    public ProcessResult(MetronError error) {
      this.error = error;
    }

    public JSONObject getMessage() {
      return message;
    }

    public MetronError getError() {
      return error;
    }

    public boolean isError() {
      return error != null;
    }
  }

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected transient Consumer<ParserRunnerResults> onSuccess;
  protected transient Consumer<MetronError> onError;

  private HashSet<String> sensorTypes;
  private Map<String, ParserComponent> sensorToParserComponentMap;

  // Stellar variables
  private transient Context stellarContext;

  public ParserRunnerImpl(HashSet<String> sensorTypes) {
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

  @Override
  public Set<String> getSensorTypes() {
    return sensorTypes;
  }

  @Override
  public void init(Supplier<ParserConfigurations> parserConfigSupplier, Context stellarContext) {
    if (parserConfigSupplier == null) {
      throw new IllegalStateException("A parser config supplier must be set before initializing the ParserRunner.");
    }
    if (stellarContext == null) {
      throw new IllegalStateException("A stellar context must be set before initializing the ParserRunner.");
    }
    this.stellarContext = stellarContext;
    initializeParsers(parserConfigSupplier);
  }

  /**
   * Parses messages with the appropriate MessageParser based on sensor type.  The resulting list of messages are then
   * post-processed and added to the ParserRunnerResults message list.  Any errors that happen during post-processing are
   * added to the ParserRunnerResults error list.  Any exceptions (including a master exception) thrown by the MessageParser
   * are also added to the ParserRunnerResults error list.
   *
   * @param sensorType Sensor type of the message
   * @param rawMessage Raw message including metadata
   * @param parserConfigurations Parser configurations
   * @return ParserRunnerResults containing a list of messages and a list of errors
   */
  @Override
  public ParserRunnerResults<JSONObject> execute(String sensorType, RawMessage rawMessage, ParserConfigurations parserConfigurations) {
    DefaultParserRunnerResults parserRunnerResults = new DefaultParserRunnerResults();
    SensorParserConfig sensorParserConfig = parserConfigurations.getSensorParserConfig(sensorType);
    if (sensorParserConfig != null) {
      MessageParser<JSONObject> parser = sensorToParserComponentMap.get(sensorType).getMessageParser();
      Optional<MessageParserResult<JSONObject>> optionalMessageParserResult = parser.parseOptionalResult(rawMessage.getMessage());
      if (optionalMessageParserResult.isPresent()) {
        MessageParserResult<JSONObject> messageParserResult = optionalMessageParserResult.get();

        // Process each message returned from the MessageParser
        messageParserResult.getMessages().forEach(message -> {
                  Optional<ProcessResult> processResult = processMessage(sensorType, message, rawMessage, parser, parserConfigurations);
                  if (processResult.isPresent()) {
                    if (processResult.get().isError()) {
                      parserRunnerResults.addError(processResult.get().getError());
                    } else {
                      parserRunnerResults.addMessage(processResult.get().getMessage());
                    }
                  }
                });

        // If a master exception is thrown by the MessageParser, wrap it with a MetronError and add it to the list of errors
        messageParserResult.getMasterThrowable().ifPresent(throwable -> parserRunnerResults.addError(new MetronError()
                .withErrorType(Constants.ErrorType.PARSER_ERROR)
                .withThrowable(throwable)
                .withSensorType(Collections.singleton(sensorType))
                .withMetadata(rawMessage.getMetadata())
                .addRawMessage(rawMessage.getMessage())));

        // If exceptions are thrown by the MessageParser, wrap them with MetronErrors and add them to the list of errors
        parserRunnerResults.addErrors(messageParserResult.getMessageThrowables().entrySet().stream().map(entry -> new MetronError()
                .withErrorType(Constants.ErrorType.PARSER_ERROR)
                .withThrowable(entry.getValue())
                .withSensorType(Collections.singleton(sensorType))
                .withMetadata(rawMessage.getMetadata())
                .addRawMessage(entry.getKey())).collect(Collectors.toList()));
      }
    } else {
      throw new IllegalStateException(String.format("Could not execute parser.  Cannot find configuration for sensor %s.",
              sensorType));
    }
    return parserRunnerResults;
  }

  /**
   * Initializes MessageParsers and MessageFilters for sensor types configured in this ParserRunner.  Objects are created
   * using reflection and the MessageParser configure and init methods are called.
   * @param parserConfigSupplier Parser configurations
   */
  private void initializeParsers(Supplier<ParserConfigurations> parserConfigSupplier) {
    LOG.info("Initializing parsers...");
    sensorToParserComponentMap = new HashMap<>();
    for(String sensorType: sensorTypes) {
      if (parserConfigSupplier.get().getSensorParserConfig(sensorType) == null) {
        throw new IllegalStateException(String.format("Could not initialize parsers.  Cannot find configuration for sensor %s.",
                sensorType));
      }

      SensorParserConfig parserConfig = parserConfigSupplier.get().getSensorParserConfig(sensorType);

      LOG.info("Creating parser for sensor {} with parser class = {} and filter class = {} ",
              sensorType, parserConfig.getParserClassName(), parserConfig.getFilterClassName());

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

  /**
   * Post-processes parsed messages by:
   * <ul>
   *   <li>Applying field transformations defined in the sensor parser config</li>
   *   <li>Filtering messages using the configured MessageFilter class</li>
   *   <li>Validating messages using the MessageParser validate method</li>
   * </ul>
   * If a message is successfully processed a message is returned in a ProcessResult.  If a message fails
   * validation, a MetronError object is created and returned in a ProcessResult.  If a message is
   * filtered out an empty Optional is returned.
   *
   * @param sensorType Sensor type of the message
   * @param message Message parsed by the MessageParser
   * @param rawMessage Raw message including metadata
   * @param parser MessageParser for the sensor type
   * @param parserConfigurations Parser configurations
   */
  @SuppressWarnings("unchecked")
  protected Optional<ProcessResult> processMessage(String sensorType, JSONObject message, RawMessage rawMessage,
                                                  MessageParser<JSONObject> parser,
                                                  ParserConfigurations parserConfigurations
                                                  ) {
    Optional<ProcessResult> processResult = Optional.empty();
    SensorParserConfig sensorParserConfig = parserConfigurations.getSensorParserConfig(sensorType);
    sensorParserConfig.getRawMessageStrategy().mergeMetadata(
            message,
            rawMessage.getMetadata(),
            sensorParserConfig.getMergeMetadata(),
            sensorParserConfig.getRawMessageStrategyConfig()
    );
    message.put(Constants.SENSOR_TYPE, sensorType);
    applyFieldTransformations(message, rawMessage, sensorParserConfig);
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
                .withMetadata(rawMessage.getMetadata())
                .addRawMessage(message);
        Set<String> errorFields = failedValidators == null ? null : failedValidators.stream()
                .flatMap(fieldValidator -> fieldValidator.getInput().stream())
                .collect(Collectors.toSet());
        if (errorFields != null && !errorFields.isEmpty()) {
          error.withErrorFields(errorFields);
        }
        processResult = Optional.of(new ProcessResult(error));
      } else {
        processResult = Optional.of(new ProcessResult(message));
      }
    }
    return processResult;
  }

  /**
   * Applies Stellar field transformations defined in the sensor parser config.
   * @param message Message parsed by the MessageParser
   * @param rawMessage Raw message including metadata
   * @param sensorParserConfig Sensor parser config
   */
  private void applyFieldTransformations(JSONObject message, RawMessage rawMessage, SensorParserConfig sensorParserConfig) {
    for (FieldTransformer handler : sensorParserConfig.getFieldTransformations()) {
      if (handler != null) {
        if (!sensorParserConfig.getMergeMetadata()) {
          //if we haven't merged metadata, then we need to pass them along as configuration params.
          handler.transformAndUpdate(
                  message,
                  stellarContext,
                  sensorParserConfig.getParserConfig(),
                  rawMessage.getMetadata()
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
