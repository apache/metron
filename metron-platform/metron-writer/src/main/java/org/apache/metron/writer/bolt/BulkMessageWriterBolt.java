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
package org.apache.metron.writer.bolt;

import org.apache.metron.common.Constants;
import org.apache.metron.common.bolt.ConfiguredIndexingBolt;
import org.apache.metron.common.configuration.writer.IndexingWriterConfiguration;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.metron.common.message.MessageGetters;
import org.apache.metron.common.system.Clock;
import org.apache.metron.common.utils.MessageUtils;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.MessageWriter;
import org.apache.metron.writer.BulkWriterComponent;
import org.apache.metron.writer.WriterToBulkWriter;
import org.apache.storm.Config;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.apache.storm.utils.TupleUtils.isTick;

public class BulkMessageWriterBolt extends ConfiguredIndexingBolt {

  private static final Logger LOG = LoggerFactory
          .getLogger(BulkMessageWriterBolt.class);
  private BulkMessageWriter<JSONObject> bulkMessageWriter;
  private BulkWriterComponent<JSONObject> writerComponent = null;
  private String messageGetStrategyType = MessageGetters.DEFAULT_JSON_FROM_FIELD.name();
  private String messageGetField;
  private transient MessageGetStrategy messageGetStrategy;
  private transient OutputCollector collector;
  private transient Function<WriterConfiguration, WriterConfiguration> configurationTransformation = null;
  private int requestedTickFreqSecs;
  private int defaultBatchTimeout;
  private int batchTimeoutDivisor = 1;

  public BulkMessageWriterBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  public BulkMessageWriterBolt withBulkMessageWriter(BulkMessageWriter<JSONObject > bulkMessageWriter) {
    this.bulkMessageWriter = bulkMessageWriter;
    return this;
  }

  public BulkMessageWriterBolt withMessageWriter(MessageWriter<JSONObject> messageWriter) {
    this.bulkMessageWriter = new WriterToBulkWriter<>(messageWriter);
    return this;
  }

  public BulkMessageWriterBolt withMessageGetter(String messageGetStrategyType) {
    this.messageGetStrategyType = messageGetStrategyType;
    return this;
  }

  public BulkMessageWriterBolt withMessageGetterField(String messageGetField) {
    this.messageGetField = messageGetField;
    return this;
  }

  /**
   * If this BulkMessageWriterBolt is in a topology where it is daisy-chained with
   * other queuing Writers, then the max amount of time it takes for a tuple
   * to clear the whole topology is the sum of all the batchTimeouts for all the
   * daisy-chained Writers.  In the common case where each Writer is using the default
   * batchTimeout, it is then necessary to divide that batchTimeout by the number of
   * daisy-chained Writers.  There are no examples of daisy-chained batching Writers
   * in the current Metron topologies, but the feature is available as a "fluent"-style
   * mutator if needed.  It would be used in the topology set-up files such as
   * metron-platform/metron-indexing/src/main/flux/indexing/remote.yaml
   * Default value, if not otherwise set, is 1.
   *
   * If non-default batchTimeouts are configured for some components, the administrator
   * may want to take this behavior into account.
   *
   * @param batchTimeoutDivisor
   * @return
   */
  public BulkMessageWriterBolt withBatchTimeoutDivisor(int batchTimeoutDivisor) {
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

  /**
   * This method is called by TopologyBuilder.createTopology() to obtain topology and
   * bolt specific configuration parameters.  We use it primarily to configure how often
   * a tick tuple will be sent to our bolt.
   * @return
   */
  @Override
  public Map<String, Object> getComponentConfiguration() {
    // This is called long before prepare(), so do some of the same stuff as prepare() does,
    // to get the valid WriterConfiguration.  But don't store any non-serializable objects,
    // else Storm will throw a runtime error.
    Function<WriterConfiguration, WriterConfiguration> configurationXform;
    if(bulkMessageWriter instanceof WriterToBulkWriter) {
      configurationXform = WriterToBulkWriter.TRANSFORMATION;
    }
    else {
      configurationXform = x -> x;
    }
    WriterConfiguration writerconf = configurationXform.apply(
            new IndexingWriterConfiguration(bulkMessageWriter.getName(), getConfigurations()));

    BatchTimeoutHelper timeoutHelper = new BatchTimeoutHelper(writerconf::getAllConfiguredTimeouts, batchTimeoutDivisor);
    this.requestedTickFreqSecs = timeoutHelper.getRecommendedTickInterval();
    //And while we've got BatchTimeoutHelper handy, capture the defaultBatchTimeout for writerComponent.
    this.defaultBatchTimeout = timeoutHelper.getDefaultBatchTimeout();

    Map<String, Object> conf = super.getComponentConfiguration();
    if (conf == null) {
      conf = new HashMap<String, Object>();
    }
    conf.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, requestedTickFreqSecs);
    LOG.info("Requesting " + Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS + " set to " + Integer.toString(requestedTickFreqSecs));
    return conf;
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    this.writerComponent = new BulkWriterComponent<>(collector);
    this.collector = collector;
    super.prepare(stormConf, context, collector);
    if (messageGetField != null) {
      messageGetStrategy = MessageGetters.valueOf(messageGetStrategyType).get(messageGetField);
    } else {
      messageGetStrategy = MessageGetters.valueOf(messageGetStrategyType).get();
    }
    if(bulkMessageWriter instanceof WriterToBulkWriter) {
      configurationTransformation = WriterToBulkWriter.TRANSFORMATION;
    }
    else {
      configurationTransformation = x -> x;
    }
    try {
      WriterConfiguration writerconf = configurationTransformation.apply(
              new IndexingWriterConfiguration(bulkMessageWriter.getName(), getConfigurations()));
      if (defaultBatchTimeout == 0) {
        //This means getComponentConfiguration was never called to initialize defaultBatchTimeout,
        //probably because we are in a unit test scenario.  So calculate it here.
        BatchTimeoutHelper timeoutHelper = new BatchTimeoutHelper(writerconf::getAllConfiguredTimeouts, batchTimeoutDivisor);
        defaultBatchTimeout = timeoutHelper.getDefaultBatchTimeout();
      }
      writerComponent.setDefaultBatchTimeout(defaultBatchTimeout);
      bulkMessageWriter.init(stormConf, context, writerconf);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Used only for unit testing.
   */
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector, Clock clock) {
    prepare(stormConf, context, collector);
    writerComponent.withClock(clock);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void execute(Tuple tuple) {
    if (isTick(tuple)) {
      try {
        if (!(bulkMessageWriter instanceof WriterToBulkWriter)) {
          //WriterToBulkWriter doesn't allow batching, so no need to flush on Tick.
          LOG.debug("Flushing message queues older than their batchTimeouts");
          writerComponent.flushTimeouts(bulkMessageWriter, configurationTransformation.apply(
                  new IndexingWriterConfiguration(bulkMessageWriter.getName(), getConfigurations()))
                  , messageGetStrategy);
        }
      }
      catch(Exception e) {
        throw new RuntimeException("This should have been caught in the writerComponent.  If you see this, file a JIRA", e);
      }
      finally {
        collector.ack(tuple);
      }
      return;
    }

    try
    {
      JSONObject message = (JSONObject) messageGetStrategy.get(tuple);
      String sensorType = MessageUtils.getSensorType(message);
      LOG.trace("Writing enrichment message: {}", message);
      WriterConfiguration writerConfiguration = configurationTransformation.apply(
              new IndexingWriterConfiguration(bulkMessageWriter.getName(), getConfigurations()));
      if(writerConfiguration.isDefault(sensorType)) {
        //want to warn, but not fail the tuple
        collector.reportError(new Exception("WARNING: Default and (likely) unoptimized writer config used for " + bulkMessageWriter.getName() + " writer and sensor " + sensorType));
      }
      writerComponent.write(sensorType
                           , tuple
                           , message
                           , bulkMessageWriter
                           , writerConfiguration
                           , messageGetStrategy
                           );
    }
    catch(Exception e) {
      throw new RuntimeException("This should have been caught in the writerComponent.  If you see this, file a JIRA", e);
    }
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream(Constants.ERROR_STREAM, new Fields("message"));
  }
}
