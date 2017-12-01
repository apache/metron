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
import java.util.LinkedList;
import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.enrichment.EnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.configuration.profiler.ProfileResult;
import org.apache.metron.common.configuration.profiler.ProfileResultExpressions;
import org.apache.metron.common.configuration.profiler.ProfileTriageExpressions;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.common.field.transformation.StellarTransformation;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.stellar.common.utils.validation.ExpressionConfigurationHolder;
import org.apache.metron.stellar.common.utils.validation.StellarConfigurationProvider;
import org.apache.metron.stellar.common.utils.validation.StellarConfiguredStatementVisitor.ErrorConsumer;
import org.apache.zookeeper.KeeperException.NoNodeException;

/**
 * StellarStatementReporter is used to report all of the configured / deployed Stellar statements in
 * the system.
 */
public class StellarStatementReporter implements StellarConfigurationProvider {

  public StellarStatementReporter() {
  }

  @Override
  public String getName() {
    return "Apache Metron";
  }

  @Override
  public List<ExpressionConfigurationHolder> provideConfigurations(CuratorFramework client,
      ErrorConsumer errorConsumer) {
    List<ExpressionConfigurationHolder> holders = new LinkedList<>();
    visitParserConfigs(client, holders, errorConsumer);
    visitEnrichmentConfigs(client, holders, errorConsumer);
    visitProfilerConfigs(client, holders, errorConsumer);
    return holders;
  }

  private void visitParserConfigs(CuratorFramework client,
      List<ExpressionConfigurationHolder> holders, ErrorConsumer errorConsumer) {
    List<String> children = null;

    try {
      children = client.getChildren().forPath(PARSER.getZookeeperRoot());
    } catch (Exception nne) {
      return;
    }
    for (String child : children) {
      try {
        byte[] data = client.getData().forPath(PARSER.getZookeeperRoot() + "/" + child);
        SensorParserConfig parserConfig = SensorParserConfig.fromBytes(data);
        ExpressionConfigurationHolder holder = new ExpressionConfigurationHolder(
            String.format("%s/%s", getName(), PARSER.toString()), parserConfig.getSensorTopic(),
            parserConfig);
        holders.add(holder);
      } catch (Exception e) {
        errorConsumer.consume(String.format("%s/%s/%s", getName(), PARSER.toString(), child), e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void visitEnrichmentConfigs(CuratorFramework client,
      List<ExpressionConfigurationHolder> holders, ErrorConsumer errorConsumer) {
    List<String> children = null;

    try {
      children = client.getChildren().forPath(ENRICHMENT.getZookeeperRoot());
    } catch (Exception nne) {
      return;
    }

    for (String child : children) {

      try {
        byte[] data = client.getData().forPath(ENRICHMENT.getZookeeperRoot() + "/" + child);
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
        ExpressionConfigurationHolder holder = new ExpressionConfigurationHolder(
            String.format("%s/%s", getName(), ENRICHMENT.toString()), child,
            sensorEnrichmentConfig);
        holders.add(holder);
        /*
        EnrichmentConfig enrichmentConfig;
        enrichmentConfig = sensorEnrichmentConfig.getEnrichment();
        visitEnrichmentConfig(child, Type.ENRICHMENT, enrichmentConfig, visitor, errorConsumer);
        enrichmentConfig = sensorEnrichmentConfig.getThreatIntel();
        visitEnrichmentConfig(child, Type.THREAT_INTEL, enrichmentConfig, visitor, errorConsumer);
        final ThreatTriageConfig threatTriageConfig = sensorEnrichmentConfig.getThreatIntel()
            .getTriageConfig();
        visitEnrichmentThreatTriageConfigs(child, threatTriageConfig, visitor, errorConsumer);
        */
      } catch (Exception e) {
        errorConsumer
            .consume(String.format("%s/%s/%s", getName(), ENRICHMENT.toString(), child), e);
      }
    }
  }

  private void visitProfilerConfigs(CuratorFramework client,
      List<ExpressionConfigurationHolder> holders, ErrorConsumer errorConsumer) {
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
        ExpressionConfigurationHolder holder = new ExpressionConfigurationHolder(
            String.format("%s/%s", getName(), PROFILER.toString()), pc.getProfile(), pc);
        holders.add(holder);
      });
    } catch (Exception e) {
      errorConsumer.consume(String.format("%s/%s", getName(), PROFILER.toString()), e);
    }
  }
}
