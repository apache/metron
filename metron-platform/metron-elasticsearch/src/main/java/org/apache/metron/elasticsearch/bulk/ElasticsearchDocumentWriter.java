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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ElasticsearchDocumentWriter<D extends Document> implements BulkDocumentWriter<D> {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private SuccessCallback onSuccess;
    private FailureCallback onFailure;
    private ElasticsearchClient client;

    public ElasticsearchDocumentWriter(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public void onSuccess(SuccessCallback<D> onSuccess) {
        this.onSuccess = onSuccess;
    }

    @Override
    public void onFailure(FailureCallback<D> onFailure) {
        this.onFailure = onFailure;
    }

    @Override
    public void write(List<D> documents) {
        try {
            // create an index request for each document
            List<DocWriteRequest> requests = createIndexRequests(documents);

            // create one bulk request for all the documents
            BulkRequest bulkRequest = new BulkRequest();
            bulkRequest.add(requests);

            // handle the bulk response
            BulkResponse bulkResponse = client.getHighLevelClient().bulk(bulkRequest);
            List<Document> successful = handleBulkResponse(bulkResponse, documents);

            // notify the success callback
            onSuccess.onSuccess(successful);
            LOG.debug("Wrote document(s) to Elasticsearch; batchSize={}, success={}, failed={}, tookInMillis={}",
                    documents.size(), successful.size(), documents.size() - successful.size(), bulkResponse.getTookInMillis());

        } catch(IOException e) {
            // failed to submit bulk request; all documents failed
            for(Document failed: documents) {
                onFailure.onFailure(failed, e, ExceptionUtils.getRootCauseMessage(e));
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
    private List<Document> handleBulkResponse(BulkResponse bulkResponse, List<D> documents) {
        List<Document> successful = new ArrayList<>();
        if (bulkResponse.hasFailures()) {

            // interrogate the response to distinguish between those that succeeded and those that failed
            Iterator<BulkItemResponse> iterator = bulkResponse.iterator();
            while(iterator.hasNext()) {
                BulkItemResponse response = iterator.next();
                if(response.isFailed()) {
                    // request failed
                    Document failed = documents.get(response.getItemId());
                    Exception cause = response.getFailure().getCause();
                    String message = response.getFailureMessage();
                    onFailure.onFailure(failed, cause, message);

                } else {
                    // request succeeded
                    Document success = documents.get(response.getItemId());
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
     * Creates an {@link IndexRequest} for each {@link Document}.
     *
     * @param documents The list of documents to write.
     * @return A list of requests; one for each document.
     */
    private List<DocWriteRequest> createIndexRequests(List<D> documents) {
        List<DocWriteRequest> requests = new ArrayList<>();

        // create a request for each document
        for(Document document: documents) {
            IndexRequest request = new IndexRequest()
                    .source(document.getDocument())
                    .type(document.getSensorType() + "_doc")
                    .id(document.getGuid());

            // the index name may not be defined
            document.getIndex().ifPresent(name -> request.index(name));

            // the timestamp may not be defined
            if(document.getTimestamp() != null) {
                request.timestamp(document.getTimestamp().toString());
            }

            requests.add(request);
        }

        return requests;
    }
}
