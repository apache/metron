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

import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrException;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Tuple;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Optional;

public class SolrWriter implements BulkMessageWriter<JSONObject>, Serializable {

  public static final String ZOOKEEPER_PROP = "solr.zookeeper";
  public static final String COMMIT_PER_BATCH_PROP = "solr.commitPerBatch";
  public static final String DEFAULT_COLLECTION_PROP = "solr.collection";
  public static final String SOLR_HTTP_CONFIG_PROP = "solr.http.config";
  public static final String DEFAULT_COLLECTION = "metron";

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Boolean shouldCommit = true;
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
  public void init(Map stormConf, TopologyContext topologyContext, WriterConfiguration configurations) throws IOException, SolrServerException {
    Map<String, Object> globalConfiguration = configurations.getGlobalConfig();
    String zookeeperUrl = (String) globalConfiguration.get(ZOOKEEPER_PROP);
    String defaultCollection = (String) globalConfiguration.get(DEFAULT_COLLECTION_PROP);
    Map<String, Object> solrHttpConfig = (Map<String, Object>)globalConfiguration.get(SOLR_HTTP_CONFIG_PROP);
    Object commitPerBatchObj = globalConfiguration.get(COMMIT_PER_BATCH_PROP);
    if(commitPerBatchObj != null) {
      Boolean commit = ConversionUtils.convert(commitPerBatchObj, Boolean.class);
      if(commit == null) {
        LOG.warn("Unable to convert {} to boolean, was {}", COMMIT_PER_BATCH_PROP, "" + commitPerBatchObj);
      }
      else {
        shouldCommit = commit;
      }
    }
    LOG.info("Initializing SOLR writer: {}", zookeeperUrl);
    LOG.info("Forcing commit per batch: {}", shouldCommit);
    LOG.info("Default Collection: {}", "" + defaultCollection );
    if(solr == null) {
      solr = new MetronSolrClient(zookeeperUrl, solrHttpConfig);

    }
    if(!StringUtils.isEmpty(defaultCollection)) {
      solr.setDefaultCollection(defaultCollection);
    }
    else {
      solr.setDefaultCollection(DEFAULT_COLLECTION);
    }
  }

  public Collection<SolrInputDocument> toDocs(Iterable<JSONObject> messages) {
    Collection<SolrInputDocument> ret = new ArrayList<>();
    for(JSONObject message: messages) {
      SolrInputDocument document = new SolrInputDocument();
      for (Object key : message.keySet()) {
        Object value = message.get(key);
        if (value instanceof Iterable) {
          for (Object v : (Iterable) value) {
            document.addField("" + key, v);
          }
        } else {
          document.addField("" + key, value);
        }
      }
      if (!document.containsKey(Constants.GUID)) {
        document.addField(Constants.GUID, UUID.randomUUID().toString());
      }
      ret.add(document);
    }
    return ret;
  }

  protected String getCollection(String sourceType, WriterConfiguration configurations) {
    String collection = configurations.getIndex(sourceType);
    if(StringUtils.isEmpty(collection)) {
      return solr.getDefaultCollection();
    }
    return collection;
  }

  @Override
  public BulkWriterResponse write(String sourceType, WriterConfiguration configurations, Iterable<Tuple> tuples, List<JSONObject> messages) throws Exception {
    String collection = getCollection(sourceType, configurations);
    BulkWriterResponse bulkResponse = new BulkWriterResponse();
    Collection<SolrInputDocument> docs = toDocs(messages);
    try {
      Optional<SolrException> exceptionOptional = fromUpdateResponse(solr.add(collection, docs));
      // Solr commits the entire batch or throws an exception for it.  There's no way to get partial failures.
      if(exceptionOptional.isPresent()) {
        bulkResponse.addAllErrors(exceptionOptional.get(), tuples);
      }
      else {
        if (shouldCommit) {
          exceptionOptional = fromUpdateResponse(solr.commit(collection));
          if(exceptionOptional.isPresent()) {
            bulkResponse.addAllErrors(exceptionOptional.get(), tuples);
          }
        }
        if(!exceptionOptional.isPresent()) {
          bulkResponse.addAllSuccesses(tuples);
        }
      }
    }
    catch(HttpSolrClient.RemoteSolrException sse) {
      bulkResponse.addAllErrors(sse, tuples);
    }

    return bulkResponse;
  }

  protected Optional<SolrException> fromUpdateResponse(UpdateResponse response) {
    if(response != null && response.getStatus() > 0) {
      String message = "Solr Update response: " + Joiner.on(",").join(response.getResponse());
      return Optional.of(new SolrException(SolrException.ErrorCode.BAD_REQUEST, message));
    }
    return Optional.empty();
  }

  @Override
  public String getName() {
    return "solr";
  }

  @Override
  public void close() throws Exception {
    if(solr != null) {
      solr.close();
    }
  }
}
