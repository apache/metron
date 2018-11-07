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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

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
    private Optional<SuccessListener> onSuccess;
    private Optional<FailureListener> onFailure;
    private ElasticsearchClient client;
    private List<Indexable> documents;
    private boolean defineDocumentId;

    public ElasticsearchBulkDocumentWriter(ElasticsearchClient client) {
        this.client = client;
        this.onSuccess = Optional.empty();
        this.onFailure = Optional.empty();
        this.documents = new ArrayList<>();
        this.defineDocumentId = false;
    }

    /**
     * By default, the {@link ElasticsearchBulkDocumentWriter} does not explicitly define
     * the document ID. In most cases this is the most performant option.
     *
     * If set to true, the document ID will be explicitly set to the Metron GUID.  The
     * Metron GUID is a randomized UUID generated with Java's `UUID.randomUUID()`. Using a
     * randomized UUID for the Elasticsearch document ID can negatively impact indexing
     * performance.
     *
     * @param defineDocumentId If the document ID should be defined.
     */
    public void setDefineDocumentId(boolean defineDocumentId) {
        this.defineDocumentId = defineDocumentId;
    }

    @Override
    public void onSuccess(SuccessListener<D> onSuccess) {
        this.onSuccess = Optional.of(onSuccess);
    }

    @Override
    public void onFailure(FailureListener<D> onFailure) {
        this.onFailure = Optional.of(onFailure);
    }

    @Override
    public void addDocument(D document, String index) {
        documents.add(new Indexable(document, index));
    }

    @Override
    public void write() {
        try {
            // create an index request for each document
            List<DocWriteRequest> requests = documents
                    .stream()
                    .map(ix -> createRequest(ix.document, ix.index))
                    .collect(Collectors.toList());

            // create one bulk request to wrap each of the index requests
            BulkRequest bulkRequest = new BulkRequest();
            bulkRequest.add(requests);

            // submit the request and handle the response
            BulkResponse bulkResponse = client.getHighLevelClient().bulk(bulkRequest);
            List<D> successful = handleBulkResponse(bulkResponse, documents);

            // notify the success callback
            onSuccess.ifPresent(listener -> listener.onSuccess(successful));
            LOG.debug("Wrote document(s) to Elasticsearch; batchSize={}, success={}, failed={}, took={} ms",
                    documents.size(), successful.size(), documents.size() - successful.size(), bulkResponse.getTookInMillis());

        } catch(IOException e) {
            // failed to submit bulk request; all documents failed
            if(onFailure.isPresent()) {
                for(D failed: getDocuments()) {
                    onFailure.get().onFailure(failed, e, ExceptionUtils.getRootCauseMessage(e));
                }
            }
            LOG.error("Failed to submit bulk request; all documents failed", e);
        }
    }

    private IndexRequest createRequest(D document, String index) {
        if(document.getTimestamp() == null) {
            throw new IllegalArgumentException("Document must contain the timestamp");
        }

        IndexRequest request = new IndexRequest()
                .source(document.getDocument())
                .type(document.getSensorType() + "_doc")
                .index(index)
                .timestamp(document.getTimestamp().toString());

        if(defineDocumentId) {
            request.id(document.getGuid());
        }

        return request;
    }

    /**
     * Handles the {@link BulkResponse} received from Elasticsearch.
     * @param bulkResponse The response received from Elasticsearch.
     * @param documents The documents included in the bulk request.
     * @return The documents that were successfully written. Failed documents are excluded.
     */
    private List<D> handleBulkResponse(BulkResponse bulkResponse, List<Indexable> documents) {
        List<D> successful = new ArrayList<>();

        // interrogate the response to distinguish between those that succeeded and those that failed
        Iterator<BulkItemResponse> iterator = bulkResponse.iterator();
        while(iterator.hasNext()) {
            BulkItemResponse response = iterator.next();
            if(response.isFailed()) {
                // request failed
                D failed = getDocument(response.getItemId());
                Exception cause = response.getFailure().getCause();
                String message = response.getFailureMessage();
                onFailure.ifPresent(listener -> listener.onFailure(failed, cause, message));
                LOG.error(format("Failed to write message; error=%s", message), cause);

            } else {
                // request succeeded
                D success = getDocument(response.getItemId());
                success.setDocumentID(response.getResponse().getId());
                successful.add(success);
            }
        }

        return successful;
    }

    private List<D> getDocuments() {
        return documents.stream().map(ix -> ix.document).collect(Collectors.toList());
    }

    private D getDocument(int index) {
        return documents.get(index).document;
    }
}
