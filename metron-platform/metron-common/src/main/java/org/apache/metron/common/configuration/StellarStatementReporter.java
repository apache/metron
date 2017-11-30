/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.metron.common.configuration;

import static org.apache.metron.common.configuration.ConfigurationType.ENRICHMENT;
import static org.apache.metron.common.configuration.ConfigurationType.PARSER;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.enrichment.EnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.RiskLevelRule;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig;
import org.apache.metron.common.field.transformation.FieldTransformation;
import org.apache.metron.common.field.transformation.StellarTransformation;
import org.apache.metron.common.utils.StringUtils;
import org.apache.metron.stellar.common.StellarConfiguredStatementReporter;

/**
 * StellarStatementReporter is used to report all of the configured / deployed Stellar statements in
 * the system.
 */
public class StellarStatementReporter implements StellarConfiguredStatementReporter {

  public enum Type {
    ENRICHMENT, THREAT_INTEL;
  }

  public StellarStatementReporter() {
  }

  @Override
  public String getName() {
    return "Apache Metron";
  }

  @Override
  public void vist(CuratorFramework client, StatementReportVisitor visitor,
      ConfigReportErrorConsumer errorConsumer) throws Exception {
    visitParserConfigs(client, visitor, errorConsumer);
    visitEnrichmentConfigs(client, visitor, errorConsumer);
  }

  private void visitParserConfigs(CuratorFramework client, StatementReportVisitor visitor,
      ConfigReportErrorConsumer errorConsumer) throws Exception {
    List<String> children = client.getChildren().forPath(PARSER.getZookeeperRoot());
    for (String child : children) {
      byte[] data = client.getData().forPath(PARSER.getZookeeperRoot() + "/" + child);
      try {
        SensorParserConfig parserConfig = SensorParserConfig.fromBytes(data);
        List<FieldTransformer> transformations = parserConfig.getFieldTransformations();
        transformations.forEach((f) -> {
          if (StellarTransformation.class.isAssignableFrom(f.getFieldTransformation().getClass())) {
            FieldTransformation transformation = f.getFieldTransformation();
            f.getConfig().forEach((k, v) -> {
              List<String> names = Arrays
                  .asList(getName(), PARSER.toString(), parserConfig.getSensorTopic(), k);
              visitor.visit(names, v.toString());
            });
          }
        });
      } catch (Exception e) {
        List<String> names = Arrays.asList(getName(), PARSER.toString(), child);
        errorConsumer.consume(names, e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void visitEnrichmentConfigs(CuratorFramework client, StatementReportVisitor visitor,
      ConfigReportErrorConsumer errorConsumer) throws Exception {
    List<String> children = client.getChildren().forPath(ENRICHMENT.getZookeeperRoot());
    for (String child : children) {
      byte[] data = client.getData().forPath(ENRICHMENT.getZookeeperRoot() + "/" + child);
      try {
        final SensorEnrichmentConfig sensorEnrichmentConfig = SensorEnrichmentConfig
            .fromBytes(data);

        EnrichmentConfig enrichmentConfig = null;
        enrichmentConfig = sensorEnrichmentConfig.getEnrichment();
        visitEnrichementConfig(child, Type.ENRICHMENT, enrichmentConfig, visitor, errorConsumer);
        enrichmentConfig = sensorEnrichmentConfig.getThreatIntel();
        visitEnrichementConfig(child, Type.THREAT_INTEL, enrichmentConfig, visitor, errorConsumer);
        ThreatTriageConfig threatTriageConfig = sensorEnrichmentConfig.getThreatIntel()
            .getTriageConfig();
        visitEnrichmentThreatTriageConfigs(child, threatTriageConfig, visitor, errorConsumer);
      } catch (Exception e) {
        List<String> names = Arrays.asList(getName(), ENRICHMENT.toString(), child);
        errorConsumer.consume(names, e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void visitEnrichementConfig(String topicName, Type type,
      EnrichmentConfig enrichmentConfig, StatementReportVisitor visitor,
      ConfigReportErrorConsumer errorConsumer) throws Exception {

    Map<String, Object> enrichmentStellarMap = (Map<String, Object>) enrichmentConfig.getFieldMap()
        .getOrDefault("stellar", new HashMap<>());
    Map<String, Object> transforms = (Map<String, Object>) enrichmentStellarMap
        .getOrDefault("config", new HashMap<>());
    try {
      for (Map.Entry<String, Object> kv : transforms.entrySet()) {
        // we can have a group or an entry
        if (kv.getValue() instanceof Map) {
          Map<String, String> groupMap = (Map<String, String>) kv.getValue();
          for (Map.Entry<String, String> groupKv : groupMap.entrySet()) {
            List<String> names = Arrays
                .asList(getName(), ENRICHMENT.toString(), topicName, type.toString(), kv.getKey(),
                    groupKv.getKey());
            visitor.visit(names, groupKv.getValue());
          }
        } else {
          List<String> names = Arrays
              .asList(getName(), ENRICHMENT.toString(), topicName, type.toString(), "(default)",
                  kv.getKey(), kv.getKey());
          visitor.visit(names, kv.getValue().toString());
        }
      }
    } catch (Exception e) {
      List<String> names = Arrays
          .asList(getName(), ENRICHMENT.toString(), topicName, type.toString());
      errorConsumer.consume(names, e);
    }
  }

  @SuppressWarnings("unchecked")
  private void visitEnrichmentThreatTriageConfigs(String topicName,
      ThreatTriageConfig threatTriageConfig, StatementReportVisitor visitor,
      ConfigReportErrorConsumer errorConsumer) throws Exception {
    try {
      List<RiskLevelRule> riskLevelRules = threatTriageConfig.getRiskLevelRules();
      riskLevelRules.forEach((r) -> {
        String name = r.getName();
        if (org.apache.commons.lang.StringUtils.isEmpty(name)) {
          name = "(default)";
        }
        List<String> names = Arrays
            .asList(getName(), ENRICHMENT.toString(), topicName, "THREAT_TRIAGE", name);
        visitor.visit(names, r.getRule());
      });

    } catch (Exception e) {
      List<String> names = Arrays
          .asList(getName(), ENRICHMENT.toString(), topicName, "THREAT_TRIAGE");
      errorConsumer.consume(names, e);
    }
  }
}
