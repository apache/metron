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
package org.apache.metron.writer;

import backtype.storm.tuple.Tuple;
import org.apache.metron.domain.SourceConfig;
import org.apache.metron.writer.interfaces.BulkMessageWriter;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ElasticsearchWriter implements BulkMessageWriter<JSONObject>, Serializable {

  private String clusterName;
  private Map<String, String> optionalSettings;
  private transient TransportClient client;
  private String host;
  private int port;
  private SimpleDateFormat dateFormat;
  private static final Logger LOG = LoggerFactory
          .getLogger(ElasticsearchWriter.class);

  public ElasticsearchWriter(String clusterName, String host, int port, String dateFormat) {
    this.clusterName = clusterName;
    this.host = host;
    this.port = port;
    this.dateFormat = new SimpleDateFormat(dateFormat);
  }

  public ElasticsearchWriter withOptionalSettings(Map<String, String> optionalSettings) {
    this.optionalSettings = optionalSettings;
    return this;
  }

  @Override
  public void init() {
    ImmutableSettings.Builder builder = ImmutableSettings.settingsBuilder();
    builder.put("cluster.name", clusterName);
    builder.put("client.transport.ping_timeout","500s");
    if (optionalSettings != null) {
      builder.put(optionalSettings);
    }
    client = new TransportClient(builder.build())
            .addTransportAddress(new InetSocketTransportAddress(host, port))
            ;

  }

  @Override
  public void write(String sourceType, SourceConfig configuration, List<Tuple> tuples, List<JSONObject> messages) throws Exception {
    String indexPostfix = dateFormat.format(new Date());
    BulkRequestBuilder bulkRequest = client.prepareBulk();
    for(JSONObject message: messages) {
      String indexName = sourceType;
      if (configuration != null) {
        indexName = configuration.getIndex();
      }
      IndexRequestBuilder indexRequestBuilder = client.prepareIndex(indexName + "_index_" + indexPostfix,
              sourceType);

      indexRequestBuilder.setSource(message.toJSONString());
      bulkRequest.add(indexRequestBuilder);
    }
    BulkResponse resp = bulkRequest.execute().actionGet();
    if (resp.hasFailures()) {
      throw new Exception(resp.buildFailureMessage());
    }
  }

  @Override
  public void close() throws Exception {
    client.close();
  }
}
