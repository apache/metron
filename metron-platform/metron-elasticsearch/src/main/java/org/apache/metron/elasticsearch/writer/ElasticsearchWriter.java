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
package org.apache.metron.elasticsearch.writer;

import backtype.storm.tuple.Tuple;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.interfaces.BulkMessageWriter;
import org.apache.metron.common.interfaces.FieldNameConverter;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ElasticsearchWriter implements BulkMessageWriter<JSONObject>, Serializable {

  private Map<String, String> optionalSettings;
  private transient TransportClient client;
  private SimpleDateFormat dateFormat;
  private static final Logger LOG = LoggerFactory
          .getLogger(ElasticsearchWriter.class);
  private FieldNameConverter fieldNameConverter = new ElasticsearchFieldNameConverter();

  public ElasticsearchWriter withOptionalSettings(Map<String, String> optionalSettings) {
    this.optionalSettings = optionalSettings;
    return this;
  }

  @Override
  public void init(Map stormConf, WriterConfiguration configurations) {
    Map<String, Object> globalConfiguration = configurations.getGlobalConfig();

    Settings.Builder settingsBuilder = Settings.settingsBuilder();
    settingsBuilder.put("cluster.name", globalConfiguration.get("es.clustername"));
    settingsBuilder.put("client.transport.ping_timeout","500s");

    if (optionalSettings != null) {

      settingsBuilder.put(optionalSettings);

    }

    Settings settings = settingsBuilder.build();

    try{

      client = TransportClient.builder().settings(settings).build()
              .addTransportAddress(
                      new InetSocketTransportAddress(InetAddress.getByName(globalConfiguration.get("es.ip").toString()), Integer.parseInt(globalConfiguration.get("es.port").toString()) )
              );


    } catch (UnknownHostException exception){

      throw new RuntimeException(exception);
    }

    dateFormat = new SimpleDateFormat((String) globalConfiguration.get("es.date.format"));

  }

  @Override
  public void write(String sensorType, WriterConfiguration configurations, Iterable<Tuple> tuples, List<JSONObject> messages) throws Exception {
    String indexPostfix = dateFormat.format(new Date());
    BulkRequestBuilder bulkRequest = client.prepareBulk();

    for(JSONObject message: messages) {

      String indexName = sensorType;

      if (configurations != null) {
        indexName = configurations.getIndex(sensorType);
      }

      indexName = indexName + "_index_" + indexPostfix;

      JSONObject esDoc = new JSONObject();
      for(Object k : message.keySet()){

        deDot(k.toString(),message,esDoc);

      }

      IndexRequestBuilder indexRequestBuilder = client.prepareIndex(indexName,
              sensorType + "_doc");
      indexRequestBuilder.setSource(esDoc.toJSONString()).setTimestamp(esDoc.get("timestamp").toString());
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

  //JSONObject doesn't expose map generics
  @SuppressWarnings("unchecked")
  private void deDot(String field, JSONObject origMessage, JSONObject message){

    if(field.contains(".")){

      if(LOG.isDebugEnabled()){
        LOG.debug("Dotted field: " + field);
      }

    }
    String newkey = fieldNameConverter.convert(field);
    message.put(newkey,origMessage.get(field));

  }

}

