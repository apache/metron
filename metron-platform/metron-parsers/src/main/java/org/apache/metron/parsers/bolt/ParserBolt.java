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

package org.apache.metron.parsers.bolt;


import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.common.bolt.ConfiguredParserBolt;
import org.apache.metron.common.configuration.FieldTransformer;
import org.apache.metron.common.configuration.FieldValidator;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.error.MetronError;
import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.metron.common.message.MessageGetters;
import org.apache.metron.common.message.metadata.RawMessage;
import org.apache.metron.common.message.metadata.RawMessageUtil;
import org.apache.metron.common.utils.ErrorUtils;
import org.apache.metron.parsers.filters.Filters;
import org.apache.metron.parsers.interfaces.MessageFilter;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.apache.metron.parsers.interfaces.MultilineMessageParser;
import org.apache.metron.parsers.topology.ParserComponents;
import org.apache.metron.stellar.common.CachingStellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.storm.kafka.flux.SimpleStormKafkaBuilder.FieldsConfiguration;
import org.apache.metron.writer.WriterToBulkWriter;
import org.apache.metron.writer.bolt.BatchTimeoutHelper;
import org.apache.storm.Config;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.utils.TupleUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ParserBolt extends ConfiguredParserBolt implements Serializable {


  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private OutputCollector collector;
  private Map<String, ParserComponents> sensorToComponentMap;
  private Map<String, String> topicToSensorMap = new HashMap<>();

  private Context stellarContext;
  private transient MessageGetStrategy messageGetStrategy;
  private transient Cache<CachingStellarProcessor.Key, Object> cache;
  private int requestedTickFreqSecs;
  private int defaultBatchTimeout;
  private int batchTimeoutDivisor = 1;

  public ParserBolt( String zookeeperUrl
                   , Map<String, ParserComponents> sensorToComponentMap
  ) {
    super(zookeeperUrl);
    this.sensorToComponentMap = sensorToComponentMap;

    // Ensure that all sensors are either bulk sensors or not bulk sensors.  Can't mix and match.
    Boolean handleAcks = null;
    for (Map.Entry<String, ParserComponents> entry : sensorToComponentMap.entrySet()) {
      boolean writerHandleAck = entry.getValue().getWriter().handleAck();
      if (handleAcks == null) {
        handleAcks = writerHandleAck;
      } else if (!handleAcks.equals(writerHandleAck)) {
        throw new IllegalArgumentException("All writers must match when calling handleAck()");
      }
    }
  }

  /**
   * If this ParserBolt is in a topology where it is daisy-chained with
   * other queuing Writers, then the max amount of time it takes for a tuple
   * to clear the whole topology is the sum of all the batchTimeouts for all the
   * daisy-chained Writers.  In the common case where each Writer is using the default
   * batchTimeout, it is then necessary to divide that batchTimeout by the number of
   * daisy-chained Writers.  There are no examples of daisy-chained batching Writers
   * in the current Metron topologies, but the feature is available as a "fluent"-style
   * mutator if needed.  It would be used in the parser topology builder.
   * Default value, if not otherwise set, is 1.
   *
   * If non-default batchTimeouts are configured for some components, the administrator
   * may want to take this behavior into account.
   *
   * @param batchTimeoutDivisor
   * @return BulkMessageWriterBolt
   */
  public ParserBolt withBatchTimeoutDivisor(int batchTimeoutDivisor) {
    if (batchTimeoutDivisor <= 0) {
      throw new IllegalArgumentException(String.format("batchTimeoutDivisor must be positive. Value provided was %s", batchTimeoutDivisor));
    }
    this.batchTimeoutDivisor = batchTimeoutDivisor;
    return this;
  }

  /**
   * Used only for unit testing
   * @param defaultBatchTimeout
   */
  protected void setDefaultBatchTimeout(int defaultBatchTimeout) {
    this.defaultBatchTimeout = defaultBatchTimeout;
  }

  /**
   * Used only for unit testing
   */
  public int getDefaultBatchTimeout() {
    return defaultBatchTimeout;
  }

  public Map<String, ParserComponents> getSensorToComponentMap() {
    return sensorToComponentMap;
  }

  /**
   * This method is called by TopologyBuilder.createTopology() to obtain topology and
   * bolt specific configuration parameters.  We use it primarily to configure how often
   * a tick tuple will be sent to our bolt.
   * @return conf topology and bolt specific configuration parameters
   */
  @Override
  public Map<String, Object> getComponentConfiguration() {
    // This is called long before prepare(), so do some of the same stuff as prepare() does,
    // to get the valid WriterConfiguration.  But don't store any non-serializable objects,
    // else Storm will throw a runtime error.
    Function<WriterConfiguration, WriterConfiguration> configurationXform;
    WriterHandler writer = sensorToComponentMap.entrySet().iterator().next().getValue().getWriter();
    if (writer.isWriterToBulkWriter()) {
      configurationXform = WriterToBulkWriter.TRANSFORMATION;
    } else {
      configurationXform = x -> x;
    }
    WriterConfiguration writerconf = configurationXform
        .apply(getConfigurationStrategy()
            .createWriterConfig(writer.getBulkMessageWriter(), getConfigurations()));

    BatchTimeoutHelper timeoutHelper = new BatchTimeoutHelper(writerconf::getAllConfiguredTimeouts, batchTimeoutDivisor);
    this.requestedTickFreqSecs = timeoutHelper.getRecommendedTickInterval();
    //And while we've got BatchTimeoutHelper handy, capture the defaultBatchTimeout for writerComponent.
    this.defaultBatchTimeout = timeoutHelper.getDefaultBatchTimeout();

    Map<String, Object> conf = super.getComponentConfiguration();
    if (conf == null) {
      conf = new HashMap<>();
    }
    conf.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, requestedTickFreqSecs);
    LOG.info("Requesting " + Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS + " set to " + Integer.toString(requestedTickFreqSecs));
    return conf;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    super.prepare(stormConf, context, collector);
    messageGetStrategy = MessageGetters.DEFAULT_BYTES_FROM_POSITION.get();
    this.collector = collector;

    // Build the Stellar cache
    Map<String, Object> cacheConfig = new HashMap<>();
    for (Map.Entry<String, ParserComponents> entry: sensorToComponentMap.entrySet()) {
      String sensor = entry.getKey();
      SensorParserConfig config = getSensorParserConfig(sensor);

      if (config != null) {
        cacheConfig.putAll(config.getCacheConfig());
      }
    }
    cache = CachingStellarProcessor.createCache(cacheConfig);

    // Need to prep all sensors
    for (Map.Entry<String, ParserComponents> entry: sensorToComponentMap.entrySet()) {
      String sensor = entry.getKey();
      MessageParser<JSONObject> parser = entry.getValue().getMessageParser();

      initializeStellar();
      if (getSensorParserConfig(sensor) != null && sensorToComponentMap.get(sensor).getFilter() == null) {
        getSensorParserConfig(sensor).getParserConfig().putIfAbsent("stellarContext", stellarContext);
        if (!StringUtils.isEmpty(getSensorParserConfig(sensor).getFilterClassName())) {
          MessageFilter<JSONObject> filter = Filters.get(
              getSensorParserConfig(sensor).getFilterClassName(),
              getSensorParserConfig(sensor).getParserConfig()
          );
          getSensorToComponentMap().get(sensor).setFilter(filter);
        }
      }

      parser.init();

      SensorParserConfig config = getSensorParserConfig(sensor);
      if (config != null) {
        config.init();
        topicToSensorMap.put(config.getSensorTopic(), sensor);
      } else {
        throw new IllegalStateException(
            "Unable to retrieve a parser config for " + sensor);
      }
      parser.configure(config.getParserConfig());

      WriterHandler writer = sensorToComponentMap.get(sensor).getWriter();
      writer.init(stormConf, context, collector, getConfigurations());
      if (defaultBatchTimeout == 0) {
        //This means getComponentConfiguration was never called to initialize defaultBatchTimeout,
        //probably because we are in a unit test scenario.  So calculate it here.
        WriterConfiguration writerConfig = getConfigurationStrategy()
            .createWriterConfig(writer.getBulkMessageWriter(), getConfigurations());
        BatchTimeoutHelper timeoutHelper = new BatchTimeoutHelper(
            writerConfig::getAllConfiguredTimeouts, batchTimeoutDivisor);
        defaultBatchTimeout = timeoutHelper.getDefaultBatchTimeout();
      }
      writer.setDefaultBatchTimeout(defaultBatchTimeout);
    }
  }

  protected void initializeStellar() {
    Context.Builder builder = new Context.Builder()
                                .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
                                .with(Context.Capabilities.GLOBAL_CONFIG, () -> getConfigurations().getGlobalConfig())
                                .with(Context.Capabilities.STELLAR_CONFIG, () -> getConfigurations().getGlobalConfig())
                                ;
    if(cache != null) {
      builder = builder.with(Context.Capabilities.CACHE, () -> cache);
    }
    this.stellarContext = builder.build();
    StellarFunctions.initialize(stellarContext);
  }


  @SuppressWarnings("unchecked")
  @Override
  public void execute(Tuple tuple) {
    if (TupleUtils.isTick(tuple)) {
      try {
        for (Entry<String, ParserComponents> entry : sensorToComponentMap.entrySet()) {
          entry.getValue().getWriter().flush(getConfigurations(), messageGetStrategy);
        }
      } catch (Exception e) {
        throw new RuntimeException(
            "This should have been caught in the writerHandler.  If you see this, file a JIRA", e);
      } finally {
        collector.ack(tuple);
      }
      return;
    }

    byte[] originalMessage = (byte[]) messageGetStrategy.get(tuple);
    try {
      SensorParserConfig sensorParserConfig;
      MessageParser<JSONObject> parser;
      String sensor;
      Map<String, Object> metadata;
      if (sensorToComponentMap.size() == 1) {
        // There's only one parser, so grab info directly
        Entry<String, ParserComponents> sensorParser = sensorToComponentMap.entrySet().iterator()
            .next();
        sensor = sensorParser.getKey();
        parser = sensorParser.getValue().getMessageParser();
        sensorParserConfig = getSensorParserConfig(sensor);
      } else {
        // There's multiple parsers, so pull the topic from the Tuple and look up the sensor
        String topic = tuple.getStringByField(FieldsConfiguration.TOPIC.getFieldName());
        sensor = topicToSensorMap.get(topic);
        parser = sensorToComponentMap.get(sensor).getMessageParser();
        sensorParserConfig = getSensorParserConfig(sensor);
      }

      List<FieldValidator> fieldValidations = getConfigurations().getFieldValidations();
      boolean ackTuple = false;
      int numWritten = 0;
      if (sensorParserConfig != null) {
        RawMessage rawMessage = RawMessageUtil.INSTANCE.getRawMessage( sensorParserConfig.getRawMessageStrategy()
            , tuple
            , originalMessage
            , sensorParserConfig.getReadMetadata()
            , sensorParserConfig.getRawMessageStrategyConfig()
        );

        metadata = rawMessage.getMetadata();

        MultilineMessageParser mmp = null;
        if (!(parser instanceof MultilineMessageParser)) {
          mmp = new MultilineMessageParser<JSONObject>() {

            @Override
            public void configure(Map<String, Object> config) {
              parser.configure(config);
            }

            @Override
            public void init() {
              parser.init();
            }

            @Override
            public boolean validate(JSONObject message) {
              return parser.validate(message);
            }

            @Override
            public List<JSONObject> parse(byte[] message) {
              return parser.parse(message);
            }

            @Override
            public Optional<List<JSONObject>> parseOptional(byte[] message) {
              return parser.parseOptional(message);
            }
          };
        } else {
          mmp = (MultilineMessageParser) parser;
        }

        Optional<MessageParserResult<JSONObject>> results = mmp.parseOptionalResult(rawMessage.getMessage());

        // check if there is a master error
        if (results.isPresent() && results.get().getMasterThrowable().isPresent()) {
          handleError(originalMessage, tuple, results.get().getMasterThrowable().get(), collector);
          return;
        }

        // Handle the message results
        List<JSONObject> messages = results.isPresent() ? results.get().getMessages() : Collections.EMPTY_LIST;
        for (JSONObject message : messages) {
          //we want to ack the tuple in the situation where we have are not doing a bulk write
          //otherwise we want to defer to the writerComponent who will ack on bulk commit.
          WriterHandler writer = sensorToComponentMap.get(sensor).getWriter();
          ackTuple = !writer.handleAck();

          sensorParserConfig.getRawMessageStrategy().mergeMetadata(
              message,
              metadata,
              sensorParserConfig.getMergeMetadata(),
              sensorParserConfig.getRawMessageStrategyConfig()
          );
          message.put(Constants.SENSOR_TYPE, sensor);

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
          if (!message.containsKey(Constants.GUID)) {
            message.put(Constants.GUID, UUID.randomUUID().toString());
          }

          MessageFilter<JSONObject> filter = sensorToComponentMap.get(sensor).getFilter();
          if (filter == null || filter.emitTuple(message, stellarContext)) {
            boolean isInvalid = !parser.validate(message);
            List<FieldValidator> failedValidators = null;
            if (!isInvalid) {
              failedValidators = getFailedValidators(message, fieldValidations);
              isInvalid = !failedValidators.isEmpty();
            }
            if (isInvalid) {
              MetronError error = new MetronError()
                  .withErrorType(Constants.ErrorType.PARSER_INVALID)
                  .withSensorType(Collections.singleton(sensor))
                  .addRawMessage(message);
              Set<String> errorFields = failedValidators == null ? null : failedValidators.stream()
                  .flatMap(fieldValidator -> fieldValidator.getInput().stream())
                  .collect(Collectors.toSet());
              if (errorFields != null && !errorFields.isEmpty()) {
                error.withErrorFields(errorFields);
              }
              ErrorUtils.handleError(collector, error);
            } else {
              numWritten++;
              writer.write(sensor, tuple, message, getConfigurations(), messageGetStrategy);
            }
          }
        }

        // Handle the error results
        Map<Object, Throwable> messageErrors = results.isPresent()
                ? results.get().getMessageThrowables() : Collections.EMPTY_MAP;

        for (Entry<Object,Throwable> entry : messageErrors.entrySet()) {
          MetronError error = new MetronError()
                  .withErrorType(Constants.ErrorType.PARSER_ERROR)
                  .withThrowable(entry.getValue())
                  .withSensorType(sensorToComponentMap.keySet())
                  .addRawMessage(originalMessage);
          ErrorUtils.handleError(collector, error);
        }
      }
      //if we are supposed to ack the tuple OR if we've never passed this tuple to the bulk writer
      //(meaning that none of the messages are valid either globally or locally)
      //then we want to handle the ack ourselves.
      if (ackTuple || numWritten == 0) {
        collector.ack(tuple);
      }

    } catch (Throwable ex) {
      handleError(originalMessage, tuple, ex, collector);
    }
  }

  protected void handleError(byte[] originalMessage, Tuple tuple, Throwable ex, OutputCollector collector) {
    MetronError error = new MetronError()
            .withErrorType(Constants.ErrorType.PARSER_ERROR)
            .withThrowable(ex)
            .withSensorType(sensorToComponentMap.keySet())
            .addRawMessage(originalMessage);
    ErrorUtils.handleError(collector, error);
    collector.ack(tuple);
  }

  private List<FieldValidator> getFailedValidators(JSONObject input, List<FieldValidator> validators) {
    List<FieldValidator> failedValidators = new ArrayList<>();
    for(FieldValidator validator : validators) {
      if(!validator.isValid(input, getConfigurations().getGlobalConfig(), stellarContext)) {
        failedValidators.add(validator);
      }
    }
    return failedValidators;
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream(Constants.ERROR_STREAM, new Fields("message"));
  }
}
