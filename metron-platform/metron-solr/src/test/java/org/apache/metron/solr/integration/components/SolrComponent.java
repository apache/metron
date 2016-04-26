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
package org.apache.metron.solr.integration.components;

import com.google.common.base.Function;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.solr.writer.MetronSolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.apache.solr.common.SolrDocument;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolrComponent implements InMemoryComponent {

  public static class Builder {
    private int port = 8983;
    private String solrXmlPath = "../metron-solr/src/test/resources/solr/solr.xml";
    private Map<String, String> collections = new HashMap<>();
    private Function<SolrComponent, Void> postStartCallback;

    public Builder withPort(int port) {
      this.port = port;
      return this;
    }

    public Builder withSolrXmlPath(String solrXmlPath) {
      this.solrXmlPath = solrXmlPath;
      return this;
    }

    public Builder addCollection(String name, String configPath) {
      collections.put(name, configPath);
      return this;
    }

    public Builder withPostStartCallback(Function<SolrComponent, Void> f) {
      postStartCallback = f;
      return this;
    }

    public SolrComponent build() throws Exception {
      if (collections.isEmpty()) throw new Exception("Must add at least 1 collection");
      return new SolrComponent(port, solrXmlPath, collections, postStartCallback);
    }
  }

  private int port;
  private String solrXmlPath;
  private Map<String, String> collections;
  private MiniSolrCloudCluster miniSolrCloudCluster;
  private Function<SolrComponent, Void> postStartCallback;

  private SolrComponent(int port, String solrXmlPath, Map<String, String> collections, Function<SolrComponent, Void> postStartCallback) throws Exception {
    this.port = port;
    this.solrXmlPath = solrXmlPath;
    this.collections = collections;
    this.postStartCallback = postStartCallback;
  }

  @Override
  public void start() throws UnableToStartException {
    try {
      File baseDir = Files.createTempDirectory("solrcomponent").toFile();
      baseDir.deleteOnExit();
      miniSolrCloudCluster = new MiniSolrCloudCluster(1, baseDir, new File(solrXmlPath), JettyConfig.builder().setPort(port).build());
      for(String name: collections.keySet()) {
        String configPath = collections.get(name);
        miniSolrCloudCluster.uploadConfigDir(new File(configPath), name);
      }
      miniSolrCloudCluster.createCollection("metron", 1, 1, "metron", new HashMap<String, String>());
      if (postStartCallback != null) postStartCallback.apply(this);
    } catch(Exception e) {
      throw new UnableToStartException(e.getMessage(), e);
    }
  }

  @Override
  public void stop() {
    try {
      miniSolrCloudCluster.shutdown();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public MetronSolrClient getSolrClient() {
    return new MetronSolrClient(getZookeeperUrl());
  }

  public MiniSolrCloudCluster getMiniSolrCloudCluster() {
    return this.miniSolrCloudCluster;
  }

  public String getZookeeperUrl() {
    return miniSolrCloudCluster.getZkServer().getZkAddress();
  }

  public boolean hasCollection(String collection) {
    MetronSolrClient solr = getSolrClient();
    boolean collectionFound = false;
    try {
      collectionFound = solr.listCollections().contains(collection);
    } catch(Exception e) {
      e.printStackTrace();
    }
    return collectionFound;
  }

  public List<Map<String, Object>> getAllIndexedDocs(String collection) {
    List<Map<String, Object>> docs = new ArrayList<>();
    CloudSolrClient solr = miniSolrCloudCluster.getSolrClient();
    solr.setDefaultCollection(collection);
    SolrQuery parameters = new SolrQuery();
    parameters.set("q", "*:*");
    try {
      solr.commit();
      QueryResponse response = solr.query(parameters);
      for (SolrDocument solrDocument : response.getResults()) {
        docs.add(solrDocument);
      }
    } catch (SolrServerException | IOException e) {
      e.printStackTrace();
    }
    return docs;
  }
}
