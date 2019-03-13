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
package org.apache.metron.enrichment.integration;

import org.apache.metron.common.Constants;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;

import java.util.Properties;

/**
 * Integration test for the 'Unified' enrichment topology.
 */
public class UnifiedEnrichmentIntegrationTest extends EnrichmentIntegrationTest {

  /**
   * Returns the path to the topology properties template.
   *
   * @return The path to the topology properties template.
   */
  public String getTemplatePath() {
    return "src/main/config/enrichment-unified.properties.j2";
  }

  /**
   * Properties for the 'Unified' topology.
   *
   * @return The topology properties.
   */
  @Override
  public Properties getTopologyProperties() {
    return new Properties() {{

      // storm
      setProperty("enrichment_workers", "1");
      setProperty("enrichment_acker_executors", "0");
      setProperty("enrichment_topology_worker_childopts", "");
      setProperty("topology_auto_credentials", "[]");
      setProperty("enrichment_topology_max_spout_pending", "500");

      // kafka - zookeeper_quorum, kafka_brokers set elsewhere
      setProperty("kafka_security_protocol", "PLAINTEXT");
      setProperty("enrichment_kafka_start", "UNCOMMITTED_EARLIEST");
      setProperty("enrichment_input_topic", Constants.ENRICHMENT_TOPIC);
      setProperty("enrichment_output_topic", Constants.INDEXING_TOPIC);
      setProperty("enrichment_error_topic", ERROR_TOPIC);
      setProperty("threatintel_error_topic", ERROR_TOPIC);

      // enrichment
      setProperty("enrichment_hbase_provider_impl", "" + MockHBaseTableProvider.class.getName());
      setProperty("enrichment_hbase_table", enrichmentsTableName);
      setProperty("enrichment_hbase_cf", cf);
      setProperty("enrichment_host_known_hosts", "[{\"ip\":\"10.1.128.236\", \"local\":\"YES\", \"type\":\"webserver\", \"asset_value\" : \"important\"}," +
              "{\"ip\":\"10.1.128.237\", \"local\":\"UNKNOWN\", \"type\":\"unknown\", \"asset_value\" : \"important\"}," +
              "{\"ip\":\"10.60.10.254\", \"local\":\"YES\", \"type\":\"printer\", \"asset_value\" : \"important\"}," +
              "{\"ip\":\"10.0.2.15\", \"local\":\"YES\", \"type\":\"printer\", \"asset_value\" : \"important\"}]");

      // threat intel
      setProperty("threatintel_hbase_table", threatIntelTableName);
      setProperty("threatintel_hbase_cf", cf);

      // parallelism
      setProperty("unified_kafka_spout_parallelism", "1");
      setProperty("unified_enrichment_parallelism", "1");
      setProperty("unified_threat_intel_parallelism", "1");
      setProperty("unified_kafka_writer_parallelism", "1");

      // caches
      setProperty("unified_enrichment_cache_size", "1000");
      setProperty("unified_threat_intel_cache_size", "1000");

      // threads
      setProperty("unified_enrichment_threadpool_size", "1");
      setProperty("unified_enrichment_threadpool_type", "FIXED");
    }};
  }

  @Override
  public String fluxPath() {
    return "src/main/flux/enrichment/remote-unified.yaml";
  }
}
