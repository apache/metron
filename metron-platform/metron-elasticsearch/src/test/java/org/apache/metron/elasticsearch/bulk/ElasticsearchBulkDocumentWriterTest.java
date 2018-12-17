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

import org.apache.metron.common.Constants;
import org.apache.metron.elasticsearch.client.ElasticsearchClient;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticsearchBulkDocumentWriterTest {

    ElasticsearchBulkDocumentWriter<Document> writer;
    ElasticsearchClient client;
    RestHighLevelClient highLevelClient;

    @Before
    public void setup() {
        // mock Elasticsearch
        highLevelClient = mock(RestHighLevelClient.class);
        client = mock(ElasticsearchClient.class);
        when(client.getHighLevelClient()).thenReturn(highLevelClient);

        writer = new ElasticsearchBulkDocumentWriter<>(client);
    }

    @Test
    public void testWriteSuccess() throws IOException {
        setupElasticsearchToSucceed();

        // write a document successfully
        Document doc = document(message());
        String index = "bro_index";
        writer.addDocument(doc, index);

        BulkDocumentWriterResults<Document> results = writer.write();
        assertEquals(1, results.getSuccesses().size());
        assertEquals(0, results.getFailures().size());

        WriteSuccess<Document> success = results.getSuccesses().get(0);
        assertEquals(doc, success.getDocument());
    }

    @Test
    public void testWriteFailure() throws IOException {
        setupElasticsearchToFail();

        // the document will fail to write
        Document doc = document(message());
        String index = "bro_index";
        writer.addDocument(doc, index);

        BulkDocumentWriterResults<Document> results = writer.write();
        assertEquals(0, results.getSuccesses().size());
        assertEquals(1, results.getFailures().size());

        WriteFailure<Document> failure = results.getFailures().get(0);
        assertEquals(doc, failure.getDocument());
        assertEquals("error message", failure.getMessage());
        assertNotNull(failure.getCause());
    }

    @Test
    public void testSizeWhenWriteSuccessful() throws IOException {
        setupElasticsearchToSucceed();
        assertEquals(0, writer.size());

        // add some documents to write
        String index = "bro_index";
        writer.addDocument(document(message()), index);
        writer.addDocument(document(message()), index);
        writer.addDocument(document(message()), index);
        writer.addDocument(document(message()), index);
        writer.addDocument(document(message()), index);
        assertEquals(5, writer.size());

        // after the write, all documents should have been flushed
        writer.write();
        assertEquals(0, writer.size());
    }

    @Test
    public void testSizeWhenWriteFails() throws IOException {
        setupElasticsearchToFail();
        assertEquals(0, writer.size());

        // add some documents to write
        String index = "bro_index";
        writer.addDocument(document(message()), index);
        writer.addDocument(document(message()), index);
        writer.addDocument(document(message()), index);
        writer.addDocument(document(message()), index);
        writer.addDocument(document(message()), index);
        assertEquals(5, writer.size());

        // after the write, all documents should have been flushed
        writer.write();
        assertEquals(0, writer.size());
    }

    private void setupElasticsearchToFail() throws IOException {
        final String errorMessage = "error message";
        final Exception cause = new Exception("test exception");
        final boolean isFailed = true;
        final int itemID = 0;

        // define the item failure
        BulkItemResponse.Failure failure = mock(BulkItemResponse.Failure.class);
        when(failure.getCause()).thenReturn(cause);
        when(failure.getMessage()).thenReturn(errorMessage);

        // define the item level response
        BulkItemResponse itemResponse = mock(BulkItemResponse.class);
        when(itemResponse.isFailed()).thenReturn(isFailed);
        when(itemResponse.getItemId()).thenReturn(itemID);

        when(itemResponse.getFailure()).thenReturn(failure);
        when(itemResponse.getFailureMessage()).thenReturn("error message");
        List<BulkItemResponse> itemsResponses = Collections.singletonList(itemResponse);

        // define the bulk response to indicate failure
        BulkResponse response = mock(BulkResponse.class);
        when(response.iterator()).thenReturn(itemsResponses.iterator());
        when(response.hasFailures()).thenReturn(isFailed);

        // have the client return the mock response
        when(highLevelClient.bulk(any(BulkRequest.class))).thenReturn(response);
    }

    private void setupElasticsearchToSucceed() throws IOException {
        final String documentId = UUID.randomUUID().toString();
        final boolean isFailed = false;
        final int itemID = 0;

        // the write response will contain what is used as the document ID
        DocWriteResponse writeResponse = mock(DocWriteResponse.class);
        when(writeResponse.getId()).thenReturn(documentId);

        // define the item level response
        BulkItemResponse itemResponse = mock(BulkItemResponse.class);
        when(itemResponse.isFailed()).thenReturn(isFailed);
        when(itemResponse.getItemId()).thenReturn(itemID);
        when(itemResponse.getResponse()).thenReturn(writeResponse);
        List<BulkItemResponse> itemsResponses = Collections.singletonList(itemResponse);

        // define the bulk response to indicate success
        BulkResponse response = mock(BulkResponse.class);
        when(response.iterator()).thenReturn(itemsResponses.iterator());
        when(response.hasFailures()).thenReturn(isFailed);

        // have the client return the mock response
        when(highLevelClient.bulk(any(BulkRequest.class))).thenReturn(response);
    }

    private Document document(JSONObject message) {
        String guid = UUID.randomUUID().toString();
        String sensorType = "bro";
        Long timestamp = System.currentTimeMillis();
        return new Document(message, guid, sensorType, timestamp);
    }

    private JSONObject message() {
        JSONObject message = new JSONObject();
        message.put(Constants.GUID, UUID.randomUUID().toString());
        message.put(Constants.Fields.TIMESTAMP.getName(), System.currentTimeMillis());
        message.put(Constants.Fields.SRC_ADDR.getName(), "192.168.1.1");
        return message;
    }
}
