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
package org.apache.metron.writer.solr;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.CollectionParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MetronSolrClient extends CloudSolrClient {

  private static final Logger LOG = Logger.getLogger(MetronSolrClient.class);


  public MetronSolrClient(String zkHost) {
    super(zkHost);
  }

  public void createCollection(String name, int numShards, int replicationFactor) {
    if(!listCollections().contains(name)) {
      try {
        request(getCreasteCollectionsRequest(name, numShards, replicationFactor));
      } catch (SolrServerException | IOException e) {
        LOG.error(e, e);
      }
    }
  }

  public QueryRequest getCreasteCollectionsRequest(String name, int numShards, int replicationFactor) {
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set("action", CollectionParams.CollectionAction.CREATE.name());
    params.set("name", name);
    params.set("numShards", numShards);
    params.set("replicationFactor", replicationFactor);
    params.set("collection.configName", name);
    QueryRequest request = new QueryRequest(params);
    request.setPath("/admin/collections");
    return request;
  }

  public List<String> listCollections() {
    List<String> collections = new ArrayList<>();
    try {
      NamedList<Object> response = request(getListCollectionsRequest(), null);
      collections = (List<String>) response.get("collections");
    } catch (SolrServerException | IOException e) {
      LOG.error(e, e);
    }
    return collections;
  }

  public QueryRequest getListCollectionsRequest() {
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set("action", CollectionParams.CollectionAction.LIST.name());
    QueryRequest request = new QueryRequest(params);
    request.setPath("/admin/collections");
    return request;
  }

  public String test() { return "test1"; }
}
