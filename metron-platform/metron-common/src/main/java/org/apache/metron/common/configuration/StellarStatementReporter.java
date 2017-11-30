/*
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
import static org.apache.metron.common.configuration.ConfigurationType.PROFILER;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.enrichment.EnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.RiskLevelRule;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.configuration.profiler.ProfileResult;
import org.apache.metron.common.configuration.profiler.ProfileResultExpressions;
import org.apache.metron.common.configuration.profiler.ProfileTriageExpressions;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.common.field.transformation.StellarTransformation;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.stellar.common.StellarConfiguredStatementReporter;
import org.apache.zookeeper.KeeperException.NoNodeException;

/**
 * StellarStatementReporter is used to report all of the configured / deployed Stellar statements in
 * the system.
 */
public class StellarStatementReporter implements StellarConfiguredStatementReporter {

  public enum Type {
    ENRICHMENT, THREAT_INTEL
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
    visitProfilerConfigs(client, visitor, errorConsumer);
  }

  private void visitParserConfigs(CuratorFramework client, StatementReportVisitor visitor,
      ConfigReportErrorConsumer errorConsumer) throws Exception {
    List<String> children = null;

    try {
      children = client.getChildren().forPath(PARSER.getZookeeperRoot());
    } catch (NoNodeException nne) {
      return;
    }

    for (String child : children) {
      byte[] data = client.getData().forPath(PARSER.getZookeeperRoot() + "/" + child);
      try {
        SensorParserConfig parserConfig = SensorParserConfig.fromBytes(data);
        List<FieldTransformer> transformations = parserConfig.getFieldTransformations();
        transformations.forEach((f) -> {
          if (StellarTransformation.class.isAssignableFrom(f.getFieldTransformation().getClass())) {
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
    List<String> children = null;

    try {
      children = client.getChildren().forPath(ENRICHMENT.getZookeeperRoot());
    } catch (NoNodeException nne) {
      return;
    }

    for (String child : children) {
      byte[] data = client.getData().forPath(ENRICHMENT.getZookeeperRoot() + "/" + child);
      try {
        // Certain parts of the SensorEnrichmentConfig do Stellar Verification on their
        // own as part of deserialization, where the bean spec will call the setter, which has
        // been wired with stellar verification calls.
        //
        // In cases where those parts of the config are in fact the parts that have invalid
        // Stellar statements, we will fail during the JSON load before we get to ANY config
        // contained in the SensorEnrichmentConfig.
        //
        // I have left the code to properly check all the configuration parts for completeness
        // on the reporting side ( the report initiator may want to list successful evals), even
        // though they can be executed, then they will never fail.
        final SensorEnrichmentConfig sensorEnrichmentConfig = SensorEnrichmentConfig
            .fromBytes(data);

        EnrichmentConfig enrichmentConfig;
        enrichmentConfig = sensorEnrichmentConfig.getEnrichment();
        visitEnrichmentConfig(child, Type.ENRICHMENT, enrichmentConfig, visitor, errorConsumer);
        enrichmentConfig = sensorEnrichmentConfig.getThreatIntel();
        visitEnrichmentConfig(child, Type.THREAT_INTEL, enrichmentConfig, visitor, errorConsumer);
        final ThreatTriageConfig threatTriageConfig = sensorEnrichmentConfig.getThreatIntel()
            .getTriageConfig();
        visitEnrichmentThreatTriageConfigs(child, threatTriageConfig, visitor, errorConsumer);
      } catch (Exception e) {
        List<String> names = Arrays.asList(getName(), ENRICHMENT.toString(), child);
        errorConsumer.consume(names, e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void visitEnrichmentConfig(String topicName, Type type,
      EnrichmentConfig enrichmentConfig, StatementReportVisitor visitor,
      ConfigReportErrorConsumer errorConsumer) {

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
      ConfigReportErrorConsumer errorConsumer) {
    try {
      List<RiskLevelRule> riskLevelRules = threatTriageConfig.getRiskLevelRules();
      riskLevelRules.forEach((r) -> {
        String name = r.getName();
        if (org.apache.commons.lang.StringUtils.isEmpty(name)) {
          name = "(default)";
        }
        List<String> names = Arrays
            .asList(getName(), ENRICHMENT.toString(), topicName, "THREAT_TRIAGE", name, "rule");
        visitor.visit(names, r.getRule());
        if (!org.apache.commons.lang.StringUtils.isEmpty(r.getReason())) {
          names = Arrays
              .asList(getName(), ENRICHMENT.toString(), topicName, "THREAT_TRIAGE", name, "reason");
          visitor.visit(names, r.getReason());
        }
      });

    } catch (Exception e) {
      List<String> names = Arrays
          .asList(getName(), ENRICHMENT.toString(), topicName, "THREAT_TRIAGE");
      errorConsumer.consume(names, e);
    }
  }

  private void visitProfilerConfigs(CuratorFramework client, StatementReportVisitor visitor,
      ConfigReportErrorConsumer errorConsumer) {
    try {
      byte[] profilerConfigData = null;
      try {
        profilerConfigData = client.getData().forPath(PROFILER.getZookeeperRoot());
      } catch (NoNodeException nne) {
        return;
      }

      ProfilerConfig profilerConfig = JSONUtils.INSTANCE
          .load(new String(profilerConfigData), ProfilerConfig.class);
      profilerConfig.getProfiles().forEach((ProfileConfig pc) -> {
        List<String> names = new LinkedList<>();
        names.add(getName());
        names.add(PROFILER.toString());
        names.add(pc.getProfile());

        // only if
        if (!org.apache.commons.lang.StringUtils.isEmpty(pc.getOnlyif())) {
          names.add("only_if");
          visitor.visit(names, pc.getOnlyif());
          names.remove("only_if");
        }

        // init
        if (!pc.getInit().isEmpty()) {
          names.add("init");
          pc.getInit().forEach((k, v) -> {
            names.add(k);
            visitor.visit(names, v);
            names.remove(k);
          });
          names.remove("init");
        }

        // update
        if (!pc.getUpdate().isEmpty()) {
          names.add("update");
          pc.getUpdate().forEach((k, v) -> {
            names.add(k);
            visitor.visit(names, v);
            names.remove(k);
          });
          names.remove("update");
        }

        // group by
        if (!pc.getGroupBy().isEmpty()) {
          names.add("group_by");
          pc.getGroupBy().forEach((gb) -> visitor.visit(names, gb));
          names.remove("group_by");
        }

        // profile result
        if (pc.getResult() != null) {
          ProfileResult result = pc.getResult();
          ProfileResultExpressions profileResultExpressions = result.getProfileExpressions();
          ProfileTriageExpressions profileTriageExpressions = result.getTriageExpressions();

          names.add("profile_result");
          if (profileResultExpressions != null && !org.apache.commons.lang.StringUtils
              .isEmpty(profileResultExpressions.getExpression())) {
            names.add("profile_expression");
            visitor.visit(names, profileResultExpressions.getExpression());
            names.remove("profile_expression");
          }

          if (profileTriageExpressions != null && !profileTriageExpressions.getExpressions()
              .isEmpty()) {
            names.add("triage_expression");
            profileTriageExpressions.getExpressions().forEach((k, v) -> {
              names.add(k);
              visitor.visit(names, v);
              names.remove(k);
            });
          }
        }


      });
    } catch (Exception e) {
      List<String> names = Arrays.asList(getName(), PROFILER.toString());
      errorConsumer.consume(names, e);
    }
  }
}
