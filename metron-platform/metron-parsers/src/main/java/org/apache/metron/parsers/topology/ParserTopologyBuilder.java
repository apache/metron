/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.parsers.topology;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.metron.parsers.topology.config.ValueSupplier;
import org.apache.metron.storm.kafka.flux.SimpleStormKafkaBuilder;
import org.apache.metron.storm.kafka.flux.SpoutConfiguration;
import org.apache.metron.storm.kafka.flux.StormKafkaSpout;
import org.apache.storm.Config;
import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.writer.ParserWriterConfiguration;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.MessageWriter;
import org.apache.metron.common.utils.ReflectionUtils;
import org.apache.metron.parsers.bolt.ParserBolt;
import org.apache.metron.parsers.bolt.WriterBolt;
import org.apache.metron.parsers.bolt.WriterHandler;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.writer.AbstractWriter;
import org.apache.metron.writer.kafka.KafkaWriter;
import org.json.simple.JSONObject;

import java.util.*;

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
   * @param sensorType               Type of sensor
   * @param spoutParallelismSupplier         Supplier for the parallelism hint for the spout
   * @param spoutNumTasksSupplier            Supplier for the number of tasks for the spout
   * @param parserParallelismSupplier        Supplier for the parallelism hint for the parser bolt
   * @param parserNumTasksSupplier           Supplier for the number of tasks for the parser bolt
   * @param errorWriterParallelismSupplier   Supplier for the parallelism hint for the bolt that handles errors
   * @param errorWriterNumTasksSupplier      Supplier for the number of tasks for the bolt that handles errors
   * @param kafkaSpoutConfigSupplier         Supplier for the configuration options for the kafka spout
   * @param securityProtocolSupplier         Supplier for the security protocol
   * @param outputTopic                      The output kafka topic
   * @param stormConfigSupplier              Supplier for the storm config
   * @return A Storm topology that parses telemetry data received from an external sensor
   * @throws Exception
   */
  public static ParserTopology build(String zookeeperUrl,
                                      Optional<String> brokerUrl,
                                      String sensorType,
                                      ValueSupplier<Integer> spoutParallelismSupplier,
                                      ValueSupplier<Integer> spoutNumTasksSupplier,
                                      ValueSupplier<Integer> parserParallelismSupplier,
                                      ValueSupplier<Integer> parserNumTasksSupplier,
                                      ValueSupplier<Integer> errorWriterParallelismSupplier,
                                      ValueSupplier<Integer> errorWriterNumTasksSupplier,
                                      ValueSupplier<Map> kafkaSpoutConfigSupplier,
                                      ValueSupplier<String> securityProtocolSupplier,
                                      Optional<String> outputTopic,
                                      ValueSupplier<Config> stormConfigSupplier
  ) throws Exception {

    // fetch configuration from zookeeper
    ParserConfigurations configs = new ParserConfigurations();
    SensorParserConfig parserConfig = getSensorParserConfig(zookeeperUrl, sensorType, configs);
    int spoutParallelism = spoutParallelismSupplier.get(parserConfig, Integer.class);
    int spoutNumTasks = spoutNumTasksSupplier.get(parserConfig, Integer.class);
    int parserParallelism = parserParallelismSupplier.get(parserConfig, Integer.class);
    int parserNumTasks = parserNumTasksSupplier.get(parserConfig, Integer.class);
    int errorWriterParallelism = errorWriterParallelismSupplier.get(parserConfig, Integer.class);
    int errorWriterNumTasks = errorWriterNumTasksSupplier.get(parserConfig, Integer.class);
    Map<String, Object> kafkaSpoutConfig = kafkaSpoutConfigSupplier.get(parserConfig, Map.class);
    Optional<String> securityProtocol = Optional.ofNullable(securityProtocolSupplier.get(parserConfig, String.class));

    // create the spout
    TopologyBuilder builder = new TopologyBuilder();
    KafkaSpout kafkaSpout = createKafkaSpout(zookeeperUrl, sensorType, securityProtocol, Optional.ofNullable(kafkaSpoutConfig) , parserConfig);
    builder.setSpout("kafkaSpout", kafkaSpout, spoutParallelism)
            .setNumTasks(spoutNumTasks);

    // create the parser bolt
    ParserBolt parserBolt = createParserBolt(zookeeperUrl, brokerUrl, sensorType, securityProtocol, configs, parserConfig, outputTopic);
    builder.setBolt("parserBolt", parserBolt, parserParallelism)
            .setNumTasks(parserNumTasks)
            .shuffleGrouping("kafkaSpout");

    // create the error bolt, if needed
    if (errorWriterNumTasks > 0) {
      WriterBolt errorBolt = createErrorBolt(zookeeperUrl, brokerUrl, sensorType, securityProtocol, configs, parserConfig);
      builder.setBolt("errorMessageWriter", errorBolt, errorWriterParallelism)
              .setNumTasks(errorWriterNumTasks)
              .shuffleGrouping("parserBolt", Constants.ERROR_STREAM);
    }

    return new ParserTopology(builder, stormConfigSupplier.get(parserConfig, Config.class));
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
            , KafkaSpoutConfig.FirstPollOffsetStrategy.UNCOMMITTED_EARLIEST.toString()
    );
    kafkaSpoutConfigOptions.putIfAbsent( ConsumerConfig.GROUP_ID_CONFIG
            , inputTopic + "_parser"
    );
    if(securityProtocol.isPresent()) {
      kafkaSpoutConfigOptions.putIfAbsent("security.protocol", securityProtocol.get());
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

  private static KafkaWriter createKafkaWriter( Optional<String> broker
                                              , String zkQuorum
                                              , Optional<String> securityProtocol
                                              )
  {
    KafkaWriter ret = null;
    if(broker.isPresent()) {
      ret = new KafkaWriter(broker.get());
    }
    else {
      ret = new KafkaWriter().withZkQuorum(zkQuorum);
    }
    if(securityProtocol.isPresent()) {
      HashMap<String, Object> config = new HashMap<>();
      config.put("security.protocol", securityProtocol.get());
      ret.withProducerConfigs(config);
    }
    return ret;
  }

  /**
   * Create a bolt that parses input from a sensor.
   *
   * @param zookeeperUrl Zookeeper URL
   * @param brokerUrl    Kafka Broker URL
   * @param sensorType   Type of sensor that is being consumed.
   * @param configs
   * @param parserConfig
   * @return A Storm bolt that parses input from a sensor
   */
  private static ParserBolt createParserBolt( String zookeeperUrl
                                            , Optional<String> brokerUrl
                                            , String sensorType
                                            , Optional<String> securityProtocol
                                            , ParserConfigurations configs
                                            , SensorParserConfig parserConfig
                                            , Optional<String> outputTopic
                                            )
  {

    // create message parser
    MessageParser<JSONObject> parser = ReflectionUtils.createInstance(parserConfig.getParserClassName());
    parser.configure(parserConfig.getParserConfig());

    // create writer - if not configured uses a sensible default
    AbstractWriter writer = parserConfig.getWriterClassName() == null ?
            createKafkaWriter( brokerUrl
                             , zookeeperUrl
                             , securityProtocol
                             ).withTopic(outputTopic.orElse(Constants.ENRICHMENT_TOPIC)) :
            ReflectionUtils.createInstance(parserConfig.getWriterClassName());
    writer.configure(sensorType, new ParserWriterConfiguration(configs));

    // create a writer handler
    WriterHandler writerHandler = createWriterHandler(writer);

    return new ParserBolt(zookeeperUrl, sensorType, parser, writerHandler);
  }

  /**
   * Create a bolt that handles error messages.
   *
   * @param zookeeperUrl    Kafka zookeeper URL
   * @param brokerUrl    Kafka Broker URL
   * @param sensorType   Type of sensor that is being consumed.
   * @param securityProtocol   Security protocol used (if any)
   * @param configs
   * @param parserConfig
   * @return A Storm bolt that handles error messages.
   */
  private static WriterBolt createErrorBolt( String zookeeperUrl
                                           , Optional<String> brokerUrl
                                           , String sensorType
                                           , Optional<String> securityProtocol
                                           , ParserConfigurations configs
                                           , SensorParserConfig parserConfig
                                           )
  {

    // create writer - if not configured uses a sensible default
    AbstractWriter writer = parserConfig.getErrorWriterClassName() == null ?
              createKafkaWriter( brokerUrl
                               , zookeeperUrl
                               , securityProtocol
                               ).withTopic((String) configs.getGlobalConfig().get("parser.error.topic"))
                                .withConfigPrefix("error")
            : ReflectionUtils.createInstance(parserConfig.getWriterClassName());
    writer.configure(sensorType, new ParserWriterConfiguration(configs));

    // create a writer handler
    WriterHandler writerHandler = createWriterHandler(writer);

    return new WriterBolt(writerHandler, configs, sensorType).withErrorType(Constants.ErrorType.PARSER_ERROR);
  }

  /**
   * Fetch the parser configuration from Zookeeper.
   *
   * @param zookeeperUrl Zookeeper URL
   * @param sensorType   Type of sensor
   * @param configs
   * @return
   * @throws Exception
   */
  private static SensorParserConfig getSensorParserConfig(String zookeeperUrl, String sensorType, ParserConfigurations configs) throws Exception {
    try(CuratorFramework client = ConfigurationsUtils.getClient(zookeeperUrl)) {
      client.start();
      ConfigurationsUtils.updateParserConfigsFromZookeeper(configs, client);
      SensorParserConfig parserConfig = configs.getSensorParserConfig(sensorType);
      if (parserConfig == null) {
        throw new IllegalStateException("Cannot find the parser configuration in zookeeper for " + sensorType + "." +
                "  Please check that it exists in zookeeper by using the 'zk_load_configs.sh -m DUMP' command.");
      }
      return parserConfig;
    }
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
