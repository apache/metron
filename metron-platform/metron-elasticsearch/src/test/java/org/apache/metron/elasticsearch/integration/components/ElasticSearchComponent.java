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
package org.apache.metron.elasticsearch.integration.components;

import org.apache.commons.io.FileUtils;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.UnableToStartException;
import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthAction;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ElasticSearchComponent implements InMemoryComponent {

    public static class Builder{
        private int httpPort;
        private File indexDir;
        private Map<String, String> extraElasticSearchSettings = null;
        public Builder withHttpPort(int httpPort) {
            this.httpPort = httpPort;
            return this;
        }
        public Builder withIndexDir(File indexDir) {
            this.indexDir = indexDir;
            return this;
        }
        public Builder withExtraElasticSearchSettings(Map<String, String> extraElasticSearchSettings) {
            this.extraElasticSearchSettings = extraElasticSearchSettings;
            return this;
        }
        public ElasticSearchComponent build() {
            return new ElasticSearchComponent(httpPort, indexDir, extraElasticSearchSettings);
        }
    }

    private Client client;
    private Node node;
    private int httpPort;
    private File indexDir;
    private Map<String, String> extraElasticSearchSettings;

    public ElasticSearchComponent(int httpPort, File indexDir) {
        this(httpPort, indexDir, null);
    }
    public ElasticSearchComponent(int httpPort, File indexDir, Map<String, String> extraElasticSearchSettings) {
        this.httpPort = httpPort;
        this.indexDir = indexDir;
        this.extraElasticSearchSettings = extraElasticSearchSettings;
    }
    public Client getClient() {
        return client;
    }

    private void cleanDir(File dir) throws IOException {
        if(dir.exists()) {
            FileUtils.deleteDirectory(dir);
        }
        dir.mkdirs();
    }
    public void start() throws UnableToStartException {
        File logDir= new File(indexDir, "/logs");
        File dataDir= new File(indexDir, "/data");
        try {
            cleanDir(logDir);
            cleanDir(dataDir);

        } catch (IOException e) {
            throw new UnableToStartException("Unable to clean log or data directories", e);
        }
        ImmutableSettings.Builder immutableSettings = ImmutableSettings.settingsBuilder()
                .put("node.http.enabled", true)
                .put("http.port", httpPort)
                .put("cluster.name", "metron")
                .put("path.logs",logDir.getAbsolutePath())
                .put("path.data",dataDir.getAbsolutePath())
                .put("gateway.type", "none")
                .put("index.store.type", "memory")
                .put("index.number_of_shards", 1)
                .put("node.mode", "network")
                .put("index.number_of_replicas", 1);
        if(extraElasticSearchSettings != null) {
            immutableSettings = immutableSettings.put(extraElasticSearchSettings);
        }
        Settings settings = immutableSettings.build();
        node = NodeBuilder.nodeBuilder().settings(settings).node();
        node.start();
        settings = ImmutableSettings.settingsBuilder()
					.put("cluster.name", "metron").build();
		client = new TransportClient(settings)
					.addTransportAddress(new InetSocketTransportAddress("localhost",
							9300));

        waitForCluster(client, ClusterHealthStatus.YELLOW, new TimeValue(60000));
    }

    public static void waitForCluster(ElasticsearchClient client, ClusterHealthStatus status, TimeValue timeout) throws UnableToStartException {
        try {
            ClusterHealthResponse healthResponse =
                    (ClusterHealthResponse)client.execute(ClusterHealthAction.INSTANCE, new ClusterHealthRequest().waitForStatus(status).timeout(timeout)).actionGet();
            if (healthResponse != null && healthResponse.isTimedOut()) {
                throw new UnableToStartException("cluster state is " + healthResponse.getStatus().name()
                        + " and not " + status.name()
                        + ", from here on, everything will fail!");
            }
        } catch (ElasticsearchTimeoutException e) {
            throw new UnableToStartException("timeout, cluster does not respond to health request, cowardly refusing to continue with operations");
        }
    }

    public List<Map<String, Object>> getAllIndexedDocs(String index, String sourceType) throws IOException {
       return getAllIndexedDocs(index, sourceType, null);
    }
    public List<Map<String, Object>> getAllIndexedDocs(String index, String sourceType, String subMessage) throws IOException {
        getClient().admin().indices().refresh(new RefreshRequest());
        SearchResponse response = getClient().prepareSearch(index)
                .setTypes(sourceType)
                .setSource("message")
                .setFrom(0)
                .setSize(1000)
                .execute().actionGet();
        List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
        for (SearchHit hit : response.getHits()) {
            Object o = null;
            if(subMessage == null) {
                o = hit.getSource();
            }
            else {
                o = hit.getSource().get(subMessage);
            }
            ret.add((Map<String, Object>)(o));
        }
        return ret;
    }
    public boolean hasIndex(String indexName) {
        Set<String> indices = getClient().admin()
                                    .indices()
                                    .stats(new IndicesStatsRequest())
                                    .actionGet()
                                    .getIndices()
                                    .keySet();
        return indices.contains(indexName);

    }

    public void stop() {
        node.stop();
        node = null;
        client = null;
    }
}
