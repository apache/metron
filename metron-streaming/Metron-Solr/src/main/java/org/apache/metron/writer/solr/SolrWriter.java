/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.writer.solr;

import backtype.storm.tuple.Tuple;
import org.apache.log4j.Logger;
import org.apache.metron.domain.SourceConfig;
import org.apache.metron.writer.interfaces.BulkMessageWriter;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class SolrWriter implements BulkMessageWriter<JSONObject>, Serializable {

  public static final String DEFAULT_COLLECTION = "metron";

  private static final Logger LOG = Logger.getLogger(SolrWriter.class);

  private String zookeeperUrl;
  private String collection = DEFAULT_COLLECTION;
  private int numShards = 1;
  private int replicationFactor = 1;
  private MetronSolrClient solr;

  public SolrWriter(String zookeeperUrl) {
    this.zookeeperUrl = zookeeperUrl;
  }

  public SolrWriter withCollection(String collection) {
    this.collection = collection;
    return this;
  }

  public SolrWriter withNumShards(int numShards) {
    this.numShards = numShards;
    return this;
  }

  public SolrWriter withReplicationFactor(int replicationFactor) {
    this.replicationFactor = replicationFactor;
    return this;
  }

  public SolrWriter withMetronSolrClient(MetronSolrClient solr) {
    this.solr = solr;
    return this;
  }

  @Override
  public void init(Map stormConf) {
    if(solr == null) solr = new MetronSolrClient(zookeeperUrl);
    solr.createCollection(collection, numShards, replicationFactor);
    solr.setDefaultCollection(collection);
  }

  @Override
  public void write(String sourceType, SourceConfig configuration, List<Tuple> tuples, List<JSONObject> messages) throws Exception {
    for(JSONObject message: messages) {
      SolrInputDocument document = new SolrInputDocument();
      document.addField("id", getIdValue(message));
      document.addField("sourceType", sourceType);
      for(Object key: message.keySet()) {
        Object value = message.get(key);
        document.addField(getFieldName(key, value), value);
      }
      UpdateResponse response = solr.add(document);
    }
    solr.commit(collection);
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
      field = (String) key;
    }
    return field;
  }

  @Override
  public void close() throws Exception {
    solr.close();
  }
}
