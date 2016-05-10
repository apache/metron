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
package org.apache.metron.common.configuration.enrichment;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatIntelConfig;
import org.apache.metron.common.utils.JSONUtils;

import java.io.IOException;

public class SensorEnrichmentConfig {

  private String index;
  private int batchSize;
  private EnrichmentConfig enrichment = new EnrichmentConfig();
  private ThreatIntelConfig threatIntel = new ThreatIntelConfig();

  public EnrichmentConfig getEnrichment() {
    return enrichment;
  }

  public void setEnrichment(EnrichmentConfig enrichment) {
    this.enrichment = enrichment;
  }

  public ThreatIntelConfig getThreatIntel() {
    return threatIntel;
  }

  public void setThreatIntel(ThreatIntelConfig threatIntel) {
    this.threatIntel = threatIntel;
  }

  public String getIndex() {
    return index;
  }

  public void setIndex(String index) {
    this.index = index;
  }


  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  @Override
  public String toString() {
    return "SensorEnrichmentConfig{" +
            "index='" + index + '\'' +
            ", batchSize=" + batchSize +
            ", enrichment=" + enrichment +
            ", threatIntel=" + threatIntel +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SensorEnrichmentConfig that = (SensorEnrichmentConfig) o;

    if (getBatchSize() != that.getBatchSize()) return false;
    if (getIndex() != null ? !getIndex().equals(that.getIndex()) : that.getIndex() != null) return false;
    if (getEnrichment() != null ? !getEnrichment().equals(that.getEnrichment()) : that.getEnrichment() != null)
      return false;
    return getThreatIntel() != null ? getThreatIntel().equals(that.getThreatIntel()) : that.getThreatIntel() == null;

  }

  @Override
  public int hashCode() {
    int result = getIndex() != null ? getIndex().hashCode() : 0;
    result = 31 * result + getBatchSize();
    result = 31 * result + (getEnrichment() != null ? getEnrichment().hashCode() : 0);
    result = 31 * result + (getThreatIntel() != null ? getThreatIntel().hashCode() : 0);
    return result;
  }

  public static SensorEnrichmentConfig fromBytes(byte[] config) throws IOException {
    return JSONUtils.INSTANCE.load(new String(config), SensorEnrichmentConfig.class);
  }

  public String toJSON() throws JsonProcessingException {
    return JSONUtils.INSTANCE.toJSON(this, true);
  }

}
