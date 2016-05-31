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
package org.apache.metron.solr.writer;

import backtype.storm.tuple.Tuple;
import org.apache.metron.common.configuration.Configurations;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.common.interfaces.BulkMessageWriter;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class SolrWriter implements BulkMessageWriter<JSONObject>, Serializable {

  public static final String DEFAULT_COLLECTION = "metron";

  private static final Logger LOG = LoggerFactory
          .getLogger(SolrWriter.class);

  private boolean shouldCommit = false;
  private MetronSolrClient solr;

  public SolrWriter withShouldCommit(boolean shouldCommit) {
    this.shouldCommit = shouldCommit;
    return this;
  }

  public SolrWriter withMetronSolrClient(MetronSolrClient solr) {
    this.solr = solr;
    return this;
  }

  @Override
  public void init(Map stormConf, EnrichmentConfigurations configurations) throws IOException, SolrServerException {
    Map<String, Object> globalConfiguration = configurations.getGlobalConfig();
    if(solr == null) solr = new MetronSolrClient((String) globalConfiguration.get("solr.zookeeper"));
    String collection = getCollection(configurations);
    solr.createCollection(collection, (Integer) globalConfiguration.get("solr.numShards"), (Integer) globalConfiguration.get("solr.replicationFactor"));
    solr.setDefaultCollection(collection);
  }

  @Override
  public void write(String sourceType, EnrichmentConfigurations configurations, List<Tuple> tuples, List<JSONObject> messages) throws Exception {
    for(JSONObject message: messages) {
      SolrInputDocument document = new SolrInputDocument();
      document.addField("id", getIdValue(message));
      document.addField("sensorType", sourceType);
      for(Object key: message.keySet()) {
        Object value = message.get(key);
        document.addField(getFieldName(key, value), value);
      }
      UpdateResponse response = solr.add(document);
    }
    if (shouldCommit) {
      solr.commit(getCollection(configurations));
    }
  }

  protected String getCollection(Configurations configurations) {
    String collection = (String) configurations.getGlobalConfig().get("solr.collection");
    return collection != null ? collection : DEFAULT_COLLECTION;
  }

  private int getIdValue(JSONObject message) {
    return message.toJSONString().hashCode();
  }

  protected String getFieldName(Object key, Object value) {
    String field;
    if (value instanceof Integer) {
      field = key + "_i";
    } else if (value instanceof Long) {
      field = key + "_l";
    } else if (value instanceof Float) {
      field = key + "_f";
    } else if (value instanceof Double) {
      field = key + "_d";
    } else {
      field = key + "_s";
    }
    return field;
  }

  @Override
  public void close() throws Exception {
    solr.close();
  }
}
