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

public class MetaAlertConfig {
  private String metaAlertIndex;
  private String threatTriageField;
  private String threatSort;
  private String sourceTypeField;

  /**
   * Simple object for storing and retrieving configs, primarily to make passing all the info to
   * the sub DAOs easier.
   * @param metaAlertIndex The metaalert index or collection we're using
   * @param threatTriageField The threat triage field's name
   * @param threatSort The sorting operation on the threat triage field
   * @param sourceTypeField The source type field
   */
  public MetaAlertConfig(String metaAlertIndex, String threatTriageField,
      String threatSort, String sourceTypeField) {
    this.metaAlertIndex = metaAlertIndex;
    this.threatTriageField = threatTriageField;
    this.threatSort = threatSort;
    this.sourceTypeField = sourceTypeField;
  }

  public String getMetaAlertIndex() {
    return metaAlertIndex;
  }

  public void setMetaAlertIndex(String metaAlertIndex) {
    this.metaAlertIndex = metaAlertIndex;
  }

  public String getThreatTriageField() {
    return threatTriageField;
  }

  public void setThreatTriageField(String threatTriageField) {
    this.threatTriageField = threatTriageField;
  }

  public String getThreatSort() {
    return threatSort;
  }

  public void setThreatSort(String threatSort) {
    this.threatSort = threatSort;
  }

  public String getSourceTypeField() {
    return sourceTypeField;
  }

  public void setSourceTypeField(String sourceTypeField) {
    this.sourceTypeField = sourceTypeField;
  }
}
