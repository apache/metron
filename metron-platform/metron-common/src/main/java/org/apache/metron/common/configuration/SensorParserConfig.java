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
import org.apache.metron.common.utils.JSONUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorParserConfig implements Serializable {

  private String parserClassName;
  private String filterClassName;
  private String sensorTopic;
  private String writerClassName;
  private String errorWriterClassName;
  private String invalidWriterClassName;
  private Boolean readMetadata = false;
  private Boolean mergeMetadata = false;
  private Integer numWorkers = null;
  private Integer numAckers= null;
  private Integer spoutParallelism = 1;
  private Integer spoutNumTasks = 1;
  private Integer parserParallelism = 1;
  private Integer parserNumTasks = 1;
  private Integer errorWriterParallelism = 1;
  private Integer errorWriterNumTasks = 1;
  private Map<String, Object> spoutConfig = new HashMap<>();
  private String securityProtocol = null;
  private Map<String, Object> stormConfig = new HashMap<>();

  /**
   * Return the number of workers for the topology.  This property will be used for the parser unless overridden on the CLI.
   * @return
   */
  public Integer getNumWorkers() {
    return numWorkers;
  }

  public void setNumWorkers(Integer numWorkers) {
    this.numWorkers = numWorkers;
  }

  /**
   * Return the number of ackers for the topology.  This property will be used for the parser unless overridden on the CLI.
   * @return
   */
  public Integer getNumAckers() {
    return numAckers;
  }

  public void setNumAckers(Integer numAckers) {
    this.numAckers = numAckers;
  }

  /**
   * Return the spout parallelism.  This property will be used for the parser unless overridden on the CLI.
   * @return
   */
  public Integer getSpoutParallelism() {
    return spoutParallelism;
  }

  public void setSpoutParallelism(Integer spoutParallelism) {
    this.spoutParallelism = spoutParallelism;
  }

  /**
   * Return the spout num tasks.  This property will be used for the parser unless overridden on the CLI.
   * @return
   */
  public Integer getSpoutNumTasks() {
    return spoutNumTasks;
  }

  public void setSpoutNumTasks(Integer spoutNumTasks) {
    this.spoutNumTasks = spoutNumTasks;
  }

  /**
   * Return the parser parallelism.  This property will be used for the parser unless overridden on the CLI.
   * @return
   */
  public Integer getParserParallelism() {
    return parserParallelism;
  }

  public void setParserParallelism(Integer parserParallelism) {
    this.parserParallelism = parserParallelism;
  }

  /**
   * Return the parser number of tasks.  This property will be used for the parser unless overridden on the CLI.
   * @return
   */
  public Integer getParserNumTasks() {
    return parserNumTasks;
  }

  public void setParserNumTasks(Integer parserNumTasks) {
    this.parserNumTasks = parserNumTasks;
  }

  /**
   * Return the error writer bolt parallelism.  This property will be used for the parser unless overridden on the CLI.
   * @return
   */
  public Integer getErrorWriterParallelism() {
    return errorWriterParallelism;
  }

  public void setErrorWriterParallelism(Integer errorWriterParallelism) {
    this.errorWriterParallelism = errorWriterParallelism;
  }

  /**
   * Return the error writer bolt number of tasks.  This property will be used for the parser unless overridden on the CLI.
   * @return
   */
  public Integer getErrorWriterNumTasks() {
    return errorWriterNumTasks;
  }

  public void setErrorWriterNumTasks(Integer errorWriterNumTasks) {
    this.errorWriterNumTasks = errorWriterNumTasks;
  }

  /**
   * Return the spout config.  This includes kafka properties.  This property will be used for the parser unless overridden on the CLI.
   * @return
   */
  public Map<String, Object> getSpoutConfig() {
    return spoutConfig;
  }

  public void setSpoutConfig(Map<String, Object> spoutConfig) {
    this.spoutConfig = spoutConfig;
  }

  /**
   * Return security protocol to use.  This property will be used for the parser unless overridden on the CLI.
   * The order of precedence is CLI > spout config > config in the sensor parser config.
   * @return
   */
  public String getSecurityProtocol() {
    return securityProtocol;
  }

  public void setSecurityProtocol(String securityProtocol) {
    this.securityProtocol = securityProtocol;
  }

  /**
   * Return Storm topologyconfig.  This property will be used for the parser unless overridden on the CLI.
   * @return
   */
  public Map<String, Object> getStormConfig() {
    return stormConfig;
  }

  public void setStormConfig(Map<String, Object> stormConfig) {
    this.stormConfig = stormConfig;
  }

  /**
   * Return whether or not to merge metadata sent into the message.  If true, then metadata become proper fields.
   * @return
   */
  public Boolean getMergeMetadata() {
    return mergeMetadata;
  }

  public void setMergeMetadata(Boolean mergeMetadata) {
    this.mergeMetadata = mergeMetadata;
  }

  /**
   * Return whether or not to read metadata at all.
   * @return
   */
  public Boolean getReadMetadata() {
    return readMetadata;
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

  public String getInvalidWriterClassName() {
    return invalidWriterClassName;
  }

  public void setInvalidWriterClassName(String invalidWriterClassName) {
    this.invalidWriterClassName = invalidWriterClassName;
  }

  public String getWriterClassName() {
    return writerClassName;
  }
  public void setWriterClassName(String classNames) {
    this.writerClassName = classNames;
  }
  private Map<String, Object> parserConfig = new HashMap<>();
  private List<FieldTransformer> fieldTransformations = new ArrayList<>();

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

  public Map<String, Object> getParserConfig() {
    return parserConfig;
  }

  public void setParserConfig(Map<String, Object> parserConfig) {
    this.parserConfig = parserConfig;
  }

  public static SensorParserConfig fromBytes(byte[] config) throws IOException {
    SensorParserConfig ret = JSONUtils.INSTANCE.load(new String(config), SensorParserConfig.class);
    ret.init();
    return ret;
  }

  public void init() {
    for(FieldTransformer h : getFieldTransformations()) {
      h.initAndValidate();
    }
  }


  public String toJSON() throws JsonProcessingException {
    return JSONUtils.INSTANCE.toJSON(this, true);
  }

  @Override
  public String toString() {
    return "SensorParserConfig{" +
            "parserClassName='" + parserClassName + '\'' +
            ", filterClassName='" + filterClassName + '\'' +
            ", sensorTopic='" + sensorTopic + '\'' +
            ", writerClassName='" + writerClassName + '\'' +
            ", errorWriterClassName='" + errorWriterClassName + '\'' +
            ", invalidWriterClassName='" + invalidWriterClassName + '\'' +
            ", readMetadata=" + readMetadata +
            ", mergeMetadata=" + mergeMetadata +
            ", numWorkers=" + numWorkers +
            ", numAckers=" + numAckers +
            ", spoutParallelism=" + spoutParallelism +
            ", spoutNumTasks=" + spoutNumTasks +
            ", parserParallelism=" + parserParallelism +
            ", parserNumTasks=" + parserNumTasks +
            ", errorWriterParallelism=" + errorWriterParallelism +
            ", errorWriterNumTasks=" + errorWriterNumTasks +
            ", spoutConfig=" + spoutConfig +
            ", securityProtocol='" + securityProtocol + '\'' +
            ", stormConfig=" + stormConfig +
            ", parserConfig=" + parserConfig +
            ", fieldTransformations=" + fieldTransformations +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SensorParserConfig that = (SensorParserConfig) o;

    if (getParserClassName() != null ? !getParserClassName().equals(that.getParserClassName()) : that.getParserClassName() != null)
      return false;
    if (getFilterClassName() != null ? !getFilterClassName().equals(that.getFilterClassName()) : that.getFilterClassName() != null)
      return false;
    if (getSensorTopic() != null ? !getSensorTopic().equals(that.getSensorTopic()) : that.getSensorTopic() != null)
      return false;
    if (getWriterClassName() != null ? !getWriterClassName().equals(that.getWriterClassName()) : that.getWriterClassName() != null)
      return false;
    if (getErrorWriterClassName() != null ? !getErrorWriterClassName().equals(that.getErrorWriterClassName()) : that.getErrorWriterClassName() != null)
      return false;
    if (getInvalidWriterClassName() != null ? !getInvalidWriterClassName().equals(that.getInvalidWriterClassName()) : that.getInvalidWriterClassName() != null)
      return false;
    if (getReadMetadata() != null ? !getReadMetadata().equals(that.getReadMetadata()) : that.getReadMetadata() != null)
      return false;
    if (getMergeMetadata() != null ? !getMergeMetadata().equals(that.getMergeMetadata()) : that.getMergeMetadata() != null)
      return false;
    if (getNumWorkers() != null ? !getNumWorkers().equals(that.getNumWorkers()) : that.getNumWorkers() != null)
      return false;
    if (getNumAckers() != null ? !getNumAckers().equals(that.getNumAckers()) : that.getNumAckers() != null)
      return false;
    if (getSpoutParallelism() != null ? !getSpoutParallelism().equals(that.getSpoutParallelism()) : that.getSpoutParallelism() != null)
      return false;
    if (getSpoutNumTasks() != null ? !getSpoutNumTasks().equals(that.getSpoutNumTasks()) : that.getSpoutNumTasks() != null)
      return false;
    if (getParserParallelism() != null ? !getParserParallelism().equals(that.getParserParallelism()) : that.getParserParallelism() != null)
      return false;
    if (getParserNumTasks() != null ? !getParserNumTasks().equals(that.getParserNumTasks()) : that.getParserNumTasks() != null)
      return false;
    if (getErrorWriterParallelism() != null ? !getErrorWriterParallelism().equals(that.getErrorWriterParallelism()) : that.getErrorWriterParallelism() != null)
      return false;
    if (getErrorWriterNumTasks() != null ? !getErrorWriterNumTasks().equals(that.getErrorWriterNumTasks()) : that.getErrorWriterNumTasks() != null)
      return false;
    if (getSpoutConfig() != null ? !getSpoutConfig().equals(that.getSpoutConfig()) : that.getSpoutConfig() != null)
      return false;
    if (getSecurityProtocol() != null ? !getSecurityProtocol().equals(that.getSecurityProtocol()) : that.getSecurityProtocol() != null)
      return false;
    if (getStormConfig() != null ? !getStormConfig().equals(that.getStormConfig()) : that.getStormConfig() != null)
      return false;
    if (getParserConfig() != null ? !getParserConfig().equals(that.getParserConfig()) : that.getParserConfig() != null)
      return false;
    return getFieldTransformations() != null ? getFieldTransformations().equals(that.getFieldTransformations()) : that.getFieldTransformations() == null;

  }

  @Override
  public int hashCode() {
    int result = getParserClassName() != null ? getParserClassName().hashCode() : 0;
    result = 31 * result + (getFilterClassName() != null ? getFilterClassName().hashCode() : 0);
    result = 31 * result + (getSensorTopic() != null ? getSensorTopic().hashCode() : 0);
    result = 31 * result + (getWriterClassName() != null ? getWriterClassName().hashCode() : 0);
    result = 31 * result + (getErrorWriterClassName() != null ? getErrorWriterClassName().hashCode() : 0);
    result = 31 * result + (getInvalidWriterClassName() != null ? getInvalidWriterClassName().hashCode() : 0);
    result = 31 * result + (getReadMetadata() != null ? getReadMetadata().hashCode() : 0);
    result = 31 * result + (getMergeMetadata() != null ? getMergeMetadata().hashCode() : 0);
    result = 31 * result + (getNumWorkers() != null ? getNumWorkers().hashCode() : 0);
    result = 31 * result + (getNumAckers() != null ? getNumAckers().hashCode() : 0);
    result = 31 * result + (getSpoutParallelism() != null ? getSpoutParallelism().hashCode() : 0);
    result = 31 * result + (getSpoutNumTasks() != null ? getSpoutNumTasks().hashCode() : 0);
    result = 31 * result + (getParserParallelism() != null ? getParserParallelism().hashCode() : 0);
    result = 31 * result + (getParserNumTasks() != null ? getParserNumTasks().hashCode() : 0);
    result = 31 * result + (getErrorWriterParallelism() != null ? getErrorWriterParallelism().hashCode() : 0);
    result = 31 * result + (getErrorWriterNumTasks() != null ? getErrorWriterNumTasks().hashCode() : 0);
    result = 31 * result + (getSpoutConfig() != null ? getSpoutConfig().hashCode() : 0);
    result = 31 * result + (getSecurityProtocol() != null ? getSecurityProtocol().hashCode() : 0);
    result = 31 * result + (getStormConfig() != null ? getStormConfig().hashCode() : 0);
    result = 31 * result + (getParserConfig() != null ? getParserConfig().hashCode() : 0);
    result = 31 * result + (getFieldTransformations() != null ? getFieldTransformations().hashCode() : 0);
    return result;
  }
}
