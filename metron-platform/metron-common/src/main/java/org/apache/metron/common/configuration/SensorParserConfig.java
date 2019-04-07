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
package org.apache.metron.common.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.metron.common.message.metadata.RawMessageStrategy;
import org.apache.metron.common.message.metadata.RawMessageStrategies;
import org.apache.metron.common.utils.JSONUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The configuration object that defines a parser for a given sensor.  Each
 * sensor has its own parser configuration.
 */
public class SensorParserConfig implements Serializable {

  /**
   * The class name of the parser.
   */
  private String parserClassName;

  /**
   * Allows logic to be defined to filter or ignore messages.  Messages that have been
   * filtered will not be parsed.
   *
   * <p>This should be a fully qualified name of a class that implements the
   * org.apache.metron.parsers.interfaces.MessageFilter interface.
   */
  private String filterClassName;

  /**
   * The input topic containing the sensor telemetry to parse.
   */
  private String sensorTopic;

  /**
   * The output topic where the parsed telemetry will be written.
   */
  private String outputTopic;

  /**
   * The error topic where errors are written to.
   */
  private String errorTopic;

  /**
   * The fully qualified name of a class used to write messages
   * to the output topic.
   *
   * <p>A sensible default is provided.
   */
  private String writerClassName;

  /**
   * The fully qualified name of a class used to write messages
   * to the error topic.
   *
   * <p>A sensible default is provided.
   */
  private String errorWriterClassName;

  /**
   * Determines if parser metadata is made available to the parser's field
   * transformations. If true, the parser field transformations can access
   * parser metadata values.
   *
   * <p>The default is dependent upon the raw message strategy used:
   * <ul>
   * <li>The default strategy sets this to false and metadata is not read by default.</li>
   * <li>The envelope strategy sets this to true and metadata is read by default.</li>
   * </ul>
   */
  private Boolean readMetadata = null;

  /**
   * Determines if parser metadata is automatically merged into the message.  If
   * true, parser metadata values will appear as fields within the message.
   *
   * <p>The default is dependent upon the raw message strategy used:
   * <ul>
   * <li>The default strategy sets this to false and metadata is not merged by default.</li>
   * <li>The envelope strategy sets this to true and metadata is merged by default.</li>
   * </ul>
   */
  private Boolean mergeMetadata = null;

  /**
   * The number of workers for the topology.
   *
   * <p>This property can be overridden on the CLI.
   */
  private Integer numWorkers = null;

  /**
   * The number of ackers for the topology.
   *
   * <p>This property can be overridden on the CLI.
   */
  private Integer numAckers= null;

  /**
   * The parallelism of the Kafka spout.
   * If multiple sensors are specified, each sensor will use it's own configured value.
   *
   * <p>This property can be overridden on the CLI.
   */
  private Integer spoutParallelism = 1;

  /**
   * The number of tasks for the Kafka spout.
   * If multiple sensors are specified, each sensor will use it's own configured value.
   *
   * <p>This property can be overridden on the CLI.
   */
  private Integer spoutNumTasks = 1;

  /**
   * The parallelism of the parser bolt.
   * If multiple sensors are defined, the last one's config will win.
   *
   * <p>This property can be overridden on the CLI.
   */
  private Integer parserParallelism = 1;

  /**
   * The number of tasks for the parser bolt.
   * If multiple sensors are defined, the last one's config will win.
   *
   * <p>This property can be overridden on the CLI.
   */
  private Integer parserNumTasks = 1;

  /**
   * The parallelism of the error writer bolt.
   *
   * <p>This property can be overridden on the CLI.
   */
  private Integer errorWriterParallelism = 1;

  /**
   * The number of tasks for the error writer bolt.
   *
   * <p>This property can be overridden on the CLI.
   */
  private Integer errorWriterNumTasks = 1;

  /**
   * Configuration properties passed to the Kafka spout.
   *
   * <p>This property can be overridden on the CLI.
   */
  private Map<String, Object> spoutConfig = new HashMap<>();

  /**
   * The Kafka security protocol.
   * If multiple sensors are defined, any non PLAINTEXT configuration will be used.
   *
   * <p>This property can be overridden on the CLI.  This property can also be overridden by the spout config.
   */
  private String securityProtocol = null;

  /**
   * Configuration properties passed to the storm topology.
   *
   * <p>This property can be overridden on the CLI.
   */
  private Map<String, Object> stormConfig = new HashMap<>();

  /**
   * Configuration for the parser.
   */
  private Map<String, Object> parserConfig = new HashMap<>();

  /**
   * The field transformations applied to the parsed messages. These allow fields
   * of the parsed message to be transformed.
   */
  private List<FieldTransformer> fieldTransformations = new ArrayList<>();

  /**
   * Configures the cache that backs stellar field transformations.
   * If there are multiple sensors, the configs are merged, and the last non-empty config wins.
   *
   * <ul>
   *   <li>stellar.cache.maxSize - The maximum number of elements in the cache.
   *   <li>stellar.cache.maxTimeRetain - The maximum amount of time an element is kept in the cache (in minutes).
   * </ul>
   */
  private Map<String, Object> cacheConfig = new HashMap<>();

  /**
   * Return the raw message supplier.  This is the strategy to use to extract the raw message and metadata from
   * the tuple.
   */
  private RawMessageStrategy rawMessageStrategy = RawMessageStrategies.DEFAULT;

  /**
   * The config for the raw message supplier.
   */
  private Map<String, Object> rawMessageStrategyConfig = new HashMap<>();

  public RawMessageStrategy getRawMessageStrategy() {
    return rawMessageStrategy;
  }

  public void setRawMessageStrategy(String rawMessageSupplierName) {
    this.rawMessageStrategy = RawMessageStrategies.valueOf(rawMessageSupplierName);
  }

  public Map<String, Object> getRawMessageStrategyConfig() {
    return rawMessageStrategyConfig;
  }

  public void setRawMessageStrategyConfig(Map<String, Object> rawMessageStrategyConfig) {
    this.rawMessageStrategyConfig = rawMessageStrategyConfig;
  }

  public Map<String, Object> getCacheConfig() {
    return cacheConfig;
  }

  public void setCacheConfig(Map<String, Object> cacheConfig) {
    this.cacheConfig = cacheConfig;
  }

  public Integer getNumWorkers() {
    return numWorkers;
  }

  public void setNumWorkers(Integer numWorkers) {
    this.numWorkers = numWorkers;
  }

  public Integer getNumAckers() {
    return numAckers;
  }

  public void setNumAckers(Integer numAckers) {
    this.numAckers = numAckers;
  }

  public Integer getSpoutParallelism() {
    return spoutParallelism;
  }

  public void setSpoutParallelism(Integer spoutParallelism) {
    this.spoutParallelism = spoutParallelism;
  }

  public Integer getSpoutNumTasks() {
    return spoutNumTasks;
  }

  public void setSpoutNumTasks(Integer spoutNumTasks) {
    this.spoutNumTasks = spoutNumTasks;
  }

  public Integer getParserParallelism() {
    return parserParallelism;
  }

  public void setParserParallelism(Integer parserParallelism) {
    this.parserParallelism = parserParallelism;
  }

  public Integer getParserNumTasks() {
    return parserNumTasks;
  }

  public void setParserNumTasks(Integer parserNumTasks) {
    this.parserNumTasks = parserNumTasks;
  }

  public Integer getErrorWriterParallelism() {
    return errorWriterParallelism;
  }

  public void setErrorWriterParallelism(Integer errorWriterParallelism) {
    this.errorWriterParallelism = errorWriterParallelism;
  }

  public Integer getErrorWriterNumTasks() {
    return errorWriterNumTasks;
  }

  public void setErrorWriterNumTasks(Integer errorWriterNumTasks) {
    this.errorWriterNumTasks = errorWriterNumTasks;
  }

  public Map<String, Object> getSpoutConfig() {
    return spoutConfig;
  }

  public void setSpoutConfig(Map<String, Object> spoutConfig) {
    this.spoutConfig = spoutConfig;
  }

  public String getSecurityProtocol() {
    return securityProtocol;
  }

  public void setSecurityProtocol(String securityProtocol) {
    this.securityProtocol = securityProtocol;
  }

  public Map<String, Object> getStormConfig() {
    return stormConfig;
  }

  public void setStormConfig(Map<String, Object> stormConfig) {
    this.stormConfig = stormConfig;
  }

  public Boolean getMergeMetadata() {
    return Optional.ofNullable(mergeMetadata).orElse(getRawMessageStrategy().mergeMetadataDefault());
  }

  public void setMergeMetadata(Boolean mergeMetadata) {
    this.mergeMetadata = mergeMetadata;
  }

  public Boolean getReadMetadata() {
    return Optional.ofNullable(readMetadata).orElse(getRawMessageStrategy().readMetadataDefault());
  }

  public void setReadMetadata(Boolean readMetadata) {
    this.readMetadata = readMetadata;
  }

  public String getErrorWriterClassName() {
    return errorWriterClassName;
  }

  public void setErrorWriterClassName(String errorWriterClassName) {
    this.errorWriterClassName = errorWriterClassName;
  }

  public String getWriterClassName() {
    return writerClassName;
  }

  public void setWriterClassName(String classNames) {
    this.writerClassName = classNames;
  }

  public List<FieldTransformer> getFieldTransformations() {
    return fieldTransformations;
  }

  public void setFieldTransformations(List<FieldTransformer> fieldTransformations) {
    this.fieldTransformations = fieldTransformations;
  }

  public String getFilterClassName() {
    return filterClassName;
  }

  public void setFilterClassName(String filterClassName) {
    this.filterClassName = filterClassName;
  }

  public String getParserClassName() {
    return parserClassName;
  }

  public void setParserClassName(String parserClassName) {
    this.parserClassName = parserClassName;
  }

  public String getSensorTopic() {
    return sensorTopic;
  }

  public void setSensorTopic(String sensorTopic) {
    this.sensorTopic = sensorTopic;
  }

  public String getOutputTopic() {
    return outputTopic;
  }

  public void setOutputTopic(String outputTopic) {
    this.outputTopic = outputTopic;
  }

  public String getErrorTopic() {
    return errorTopic;
  }

  public void setErrorTopic(String errorTopic) {
    this.errorTopic = errorTopic;
  }

  public Map<String, Object> getParserConfig() {
    return parserConfig;
  }

  public void setParserConfig(Map<String, Object> parserConfig) {
    this.parserConfig = parserConfig;
  }

  /**
   * Creates a SensorParserConfig from the raw bytes of a Json string.
   *
   * @param config The raw bytes value of the config as a Json string
   * @return SensorParserConfig containing the configuration
   * @throws IOException If the config cannot be loaded
   */
  public static SensorParserConfig fromBytes(byte[] config) throws IOException {
    SensorParserConfig ret = JSONUtils.INSTANCE.load(new String(config), SensorParserConfig.class);
    ret.init();
    return ret;
  }

  /**
   * Init method that retrieves, initializes, and validates field transformations for this config.
   */
  public void init() {
    for(FieldTransformer h : getFieldTransformations()) {
      h.initAndValidate();
    }
  }

  public String toJSON() throws JsonProcessingException {
    return JSONUtils.INSTANCE.toJSON(this, true);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SensorParserConfig that = (SensorParserConfig) o;
    return new EqualsBuilder()
            .append(parserClassName, that.parserClassName)
            .append(filterClassName, that.filterClassName)
            .append(sensorTopic, that.sensorTopic)
            .append(outputTopic, that.outputTopic)
            .append(errorTopic, that.errorTopic)
            .append(writerClassName, that.writerClassName)
            .append(errorWriterClassName, that.errorWriterClassName)
            .append(getReadMetadata(), that.getReadMetadata())
            .append(getMergeMetadata(), that.getMergeMetadata())
            .append(numWorkers, that.numWorkers)
            .append(numAckers, that.numAckers)
            .append(spoutParallelism, that.spoutParallelism)
            .append(spoutNumTasks, that.spoutNumTasks)
            .append(parserParallelism, that.parserParallelism)
            .append(parserNumTasks, that.parserNumTasks)
            .append(errorWriterParallelism, that.errorWriterParallelism)
            .append(errorWriterNumTasks, that.errorWriterNumTasks)
            .append(spoutConfig, that.spoutConfig)
            .append(securityProtocol, that.securityProtocol)
            .append(stormConfig, that.stormConfig)
            .append(cacheConfig, that.cacheConfig)
            .append(parserConfig, that.parserConfig)
            .append(fieldTransformations, that.fieldTransformations)
            .append(rawMessageStrategy, that.rawMessageStrategy)
            .append(rawMessageStrategyConfig, that.rawMessageStrategyConfig)
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
            .append(parserClassName)
            .append(filterClassName)
            .append(sensorTopic)
            .append(outputTopic)
            .append(errorTopic)
            .append(writerClassName)
            .append(errorWriterClassName)
            .append(getReadMetadata())
            .append(getMergeMetadata())
            .append(numWorkers)
            .append(numAckers)
            .append(spoutParallelism)
            .append(spoutNumTasks)
            .append(parserParallelism)
            .append(parserNumTasks)
            .append(errorWriterParallelism)
            .append(errorWriterNumTasks)
            .append(spoutConfig)
            .append(securityProtocol)
            .append(stormConfig)
            .append(cacheConfig)
            .append(parserConfig)
            .append(fieldTransformations)
            .append(rawMessageStrategy)
            .append(rawMessageStrategyConfig)
            .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("parserClassName", parserClassName)
            .append("filterClassName", filterClassName)
            .append("sensorTopic", sensorTopic)
            .append("outputTopic", outputTopic)
            .append("errorTopic", errorTopic)
            .append("writerClassName", writerClassName)
            .append("errorWriterClassName", errorWriterClassName)
            .append("readMetadata", getReadMetadata())
            .append("mergeMetadata", getMergeMetadata())
            .append("numWorkers", numWorkers)
            .append("numAckers", numAckers)
            .append("spoutParallelism", spoutParallelism)
            .append("spoutNumTasks", spoutNumTasks)
            .append("parserParallelism", parserParallelism)
            .append("parserNumTasks", parserNumTasks)
            .append("errorWriterParallelism", errorWriterParallelism)
            .append("errorWriterNumTasks", errorWriterNumTasks)
            .append("spoutConfig", spoutConfig)
            .append("securityProtocol", securityProtocol)
            .append("stormConfig", stormConfig)
            .append("cacheConfig", cacheConfig)
            .append("parserConfig", parserConfig)
            .append("fieldTransformations", fieldTransformations)
            .append("rawMessageStrategy", rawMessageStrategy)
            .append("rawMessageStrategyConfig", rawMessageStrategyConfig)
            .toString();
  }
}
