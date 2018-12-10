/*
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

import org.apache.metron.elasticsearch.bulk.BulkDocumentWriter;
import org.apache.metron.elasticsearch.bulk.BulkDocumentWriterResults;
import org.apache.metron.indexing.dao.update.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A {@link BulkDocumentWriter} stub that can pretend that all documents
 * have been written successfully, that all documents have failed, or something
 * in between those two extremes.
 *
 * @param <D> The type of {@link Document} to write.
 */
public class BulkDocumentWriterStub<D extends Document> implements BulkDocumentWriter<D> {

    private float probabilityOfSuccess;
    private Exception exception;
    private List<D> documents;

    /**
     * @param probabilityOfSuccess The probability that a write will succeed.
     *                             If 1.0, all writes succeed.  If 0.0, all writes fail.
     */
    public BulkDocumentWriterStub(float probabilityOfSuccess) {
        this.probabilityOfSuccess = probabilityOfSuccess;
        this.exception = new IllegalStateException("Exception created by a stub for testing");
        this.documents = new ArrayList<>();
    }

    @Override
    public void addDocument(D document, String index) {
        documents.add(document);
    }

    @Override
    public BulkDocumentWriterResults<D> write() {
        BulkDocumentWriterResults<D> results = new BulkDocumentWriterResults<>();
        Random random = new Random();
        for(D document: documents) {
            boolean success = random.nextFloat() <= probabilityOfSuccess;
            if(success) {
                results.addSuccess(document);
            } else {
                results.addFailure(document, exception, "error message");
            }
        }
        return results;
    }

    @Override
    public int size() {
        return documents.size();
    }

    /**
     * Set the exception that is passed to the failure callback when a message fails to write.
     * @param exception The exception passed to the failure callback.
     */
    public void setException(Exception exception) {
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
