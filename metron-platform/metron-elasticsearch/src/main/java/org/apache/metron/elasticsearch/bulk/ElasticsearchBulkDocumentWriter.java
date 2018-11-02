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
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ElasticsearchBulkDocumentWriter<D extends IndexedDocument> implements BulkDocumentWriter<D> {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Optional<SuccessCallback> onSuccess;
    private Optional<FailureCallback> onFailure;
    private ElasticsearchClient client;

    public ElasticsearchBulkDocumentWriter(ElasticsearchClient client) {
        this.client = client;
        this.onSuccess = Optional.empty();
        this.onFailure = Optional.empty();
    }

    @Override
    public void onSuccess(SuccessCallback<D> onSuccess) {
        this.onSuccess = Optional.of(onSuccess);
    }

    @Override
    public void onFailure(FailureCallback<D> onFailure) {
        this.onFailure = Optional.of(onFailure);
    }

    @Override
    public void write(List<D> documents) {
        try {
            // create an index request for each document
            List<DocWriteRequest> requests = documents
                    .stream()
                    .map(doc -> createRequest(doc))
                    .collect(Collectors.toList());

            // create one bulk request for all the documents
            BulkRequest bulkRequest = new BulkRequest();
            bulkRequest.add(requests);

            // handle the bulk response
            BulkResponse bulkResponse = client.getHighLevelClient().bulk(bulkRequest);
            List<D> successful = handleBulkResponse(bulkResponse, documents);

            // notify the success callback
            onSuccess.ifPresent(callback -> callback.onSuccess(successful));
            LOG.debug("Wrote document(s) to Elasticsearch; batchSize={}, success={}, failed={}, took={} ms",
                    documents.size(), successful.size(), documents.size() - successful.size(), bulkResponse.getTookInMillis());

        } catch(IOException e) {
            // failed to submit bulk request; all documents failed
            for(D failed: documents) {
                onFailure.ifPresent(callback -> callback.onFailure(failed, e, ExceptionUtils.getRootCauseMessage(e)));
            }
            LOG.error("Failed to submit bulk request; all documents failed", e);
        }
    }

    /**
     * Handles the {@link BulkResponse} received from Elasticsearch.
     *
     * @param bulkResponse The response received from Elasticsearch.
     * @param documents The documents that are being written.
     * @return The documents that were successfully written. Failed documents are excluded.
     */
    private List<D> handleBulkResponse(BulkResponse bulkResponse, List<D> documents) {
        List<D> successful = new ArrayList<>();
        if (bulkResponse.hasFailures()) {

            // interrogate the response to distinguish between those that succeeded and those that failed
            Iterator<BulkItemResponse> iterator = bulkResponse.iterator();
            while(iterator.hasNext()) {
                BulkItemResponse response = iterator.next();
                if(response.isFailed()) {
                    // request failed
                    D failed = documents.get(response.getItemId());
                    Exception cause = response.getFailure().getCause();
                    String message = response.getFailureMessage();
                    onFailure.ifPresent(callback -> callback.onFailure(failed, cause, message));

                } else {
                    // request succeeded
                    D success = documents.get(response.getItemId());
                    successful.add(success);
                }
            }
        } else {
            // all requests succeeded
            successful.addAll(documents);
        }

        return successful;
    }

    /**
     * Creates an {@link IndexRequest} for a document.
     *
     * @param document The document to index.
     * @return The {@link IndexRequest} for a document.
     */
    private IndexRequest createRequest(D document) {
        if(document.getTimestamp() == null) {
            throw new IllegalArgumentException("Document must contain the timestamp");
        }

        return new IndexRequest()
                .source(document.getDocument())
                .type(document.getSensorType() + "_doc")
                .id(document.getGuid())
                .index(document.getIndex())
                .timestamp(document.getTimestamp().toString());
    }
}
