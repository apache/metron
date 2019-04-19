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

package org.apache.metron.indexing.dao.metaalert;

import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationsUtils;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class MetaAlertConfig {
  private String metaAlertIndex;
  private String threatSort;
  private Supplier<Map<String, Object>> globalConfigSupplier;

  /**
   * Simple object for storing and retrieving configs, primarily to make passing all the info to
   * the sub DAOs easier.
   * @param metaAlertIndex The metaalert index or collection we're using
   * @param threatSort The sorting operation on the threat triage field
   */
  public MetaAlertConfig( String metaAlertIndex
                        , String threatSort
                        , Supplier<Map<String, Object>> globalConfigSupplier) {
    this.metaAlertIndex = metaAlertIndex;
    this.threatSort = threatSort;
    this.globalConfigSupplier = globalConfigSupplier;
  }

  public String getMetaAlertIndex() {
    return metaAlertIndex;
  }

  public void setMetaAlertIndex(String metaAlertIndex) {
    this.metaAlertIndex = metaAlertIndex;
  }

  public String getThreatTriageField() {
    Optional<Map<String, Object>> globalConfig = Optional.ofNullable(globalConfigSupplier.get());
    if(!globalConfig.isPresent()) {
      return getDefaultThreatTriageField();
    }
    return ConfigurationsUtils.getFieldName(globalConfig.get(), Constants.THREAT_SCORE_FIELD_PROPERTY, getDefaultThreatTriageField());
  }

  protected abstract String getDefaultThreatTriageField();

  public String getThreatSort() {
    return threatSort;
  }

  public void setThreatSort(String threatSort) {
    this.threatSort = threatSort;
  }

  public String getSourceTypeField() {
    Optional<Map<String, Object>> globalConfig = Optional.ofNullable(globalConfigSupplier.get());
    if(!globalConfig.isPresent()) {
      return getDefaultSourceTypeField();
    }
    return ConfigurationsUtils.getFieldName(globalConfig.get(), Constants.SENSOR_TYPE_FIELD_PROPERTY, getDefaultSourceTypeField());
  }

  protected abstract String getDefaultSourceTypeField();

}
