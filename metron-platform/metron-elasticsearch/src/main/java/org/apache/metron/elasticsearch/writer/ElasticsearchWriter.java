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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.field.FieldNameConverter;
import org.apache.metron.common.field.FieldNameConverters;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Tuple;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
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

import static org.apache.metron.elasticsearch.utils.ElasticsearchUtils.DOC_ID_SOURCE_FIELD;
import static org.apache.metron.elasticsearch.utils.ElasticsearchUtils.DOC_ID_SOURCE_FIELD_DEFAULT;

/**
 * A {@link BulkMessageWriter} that writes messages to Elasticsearch.
 */
public class ElasticsearchWriter implements BulkMessageWriter<JSONObject>, Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The Elasticsearch client.
   */
  private transient TransportClient client;

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
    // writer settings
    FieldNameConverter fieldNameConverter = FieldNameConverters.create(sensorType, configurations);
    final String indexPostfix = dateFormat.format(new Date());
    final String indexName = ElasticsearchUtils.getIndexName(sensorType, indexPostfix, configurations);
    final String docType = sensorType + "_doc";
    final String docIdSourceField = (String) configurations.getGlobalConfig().getOrDefault(DOC_ID_SOURCE_FIELD, DOC_ID_SOURCE_FIELD_DEFAULT);

    BulkRequestBuilder bulkRequest = client.prepareBulk();
    for(JSONObject message: messages) {

      // clone the message to use as the document that will be indexed
      JSONObject esDoc = new JSONObject();
      for(Object k : message.keySet()){
        copyField(k.toString(), message, esDoc, fieldNameConverter);
      }

      IndexRequestBuilder indexRequestBuilder = client
              .prepareIndex(indexName, docType)
              .setSource(esDoc.toJSONString());

      // set the document identifier
      if(StringUtils.isNotBlank(docIdSourceField)) {
        String docId = (String) esDoc.get(docIdSourceField);
        if(docId != null) {
          indexRequestBuilder.setId(docId);
        } else {
          LOG.warn("Message is missing document ID source field; document ID not set; sourceField={}", docIdSourceField);
        }
      }

      // set the document timestamp, if one exists
      Object ts = esDoc.get(Constants.Fields.TIMESTAMP.getName());
      if(ts != null) {
        indexRequestBuilder.setTimestamp(ts.toString());
      }

      bulkRequest.add(indexRequestBuilder);
    }

    BulkResponse bulkResponse = bulkRequest.execute().actionGet();
    LOG.info("Wrote {} message(s) to Elasticsearch; sensorType={}, index={}, docType={}, took={}",
            ArrayUtils.getLength(bulkResponse.getItems()), sensorType, indexName, docType, bulkResponse.getTook().format());
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

