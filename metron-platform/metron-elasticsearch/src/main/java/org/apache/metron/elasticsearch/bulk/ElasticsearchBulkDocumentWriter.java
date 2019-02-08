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
package org.apache.metron.elasticsearch.bulk;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.metron.elasticsearch.client.ElasticsearchClient;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes documents to an Elasticsearch index in bulk.
 *
 * @param <D> The type of document to write.
 */
public class ElasticsearchBulkDocumentWriter<D extends Document> implements BulkDocumentWriter<D> {

    /**
     * A {@link Document} along with the index it will be written to.
     */
    private class Indexable {
        D document;
        String index;

        public Indexable(D document, String index) {
            this.document = document;
            this.index = index;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private ElasticsearchClient client;
    private List<Indexable> documents;
    private WriteRequest.RefreshPolicy refreshPolicy;

    public ElasticsearchBulkDocumentWriter(ElasticsearchClient client) {
        this.client = client;
        this.documents = new ArrayList<>();
        this.refreshPolicy = WriteRequest.RefreshPolicy.NONE;
    }

    @Override
    public void addDocument(D document, String indexName) {
        documents.add(new Indexable(document, indexName));
        LOG.debug("Adding document to batch; document={}, index={}", document, indexName);
    }

    @Override
    public BulkDocumentWriterResults<D> write() {
        BulkDocumentWriterResults<D> results = new BulkDocumentWriterResults<>();
        try {
            // create an index request for each document
            BulkRequest bulkRequest = new BulkRequest();
            bulkRequest.setRefreshPolicy(refreshPolicy);
            for(Indexable doc: documents) {
                DocWriteRequest request = createRequest(doc.document, doc.index);
                bulkRequest.add(request);
            }

            // submit the request and handle the response
            BulkResponse bulkResponse = client.getHighLevelClient().bulk(bulkRequest);
            handleBulkResponse(bulkResponse, documents, results);
            if (LOG.isDebugEnabled()) {
                String shards = Arrays.stream(bulkResponse.getItems())
                        .map(bulkItemResponse -> bulkItemResponse.getResponse().getShardId().toString())
                        .collect(Collectors.joining(","));
                LOG.debug("{} results written to shards {} in {} ms; batchSize={}, success={}, failed={}",
                        bulkResponse.getItems().length, shards, bulkResponse.getTookInMillis(),
                        documents.size(), results.getSuccesses().size(), results.getFailures().size());
            }
        } catch(IOException e) {
            // assume all documents have failed
            for(Indexable indexable: documents) {
                D failed = indexable.document;
                results.addFailure(failed, e, ExceptionUtils.getRootCauseMessage(e));
            }
            LOG.error("Failed to submit bulk request; all documents failed", e);

        } finally {
            // flush all documents no matter which ones succeeded or failed
            documents.clear();
        }
        return results;
    }

    @Override
    public int size() {
        return documents.size();
    }

    public ElasticsearchBulkDocumentWriter<D> withRefreshPolicy(WriteRequest.RefreshPolicy refreshPolicy) {
        this.refreshPolicy = refreshPolicy;
        return this;
    }

    private IndexRequest createRequest(D document, String index) {
        if(document.getTimestamp() == null) {
            throw new IllegalArgumentException("Document must contain the timestamp");
        }

        // if updating an existing document, the doc ID should be defined.
        // if creating a new document, set the doc ID to null to allow Elasticsearch to generate one.
        String docId = document.getDocumentID().orElse(null);
        if(LOG.isDebugEnabled() && document.getDocumentID().isPresent()) {
            LOG.debug("Updating existing document with known doc ID; docID={}, guid={}, sensorType={}",
                    docId, document.getGuid(), document.getSensorType());
        } else if(LOG.isDebugEnabled()) {
            LOG.debug("Creating a new document, doc ID not yet known; guid={}, sensorType={}",
                    document.getGuid(), document.getSensorType());
        }

        return new IndexRequest()
                .source(document.getDocument())
                .type(document.getSensorType() + "_doc")
                .index(index)
                .id(docId)
                .index(index)
                .timestamp(document.getTimestamp().toString());
    }

    /**
     * Handles the {@link BulkResponse} received from Elasticsearch.
     * @param bulkResponse The response received from Elasticsearch.
     * @param documents The documents included in the bulk request.
     * @param results The writer results.
     */
    private void handleBulkResponse(BulkResponse bulkResponse, List<Indexable> documents, BulkDocumentWriterResults<D> results) {
        if (bulkResponse.hasFailures()) {

            // interrogate the response to distinguish between those that succeeded and those that failed
            for(BulkItemResponse response: bulkResponse) {
                if(response.isFailed()) {
                    // request failed
                    D failed = getDocument(response.getItemId());
                    Exception cause = response.getFailure().getCause();
                    String message = response.getFailureMessage();
                    results.addFailure(failed, cause, message);

                } else {
                    // request succeeded
                    D success = getDocument(response.getItemId());
                    success.setDocumentID(response.getResponse().getId());
                    results.addSuccess(success);
                }
            }
        } else {
            // all requests succeeded
            for(Indexable success: documents) {
                results.addSuccess(success.document);
            }
        }
    }

    private D getDocument(int index) {
        return documents.get(index).document;
    }
}
