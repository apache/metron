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

import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.field.FieldNameConverter;
import org.apache.metron.common.field.FieldNameConverters;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.metron.elasticsearch.utils.ElasticsearchClient;
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Tuple;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A {@link BulkMessageWriter} that writes messages to Elasticsearch.
 */
public class ElasticsearchWriter implements BulkMessageWriter<JSONObject>, Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The Elasticsearch client.
   */
  private transient ElasticsearchClient client;

  /**
   * A simple data formatter used to build the appropriate Elasticsearch index name.
   */
  private SimpleDateFormat dateFormat;


  @Override
  public void init(Map stormConf, TopologyContext topologyContext, WriterConfiguration configurations) {

    Map<String, Object> globalConfiguration = configurations.getGlobalConfig();
    client = ElasticsearchUtils.getClient(globalConfiguration);
    dateFormat = ElasticsearchUtils.getIndexFormat(globalConfiguration);
  }

  @Override
  public BulkWriterResponse write(String sensorType, WriterConfiguration configurations, Iterable<Tuple> tuples, List<JSONObject> messages) throws Exception {

    // fetch the field name converter for this sensor type
    FieldNameConverter fieldNameConverter = FieldNameConverters.create(sensorType, configurations);

    final String indexPostfix = dateFormat.format(new Date());
    BulkRequest bulkRequest = new BulkRequest();
    //BulkRequestBuilder bulkRequest = client.prepareBulk();
    for(JSONObject message: messages) {

      JSONObject esDoc = new JSONObject();
      for(Object k : message.keySet()){
        copyField(k.toString(), message, esDoc, fieldNameConverter);
      }

      String indexName = ElasticsearchUtils.getIndexName(sensorType, indexPostfix, configurations);
      IndexRequest indexRequest = new IndexRequest(indexName, sensorType + "_doc");
      indexRequest.source(esDoc.toJSONString());
      String guid = (String)esDoc.get(Constants.GUID);
      if(guid != null) {
        indexRequest.id(guid);
      }

      Object ts = esDoc.get("timestamp");
      if(ts != null) {
        indexRequest.timestamp(ts.toString());
      }
      bulkRequest.add(indexRequest);
    }

    BulkResponse bulkResponse = client.getHighLevelClient().bulk(bulkRequest);
    return buildWriteReponse(tuples, bulkResponse);
  }

  @Override
  public String getName() {
    return "elasticsearch";
  }

  protected BulkWriterResponse buildWriteReponse(Iterable<Tuple> tuples, BulkResponse bulkResponse) throws Exception {
    // Elasticsearch responses are in the same order as the request, giving us an implicit mapping with Tuples
    BulkWriterResponse writerResponse = new BulkWriterResponse();
    if (bulkResponse.hasFailures()) {
      Iterator<BulkItemResponse> respIter = bulkResponse.iterator();
      Iterator<Tuple> tupleIter = tuples.iterator();
      while (respIter.hasNext() && tupleIter.hasNext()) {
        BulkItemResponse item = respIter.next();
        Tuple tuple = tupleIter.next();

        if (item.isFailed()) {
          writerResponse.addError(item.getFailure().getCause(), tuple);
        } else {
          writerResponse.addSuccess(tuple);
        }

        // Should never happen, so fail the entire batch if it does.
        if (respIter.hasNext() != tupleIter.hasNext()) {
          throw new Exception(bulkResponse.buildFailureMessage());
        }
      }
    } else {
      writerResponse.addAllSuccesses(tuples);
    }

    return writerResponse;
  }

  @Override
  public void close() throws Exception {
    client.close();
  }

  /**
   * Copies the value of a field from the source message to the destination message.
   *
   * <p>A field name may also be transformed in the destination message by the {@link FieldNameConverter}.
   *
   * @param sourceFieldName The name of the field to copy from the source message
   * @param source The source message.
   * @param destination The destination message.
   * @param fieldNameConverter The field name converter that transforms the field name
   *                           between the source and destination.
   */
  //JSONObject doesn't expose map generics
  @SuppressWarnings("unchecked")
  private void copyField(
          String sourceFieldName,
          JSONObject source,
          JSONObject destination,
          FieldNameConverter fieldNameConverter) {

    // allow the field name to be transformed
    String destinationFieldName = fieldNameConverter.convert(sourceFieldName);

    // copy the field
    destination.put(destinationFieldName, source.get(sourceFieldName));
  }
}

