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

package org.apache.metron.common.configuration.enrichment.threatintel;

import org.apache.metron.common.configuration.enrichment.EnrichmentConfig;

public class ThreatIntelConfig extends EnrichmentConfig {
  private ThreatTriageConfig triageConfig = new ThreatTriageConfig();

  public ThreatTriageConfig getTriageConfig() {
    return triageConfig;
  }

  public void setTriageConfig(ThreatTriageConfig triageConfig) {
    this.triageConfig = triageConfig;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    ThreatIntelConfig that = (ThreatIntelConfig) o;

    return getTriageConfig() != null ? getTriageConfig().equals(that.getTriageConfig()) : that.getTriageConfig() == null;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (getTriageConfig() != null ? getTriageConfig().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ThreatIntelConfig{" +
            "triageConfig=" + triageConfig +
            '}';
  }
}
