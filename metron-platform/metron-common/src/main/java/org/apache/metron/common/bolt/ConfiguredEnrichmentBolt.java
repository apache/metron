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
package org.apache.metron.common.bolt;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.stellar.common.utils.HttpClientUtils;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConfiguredEnrichmentBolt extends ConfiguredBolt<EnrichmentConfigurations> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected CloseableHttpClient httpClient;

  public ConfiguredEnrichmentBolt(String zookeeperUrl) {
    super(zookeeperUrl, "ENRICHMENT");
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    super.prepare(stormConf, context, collector);
    httpClient = HttpClientUtils.getPoolingClient(getConfigurations().getGlobalConfig());
  }

  @Override
  public void cleanup() {
    super.cleanup();
    try {
      httpClient.close();
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
  }
}
