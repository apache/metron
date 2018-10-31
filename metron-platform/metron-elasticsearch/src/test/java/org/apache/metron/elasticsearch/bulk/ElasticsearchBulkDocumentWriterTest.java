package org.apache.metron.elasticsearch.bulk;

import org.apache.metron.common.Constants;
import org.apache.metron.elasticsearch.client.ElasticsearchClient;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ElasticsearchBulkDocumentWriterTest {

    ElasticsearchBulkDocumentWriter<Document> writer;
    ElasticsearchClient client;
    RestHighLevelClient highLevelClient;
    ArgumentCaptor<BulkRequest> argumentCaptor;
    boolean onSuccessCalled;
    boolean onFailureCalled;

    @Before
    public void setup() {
        // initial setup to mock Elasticsearch
        highLevelClient = mock(RestHighLevelClient.class);
        client = mock(ElasticsearchClient.class);
        when(client.getHighLevelClient()).thenReturn(highLevelClient);

        writer = new ElasticsearchBulkDocumentWriter<>(client);
        onFailureCalled = false;
        onSuccessCalled = false;
    }

    @Test
    public void testSuccessCallback() throws IOException {
        setupElasticsearchToSucceed();

        // create a document to write
        List<Document> documents = new ArrayList<>();
        documents.add(document(message()));

        // validate the "on success" callback
        writer.onSuccess(successfulDocs -> {
            onSuccessCalled = true;
            assertEquals(documents, successfulDocs);
        });

        writer.write(documents);
        assertTrue(onSuccessCalled);
        assertFalse(onFailureCalled);
    }

    @Test
    public void testSuccessWithNoCallbacks() throws IOException {
        setupElasticsearchToSucceed();

        // create a document to write
        List<Document> documents = new ArrayList<>();
        documents.add(document(message()));

        // no callbacks defined
        writer.write(documents);
        assertFalse(onSuccessCalled);
        assertFalse(onFailureCalled);
    }

    @Test
    public void testFailureCallback() throws IOException {
        setupElasticsearchToFail();

        // create a document to write
        List<Document> documents = new ArrayList<>();
        documents.add(document(message()));

        // validate the "on failure" callback
        writer.onFailure((failedDoc, cause, msg) -> {
            onFailureCalled = true;
            assertEquals(documents.get(0), failedDoc);
        });

        // no callbacks defined
        writer.write(documents);
        assertFalse(onSuccessCalled);
        assertTrue(onFailureCalled);
    }

    @Test
    public void testFailureWithNoCallbacks() throws IOException {
        setupElasticsearchToFail();

        // create a document to write
        List<Document> documents = new ArrayList<>();
        documents.add(document(message()));

        // validate the "on failure" callback
        writer.write(documents);
        assertFalse(onSuccessCalled);
        assertFalse(onFailureCalled);
    }

    @Test
    public void testDocumentWithIndex() throws IOException {
        setupElasticsearchToSucceed();

        // create a document that does not contain a timestamp
        final String indexName = "test_index_foo";
        Document document = document(message());
        document.setIndex(Optional.of(indexName));

        List<Document> documents = new ArrayList<>();
        documents.add(document);

        // validate the "on success" callback
        writer.onSuccess(successfulDocs -> {
            onSuccessCalled = true;
            assertEquals(documents, successfulDocs);
        });

        writer.write(documents);
        assertTrue(onSuccessCalled);
        assertFalse(onFailureCalled);

        // capture the bulk request that is submitted to elasticsearch
        argumentCaptor = ArgumentCaptor.forClass(BulkRequest.class);
        verify(highLevelClient).bulk(argumentCaptor.capture());

        // ensure the index name was set on the request
        BulkRequest request = argumentCaptor.getValue();
        assertEquals(indexName, request.requests().get(0).index());
    }

    private void setupElasticsearchToFail() throws IOException {
        // define the item failure
        BulkItemResponse.Failure failure = mock(BulkItemResponse.Failure.class);
        when(failure.getCause()).thenReturn(new Exception("test exception"));
        when(failure.getMessage()).thenReturn("error message");

        // define the item level response
        BulkItemResponse itemResponse = mock(BulkItemResponse.class);
        when(itemResponse.isFailed()).thenReturn(true);
        when(itemResponse.getItemId()).thenReturn(0);
        when(itemResponse.getFailure()).thenReturn(failure);
        List<BulkItemResponse> itemsResponses = Collections.singletonList(itemResponse);

        // define the bulk response to indicate failure
        BulkResponse response = mock(BulkResponse.class);
        when(response.iterator()).thenReturn(itemsResponses.iterator());
        when(response.hasFailures()).thenReturn(true);

        // have the client return the mock response
        when(highLevelClient.bulk(any(BulkRequest.class))).thenReturn(response);
    }

    private void setupElasticsearchToSucceed() throws IOException {
        // define the bulk response to indicate success
        BulkResponse response = mock(BulkResponse.class);
        when(response.hasFailures()).thenReturn(false);

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
