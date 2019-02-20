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
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.metron.common.Constants;
import org.apache.metron.common.bolt.ConfiguredParserBolt;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.error.MetronError;
import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.metron.common.message.MessageGetters;
import org.apache.metron.common.message.metadata.RawMessage;
import org.apache.metron.common.message.metadata.RawMessageUtil;
import org.apache.metron.common.utils.ErrorUtils;
import org.apache.metron.common.utils.MessageUtils;
import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.writer.AckTuplesPolicy;
import org.apache.metron.parsers.ParserRunner;
import org.apache.metron.parsers.ParserRunnerResults;
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

public class ParserBolt extends ConfiguredParserBolt implements Serializable {


  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private OutputCollector collector;
  private ParserRunner<JSONObject> parserRunner;
  private Map<String, WriterHandler> sensorToWriterMap;
  private Map<String, String> topicToSensorMap = new HashMap<>();

  private transient MessageGetStrategy messageGetStrategy;
  private int requestedTickFreqSecs;
  private int maxBatchTimeout;
  private int batchTimeoutDivisor = 1;
  private transient AckTuplesPolicy ackTuplesPolicy;

  public ParserBolt( String zookeeperUrl
                   , ParserRunner parserRunner
                     , Map<String, WriterHandler> sensorToWriterMap
  ) {
    super(zookeeperUrl);
    this.parserRunner = parserRunner;
    this.sensorToWriterMap = sensorToWriterMap;
  }

  /**
   * If this ParserBolt is in a topology where it is daisy-chained with
   * other queuing Writers, then the max amount of time it takes for a tuple
   * to clear the whole topology is the sum of all the batchTimeouts for all the
   * daisy-chained Writers.  In the common case where each Writer is using the max
   * batchTimeout, it is then necessary to divide that batchTimeout by the number of
   * daisy-chained Writers.  There are no examples of daisy-chained batching Writers
   * in the current Metron topologies, but the feature is available as a "fluent"-style
   * mutator if needed.  It would be used in the parser topology builder.
   * Default value, if not otherwise set, is 1.
   *
   * If sensor batchTimeouts are configured for some components, the administrator
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
   */
  public int getBatchTimeoutDivisor() {
    return batchTimeoutDivisor;
  }

  /**
   * Used only for unit testing
   */
  protected void setSensorToWriterMap(Map<String, WriterHandler> sensorToWriterMap) {
    this.sensorToWriterMap = sensorToWriterMap;
  }

  /**
   * Used only for unit testing
   */
  protected Map<String, String> getTopicToSensorMap() {
    return topicToSensorMap;
  }

  /**
   * Used only for unit testing
   */
  protected void setTopicToSensorMap(Map<String, String> topicToSensorMap) {
    this.topicToSensorMap = topicToSensorMap;
  }

  /**
   * Used only for unit testing
   */
  public void setMessageGetStrategy(MessageGetStrategy messageGetStrategy) {
    this.messageGetStrategy = messageGetStrategy;
  }

  /**
   * Used only for unit testing
   */
  public void setOutputCollector(OutputCollector collector) {
    this.collector = collector;
  }

  /**
   * Used only for unit testing
   */
  public void setAckTuplesPolicy(AckTuplesPolicy ackTuplesPolicy) {
    this.ackTuplesPolicy = ackTuplesPolicy;
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
    WriterHandler writer = sensorToWriterMap.entrySet().iterator().next().getValue();
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
    //And while we've got BatchTimeoutHelper handy, capture the maxBatchTimeout for writerComponent.
    this.maxBatchTimeout = timeoutHelper.getMaxBatchTimeout();

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
    this.parserRunner.init(this::getConfigurations, initializeStellar());

    ackTuplesPolicy = new AckTuplesPolicy(collector, messageGetStrategy);

    // Need to prep all sensors
    for (Map.Entry<String, WriterHandler> entry: sensorToWriterMap.entrySet()) {
      String sensor = entry.getKey();

      SensorParserConfig config = getSensorParserConfig(sensor);
      if (config != null) {
        config.init();
        topicToSensorMap.put(config.getSensorTopic(), sensor);
      } else {
        throw new IllegalStateException(
            "Unable to retrieve a parser config for " + sensor);
      }

      WriterHandler writer = sensorToWriterMap.get(sensor);
      if (maxBatchTimeout == 0) {
        //This means getComponentConfiguration was never called to initialize maxBatchTimeout,
        //probably because we are in a unit test scenario.  So calculate it here.
        WriterConfiguration writerConfig = getConfigurationStrategy()
                .createWriterConfig(writer.getBulkMessageWriter(), getConfigurations());
        BatchTimeoutHelper timeoutHelper = new BatchTimeoutHelper(
                writerConfig::getAllConfiguredTimeouts, batchTimeoutDivisor);
        maxBatchTimeout = timeoutHelper.getMaxBatchTimeout();
      }

      writer.init(stormConf, context, collector, getConfigurations(), ackTuplesPolicy, maxBatchTimeout);
    }
  }


  @SuppressWarnings("unchecked")
  @Override
  public void execute(Tuple tuple) {
    if (TupleUtils.isTick(tuple)) {
      handleTickTuple(tuple);
      return;
    }
    byte[] originalMessage = (byte[]) messageGetStrategy.get(tuple);
    String topic = tuple.getStringByField(FieldsConfiguration.TOPIC.getFieldName());
    String sensorType = topicToSensorMap.get(topic);
    try {
      ParserConfigurations parserConfigurations = getConfigurations();
      SensorParserConfig sensorParserConfig = parserConfigurations.getSensorParserConfig(sensorType);
      RawMessage rawMessage = RawMessageUtil.INSTANCE.getRawMessage( sensorParserConfig.getRawMessageStrategy()
              , tuple
              , originalMessage
              , sensorParserConfig.getReadMetadata()
              , sensorParserConfig.getRawMessageStrategyConfig()
      );
      ParserRunnerResults<JSONObject> parserRunnerResults = parserRunner.execute(sensorType, rawMessage, parserConfigurations);
      parserRunnerResults.getErrors().forEach(error -> ErrorUtils.handleError(collector, error));

      WriterHandler writer = sensorToWriterMap.get(sensorType);
      int numWritten = 0;
      List<JSONObject> messages = parserRunnerResults.getMessages();
      List<String> messageIds = messages.stream().map(MessageUtils::getGuid).collect(Collectors.toList());
      ackTuplesPolicy.addTupleMessageIds(tuple, messageIds);
      for(int i = 0; i < messages.size(); i++) {
        String messageId = messageIds.get(i);
        JSONObject message = messages.get(i);
        try {
          writer.write(sensorType, new BulkMessage<>(messageId, message), getConfigurations());
          numWritten++;
        } catch (Exception ex) {
          handleError(sensorType, originalMessage, tuple, ex, collector);
        }
      }

      if (numWritten == 0) {
        collector.ack(tuple);
      }

    } catch (Throwable ex) {
      handleError(sensorType, originalMessage, tuple, ex, collector);
      collector.ack(tuple);
    }
  }

  protected Context initializeStellar() {
    Map<String, Object> cacheConfig = new HashMap<>();
    for (String sensorType: this.parserRunner.getSensorTypes()) {
      SensorParserConfig config = getSensorParserConfig(sensorType);

      if (config != null) {
        cacheConfig.putAll(config.getCacheConfig());
      }
    }
    Cache<CachingStellarProcessor.Key, Object> cache = CachingStellarProcessor.createCache(cacheConfig);

    Context.Builder builder = new Context.Builder()
            .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
            .with(Context.Capabilities.GLOBAL_CONFIG, () -> getConfigurations().getGlobalConfig())
            .with(Context.Capabilities.STELLAR_CONFIG, () -> getConfigurations().getGlobalConfig())
            ;
    if(cache != null) {
      builder = builder.with(Context.Capabilities.CACHE, () -> cache);
    }
    Context stellarContext = builder.build();
    StellarFunctions.initialize(stellarContext);
    return stellarContext;
  }

  protected void handleTickTuple(Tuple tuple) {
    try {
      for (Entry<String, WriterHandler> entry : sensorToWriterMap.entrySet()) {
        entry.getValue().flush(getConfigurations(), messageGetStrategy);
      }
    } catch (Exception e) {
      throw new RuntimeException(
              "This should have been caught in the writerHandler.  If you see this, file a JIRA", e);
    } finally {
      collector.ack(tuple);
    }
  }

  protected void handleError(String sensorType, byte[] originalMessage, Tuple tuple, Throwable ex, OutputCollector collector) {
    MetronError error = new MetronError()
            .withErrorType(Constants.ErrorType.PARSER_ERROR)
            .withThrowable(ex)
            .withSensorType(Collections.singleton(sensorType))
            .addRawMessage(originalMessage);
    ErrorUtils.handleError(collector, error);
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream(Constants.ERROR_STREAM, new Fields("message"));
  }

}
