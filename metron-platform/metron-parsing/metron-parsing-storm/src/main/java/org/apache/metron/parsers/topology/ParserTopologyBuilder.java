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
package org.apache.metron.parsers.topology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.apache.curator.framework.CuratorFramework;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.writer.ParserWriterConfiguration;
import org.apache.metron.common.utils.KafkaUtils;
import org.apache.metron.common.utils.ReflectionUtils;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.MessageWriter;
import org.apache.metron.parsers.ParserRunnerImpl;
import org.apache.metron.parsers.bolt.ParserBolt;
import org.apache.metron.parsers.bolt.WriterBolt;
import org.apache.metron.parsers.bolt.WriterHandler;
import org.apache.metron.parsers.topology.config.ValueSupplier;
import org.apache.metron.storm.kafka.flux.SimpleStormKafkaBuilder;
import org.apache.metron.storm.kafka.flux.SpoutConfiguration;
import org.apache.metron.storm.kafka.flux.StormKafkaSpout;
import org.apache.metron.writer.AbstractWriter;
import org.apache.metron.writer.kafka.KafkaWriter;
import org.apache.storm.Config;
import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.topology.BoltDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.json.simple.JSONObject;

/**
 * Builds a Storm topology that parses telemetry data received from a sensor.
 */
public class ParserTopologyBuilder {

  public static class ParserTopology {
    private TopologyBuilder builder;
    private Config topologyConfig;

    private ParserTopology(TopologyBuilder builder, Config topologyConfig) {
      this.builder = builder;
      this.topologyConfig = topologyConfig;
    }


    public TopologyBuilder getBuilder() {
      return builder;
    }

    public Config getTopologyConfig() {
      return topologyConfig;
    }
  }

  /**
   * Builds a Storm topology that parses telemetry data received from an external sensor.
   *
   * @param zookeeperUrl             Zookeeper URL
   * @param brokerUrl                Kafka Broker URL
   * @param sensorTypes               Type of sensor
   * @param spoutParallelismSupplier         Supplier for the parallelism hint for the spout
   * @param spoutNumTasksSupplier            Supplier for the number of tasks for the spout
   * @param parserParallelismSupplier        Supplier for the parallelism hint for the parser bolt
   * @param parserNumTasksSupplier           Supplier for the number of tasks for the parser bolt
   * @param errorWriterParallelismSupplier   Supplier for the parallelism hint for the bolt that handles errors
   * @param errorWriterNumTasksSupplier      Supplier for the number of tasks for the bolt that handles errors
   * @param kafkaSpoutConfigSupplier         Supplier for the configuration options for the kafka spout
   * @param securityProtocolSupplier         Supplier for the security protocol
   * @param outputTopicSupplier              Supplier for the output kafka topic
   * @param stormConfigSupplier              Supplier for the storm config
   * @return A Storm topology that parses telemetry data received from an external sensor
   * @throws Exception
   */
  public static ParserTopology build(String zookeeperUrl,
                                      Optional<String> brokerUrl,
                                      List<String> sensorTypes,
                                      ValueSupplier<List> spoutParallelismSupplier,
                                      ValueSupplier<List> spoutNumTasksSupplier,
                                      ValueSupplier<Integer> parserParallelismSupplier,
                                      ValueSupplier<Integer> parserNumTasksSupplier,
                                      ValueSupplier<Integer> errorWriterParallelismSupplier,
                                      ValueSupplier<Integer> errorWriterNumTasksSupplier,
                                      ValueSupplier<List> kafkaSpoutConfigSupplier,
                                      ValueSupplier<String> securityProtocolSupplier,
                                      ValueSupplier<String> outputTopicSupplier,
                                      ValueSupplier<String> errorTopicSupplier,
                                      ValueSupplier<Config> stormConfigSupplier
  ) throws Exception {

    // fetch configuration from zookeeper
    ParserConfigurations configs = new ParserConfigurations();
    Map<String, SensorParserConfig> sensorToParserConfigs = getSensorParserConfig(zookeeperUrl, sensorTypes, configs);
    Collection<SensorParserConfig> parserConfigs = sensorToParserConfigs.values();

    @SuppressWarnings("unchecked")
    List<Integer> spoutParallelism = (List<Integer>) spoutParallelismSupplier.get(parserConfigs, List.class);
    @SuppressWarnings("unchecked")
    List<Integer> spoutNumTasks = (List<Integer>) spoutNumTasksSupplier.get(parserConfigs, List.class);
    int parserParallelism = parserParallelismSupplier.get(parserConfigs, Integer.class);
    int parserNumTasks = parserNumTasksSupplier.get(parserConfigs, Integer.class);
    int errorWriterParallelism = errorWriterParallelismSupplier.get(parserConfigs, Integer.class);
    int errorWriterNumTasks = errorWriterNumTasksSupplier.get(parserConfigs, Integer.class);
    String outputTopic = outputTopicSupplier.get(parserConfigs, String.class);

    List<Map<String, Object>> kafkaSpoutConfig = kafkaSpoutConfigSupplier.get(parserConfigs, List.class);
    Optional<String> securityProtocol = Optional.ofNullable(securityProtocolSupplier.get(parserConfigs, String.class));

    // create the spout
    TopologyBuilder builder = new TopologyBuilder();
    int i = 0;
    List<String> spoutIds = new ArrayList<>();
    for (Entry<String, SensorParserConfig> entry: sensorToParserConfigs.entrySet()) {
      KafkaSpout kafkaSpout = createKafkaSpout(zookeeperUrl, entry.getKey(), securityProtocol,
          Optional.ofNullable(kafkaSpoutConfig.get(i)), entry.getValue());
      String spoutId = sensorToParserConfigs.size() > 1 ? "kafkaSpout-" + entry.getKey() : "kafkaSpout";
      builder.setSpout(spoutId, kafkaSpout, spoutParallelism.get(i))
          .setNumTasks(spoutNumTasks.get(i));
      spoutIds.add(spoutId);
      ++i;
    }

    // create the parser bolt
    ParserBolt parserBolt = createParserBolt(
        zookeeperUrl,
        brokerUrl,
        sensorToParserConfigs,
        securityProtocol,
        configs,
        Optional.ofNullable(outputTopic)
    );

    BoltDeclarer boltDeclarer = builder
        .setBolt("parserBolt", parserBolt, parserParallelism)
        .setNumTasks(parserNumTasks);

    for (String spoutId : spoutIds) {
      boltDeclarer.localOrShuffleGrouping(spoutId);
    }

    // create the error bolt, if needed
    if (errorWriterNumTasks > 0) {
      String errorTopic = errorTopicSupplier.get(parserConfigs, String.class);
      WriterBolt errorBolt = createErrorBolt(
          zookeeperUrl,
          brokerUrl,
          sensorTypes.get(0),
          securityProtocol,
          configs,
          parserConfigs.iterator().next(),
          errorTopic
      );
      builder.setBolt("errorMessageWriter", errorBolt, errorWriterParallelism)
              .setNumTasks(errorWriterNumTasks)
              .localOrShuffleGrouping("parserBolt", Constants.ERROR_STREAM);
    }

    return new ParserTopology(builder, stormConfigSupplier.get(parserConfigs, Config.class));
  }

  /**
   * Create a spout that consumes tuples from a Kafka topic.
   *
   * @param zkQuorum Zookeeper URL
   * @param sensorType              Type of sensor
   * @param kafkaConfigOptional     Configuration options for the kafka spout
   * @param parserConfig            Configuration for the parser
   * @return
   */
  private static StormKafkaSpout<Object, Object> createKafkaSpout( String zkQuorum
                                                 , String sensorType
                                                 , Optional<String> securityProtocol
                                                 , Optional<Map<String, Object>> kafkaConfigOptional
                                                 , SensorParserConfig parserConfig
                                                 )
  {
    Map<String, Object> kafkaSpoutConfigOptions = kafkaConfigOptional.orElse(new HashMap<>());
    String inputTopic = parserConfig.getSensorTopic() != null ? parserConfig.getSensorTopic() : sensorType;
    kafkaSpoutConfigOptions.putIfAbsent( SpoutConfiguration.FIRST_POLL_OFFSET_STRATEGY.key
            , KafkaSpoutConfig.FirstPollOffsetStrategy.UNCOMMITTED_EARLIEST.name()
    );
    kafkaSpoutConfigOptions.putIfAbsent( ConsumerConfig.GROUP_ID_CONFIG
            , inputTopic + "_parser"
    );
    if(securityProtocol.isPresent()) {
      kafkaSpoutConfigOptions.putIfAbsent("security.protocol", KafkaUtils.INSTANCE.normalizeProtocol(securityProtocol.get()));
    }
    return SimpleStormKafkaBuilder.create( inputTopic
                                         , zkQuorum
                                         , Arrays.asList( SimpleStormKafkaBuilder.FieldsConfiguration.VALUE.getFieldName()
                                                        , SimpleStormKafkaBuilder.FieldsConfiguration.KEY.getFieldName()
                                                        , SimpleStormKafkaBuilder.FieldsConfiguration.TOPIC.getFieldName()
                                                        )
                                         , kafkaSpoutConfigOptions
                                         );
  }

  /**
   * Create a Kafka writer.
   *
   * @param broker An optional URL to the Kafka brokers.
   * @param zkQuorum The URL to Zookeeper.
   * @param securityProtocol An optional security protocol in use.
   * @return
   */
  protected static KafkaWriter createKafkaWriter(Optional<String> broker,
                                               String zkQuorum,
                                               Optional<String> securityProtocol) {
    KafkaWriter writer = new KafkaWriter();

    // cluster URL; either broker or zookeeper
    if(broker.isPresent()) {
      writer.withBrokerUrl(broker.get());

    } else {
      writer.withZkQuorum(zkQuorum);
    }

    // security protocol
    if(securityProtocol.isPresent()) {
      HashMap<String, Object> config = new HashMap<>();
      config.put("security.protocol", securityProtocol.get());
      writer.withProducerConfigs(config);
    }

    return writer;
  }

  /**
   * Create a bolt that parses input from a sensor.
   *
   * @param zookeeperUrl Zookeeper URL
   * @param brokerUrl    Kafka Broker URL
   * @param sensorTypeToParserConfig
   * @param configs
   * @return A Storm bolt that parses input from a sensor
   */
  private static ParserBolt createParserBolt( String zookeeperUrl,
                                              Optional<String> brokerUrl,
                                              Map<String, SensorParserConfig> sensorTypeToParserConfig,
                                              Optional<String> securityProtocol,
                                              ParserConfigurations configs,
                                              Optional<String> outputTopic) {
    Map<String, WriterHandler> writerConfigs = createWriterConfigs(zookeeperUrl,
            brokerUrl,
            sensorTypeToParserConfig,
            securityProtocol,
            configs,
            outputTopic);
    return new ParserBolt(zookeeperUrl, new ParserRunnerImpl(new HashSet<>(sensorTypeToParserConfig.keySet())), writerConfigs);
  }

  protected static Map<String, WriterHandler> createWriterConfigs(String zookeeperUrl,
                                                                  Optional<String> brokerUrl,
                                                                  Map<String, SensorParserConfig> sensorTypeToParserConfig,
                                                                  Optional<String> securityProtocol,
                                                                  ParserConfigurations configs,
                                                                  Optional<String> outputTopic) {
    Map<String, WriterHandler> writerConfigs = new HashMap<>();
    for( Entry<String, SensorParserConfig> entry : sensorTypeToParserConfig.entrySet()) {
      String sensorType = entry.getKey();
      SensorParserConfig parserConfig = entry.getValue();

      // create a writer
      AbstractWriter writer;
      if (parserConfig.getWriterClassName() == null) {
        // if not configured, use a sensible default
        writer = createKafkaWriter(brokerUrl, zookeeperUrl, securityProtocol)
                .withTopic(outputTopic.orElse(
                        parserConfig.getOutputTopic() != null ? parserConfig.getOutputTopic() : Constants.ENRICHMENT_TOPIC));

      } else {
        writer = ReflectionUtils.createInstance(parserConfig.getWriterClassName());
      }

      // configure it
      writer.configure(sensorType, new ParserWriterConfiguration(configs));

      // create a writer handler
      WriterHandler writerHandler = createWriterHandler(writer);
      writerConfigs.put(sensorType, writerHandler);
    }
    return writerConfigs;
  }

  /**
   * Create a bolt that handles error messages.
   *
   * @param zookeeperUrl Kafka zookeeper URL
   * @param brokerUrl Kafka Broker URL
   * @param sensorType Type of sensor that is being consumed.
   * @param securityProtocol Security protocol used (if any)
   * @param configs
   * @param parserConfig The sensor's parser configuration.
   * @return A Storm bolt that handles error messages.
   */
  private static WriterBolt createErrorBolt( String zookeeperUrl,
                                             Optional<String> brokerUrl,
                                             String sensorType,
                                             Optional<String> securityProtocol,
                                             ParserConfigurations configs,
                                             SensorParserConfig parserConfig,
                                             String errorTopic) {

    // create a writer
    AbstractWriter writer;
    if (parserConfig.getErrorWriterClassName() == null) {

      if(errorTopic == null) {
        errorTopic = (String) configs.getGlobalConfig().get(Constants.PARSER_ERROR_TOPIC_GLOBALS_KEY);
      }

      // if not configured, uses a sensible default
      writer = createKafkaWriter(brokerUrl, zookeeperUrl, securityProtocol)
              .withTopic(errorTopic)
              .withConfigPrefix("error");

    } else {
      writer = ReflectionUtils.createInstance(parserConfig.getWriterClassName());
    }

    // configure it
    writer.configure(sensorType, new ParserWriterConfiguration(configs));

    // create a writer handler
    WriterHandler writerHandler = createWriterHandler(writer);

    return new WriterBolt(writerHandler, configs, sensorType)
            .withErrorType(Constants.ErrorType.PARSER_ERROR);
  }

  /**
   * Fetch the parser configuration from Zookeeper.
   *
   * @param zookeeperUrl Zookeeper URL
   * @param sensorTypes Types of sensor
   * @param configs
   * @return
   * @throws Exception
   */
  private static Map<String, SensorParserConfig> getSensorParserConfig(String zookeeperUrl, List<String> sensorTypes, ParserConfigurations configs) throws Exception {
    Map<String, SensorParserConfig> parserConfigs = new HashMap<>();
    try(CuratorFramework client = ConfigurationsUtils.getClient(zookeeperUrl)) {
      client.start();
      ConfigurationsUtils.updateParserConfigsFromZookeeper(configs, client);
      for (String sensorType : sensorTypes) {
        SensorParserConfig parserConfig = configs.getSensorParserConfig(sensorType);
        if (parserConfig == null) {
          throw new IllegalStateException("Cannot find the parser configuration in zookeeper for " + sensorType + "." +
                  "  Please check that it exists in zookeeper by using the 'zk_load_configs.sh -m DUMP' command.");
        }
        parserConfigs.put(sensorType, parserConfig);
      }
    }
    return parserConfigs;
  }

  /**
   * Creates a WriterHandler
   *
   * @param writer The writer.
   * @return A WriterHandler
   */
  private static WriterHandler createWriterHandler(AbstractWriter writer) {

    if (writer instanceof BulkMessageWriter) {
      return new WriterHandler((BulkMessageWriter<JSONObject>) writer);
    } else if (writer instanceof MessageWriter) {
      return new WriterHandler((MessageWriter<JSONObject>) writer);
    } else {
      throw new IllegalStateException("Unable to create parser bolt: writer must be a MessageWriter or a BulkMessageWriter");
    }
  }
}
