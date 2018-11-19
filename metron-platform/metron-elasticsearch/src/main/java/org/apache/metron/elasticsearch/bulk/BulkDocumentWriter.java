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

import org.apache.metron.indexing.dao.update.Document;

import java.util.List;

/**
 * Writes documents to an index in bulk.
 *
 * <p>Partial failures within a batch can be handled individually by registering
 * a {@link FailureListener}.
 *
 * @param <D> The type of document to write.
 */
public interface BulkDocumentWriter<D extends Document> {

    /**
     * A listener that is notified when a set of documents have been
     * written successfully.
     * @param <D> The type of document to write.
     */
    interface SuccessListener<D extends Document> {
        void onSuccess(List<D> documents);
    }

    /**
     * A listener that is notified when a document has failed to write.
     * @param <D> The type of document to write.
     */
    interface FailureListener<D extends Document> {
        void onFailure(D failedDocument, Throwable cause, String message);
    }

    /**
     * Register a listener that is notified when a document is successfully written.
     * @param onSuccess The listener to notify.
     */
    void onSuccess(SuccessListener<D> onSuccess);

    /**
     * Register a listener that is notified when a document fails to write.
     * @param onFailure The listener to notify.
     */
    void onFailure(FailureListener<D> onFailure);

    /**
     * Add a document to the batch.
     * @param document The document to write.
     * @param index The name of the index to write to.
     */
    void addDocument(D document, String index);

    /**
     * @return The number of documents waiting to be written.
     */
    int size();

    /**
     * Write all documents in the batch.
     */
    void write();
}