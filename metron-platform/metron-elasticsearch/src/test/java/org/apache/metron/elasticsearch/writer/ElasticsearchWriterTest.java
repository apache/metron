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

import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.metron.common.writer.MessageId;
import org.apache.metron.elasticsearch.bulk.BulkDocumentWriter;
import org.apache.metron.elasticsearch.bulk.BulkDocumentWriterResults;
import org.apache.storm.task.TopologyContext;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticsearchWriterTest {

    Map stormConf;
    TopologyContext topologyContext;
    WriterConfiguration writerConfiguration;

    @Before
    public void setup() {
        topologyContext = mock(TopologyContext.class);

        writerConfiguration = mock(WriterConfiguration.class);
        when(writerConfiguration.getGlobalConfig()).thenReturn(globals());

        stormConf = new HashMap();
    }

    @Test
    public void shouldWriteSuccessfully() {
        // create a message id and a message associated with that id
        List<BulkMessage<JSONObject>> messages = createMessages(1);

        // create a document writer which will successfully write all
        BulkDocumentWriterResults<MessageIdBasedDocument> results = new BulkDocumentWriterResults<>();
        results.addSuccess(createDocument(messages.get(0)));
        BulkDocumentWriter<MessageIdBasedDocument> docWriter = mock(BulkDocumentWriter.class);
        when(docWriter.write()).thenReturn(results);

        // attempt to write
        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        esWriter.setDocumentWriter(docWriter);
        esWriter.init(stormConf, topologyContext, writerConfiguration);
        BulkWriterResponse response = esWriter.write("bro", writerConfiguration, messages);

        // response should only contain successes
        assertFalse(response.hasErrors());
        assertTrue(response.getSuccesses().contains(new MessageId("message1")));
    }

    @Test
    public void shouldWriteManySuccessfully() {
        // create a few message ids and the messages associated with the ids
        List<BulkMessage<JSONObject>> messages = createMessages(3);

        // create a document writer which will successfully write all
        BulkDocumentWriterResults<MessageIdBasedDocument> results = new BulkDocumentWriterResults<>();
        results.addSuccess(createDocument(messages.get(0)));
        results.addSuccess(createDocument(messages.get(1)));
        results.addSuccess(createDocument(messages.get(2)));
        BulkDocumentWriter<MessageIdBasedDocument> docWriter = mock(BulkDocumentWriter.class);
        when(docWriter.write()).thenReturn(results);

        // attempt to write
        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        esWriter.setDocumentWriter(docWriter);
        esWriter.init(stormConf, topologyContext, writerConfiguration);
        BulkWriterResponse response = esWriter.write("bro", writerConfiguration, messages);

        // response should only contain successes
        assertFalse(response.hasErrors());
        assertTrue(response.getSuccesses().contains(new MessageId("message1")));
        assertTrue(response.getSuccesses().contains(new MessageId("message2")));
        assertTrue(response.getSuccesses().contains(new MessageId("message3")));
    }

    @Test
    public void shouldHandleWriteFailure() {
        // create a message id and a message associated with that id
        List<BulkMessage<JSONObject>> messages = createMessages(3);
        Exception cause = new Exception();

        // create a document writer which will fail all writes
        BulkDocumentWriterResults<MessageIdBasedDocument> results = new BulkDocumentWriterResults<>();
        results.addFailure(createDocument(messages.get(0)), cause, "error");
        BulkDocumentWriter<MessageIdBasedDocument> docWriter = mock(BulkDocumentWriter.class);
        when(docWriter.write()).thenReturn(results);

        // attempt to write
        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        esWriter.setDocumentWriter(docWriter);
        esWriter.init(stormConf, topologyContext, writerConfiguration);
        BulkWriterResponse response = esWriter.write("bro", writerConfiguration, messages);

        // the writer response should only contain failures
        assertEquals(0, response.getSuccesses().size());
        assertEquals(1, response.getErrors().size());
        Collection<MessageId> errors = response.getErrors().get(cause);
        assertTrue(errors.contains(new MessageId("message1")));
    }

    @Test
    public void shouldHandleManyWriteFailures() {
        // create a few message ids and the messages associated with the ids
        int count = 3;
        List<BulkMessage<JSONObject>> messages = createMessages(count);
        Exception cause = new Exception();

        // create a document writer which will fail all writes
        BulkDocumentWriterResults<MessageIdBasedDocument> results = new BulkDocumentWriterResults<>();
        results.addFailure(createDocument(messages.get(0)), cause, "error");
        results.addFailure(createDocument(messages.get(1)), cause, "error");
        results.addFailure(createDocument(messages.get(2)), cause, "error");
        BulkDocumentWriter<MessageIdBasedDocument> docWriter = mock(BulkDocumentWriter.class);
        when(docWriter.write()).thenReturn(results);

        // attempt to write
        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        esWriter.setDocumentWriter(docWriter);
        esWriter.init(stormConf, topologyContext, writerConfiguration);
        BulkWriterResponse response = esWriter.write("bro", writerConfiguration, messages);

        // the writer response should only contain failures
        assertEquals(0, response.getSuccesses().size());
        assertEquals(1, response.getErrors().size());
        Collection<MessageId> errors = response.getErrors().get(cause);
        assertTrue(errors.contains(new MessageId("message1")));
        assertTrue(errors.contains(new MessageId("message2")));
        assertTrue(errors.contains(new MessageId("message3")));
    }

    @Test
    public void shouldHandlePartialFailures() {
        // create a few message ids and the messages associated with the ids
        int count = 2;
        List<BulkMessage<JSONObject>> messages = createMessages(count);
        Exception cause = new Exception();

        // create a document writer that will fail one and succeed the other
        BulkDocumentWriterResults<MessageIdBasedDocument> results = new BulkDocumentWriterResults<>();
        results.addFailure(createDocument(messages.get(0)), cause, "error");
        results.addSuccess(createDocument(messages.get(1)));
        BulkDocumentWriter<MessageIdBasedDocument> docWriter = mock(BulkDocumentWriter.class);
        when(docWriter.write()).thenReturn(results);

        // attempt to write
        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        esWriter.setDocumentWriter(docWriter);
        esWriter.init(stormConf, topologyContext, writerConfiguration);
        BulkWriterResponse response = esWriter.write("bro", writerConfiguration, messages);

        // response should contain some successes and some failures
        assertEquals(1, response.getSuccesses().size());
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().get(cause).contains(new MessageId("message1")));
        assertTrue(response.getSuccesses().contains(new MessageId("message2")));
    }

    @Test
    public void shouldWriteSuccessfullyWhenMessageTimestampIsString() {
        List<BulkMessage<JSONObject>> messages = createMessages(1);
        JSONObject message = messages.get(0).getMessage();

        // the timestamp is a String, rather than a Long
        message.put(Constants.Fields.TIMESTAMP.getName(), new Long(System.currentTimeMillis()).toString());

        // create the document

        String timestamp = (String) message.get(Constants.Fields.TIMESTAMP.getName());
        String guid = (String) message.get(Constants.GUID);
        String sensorType = (String) message.get(Constants.SENSOR_TYPE);
        MessageIdBasedDocument document = new MessageIdBasedDocument(message, guid, sensorType, Long.parseLong(timestamp), new MessageId("message1"));

        // create a document writer which will successfully write that document
        BulkDocumentWriterResults<MessageIdBasedDocument> results = new BulkDocumentWriterResults<>();
        results.addSuccess(document);
        BulkDocumentWriter<MessageIdBasedDocument> docWriter = mock(BulkDocumentWriter.class);
        when(docWriter.write()).thenReturn(results);

        // attempt to write
        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        esWriter.setDocumentWriter(docWriter);
        esWriter.init(stormConf, topologyContext, writerConfiguration);
        BulkWriterResponse response = esWriter.write("bro", writerConfiguration, messages);

        // response should only contain successes
        assertFalse(response.hasErrors());
        assertTrue(response.getSuccesses().contains(new MessageId("message1")));
    }

    @Test
    public void shouldWriteSuccessfullyWhenMissingGUID() {
        // create a message id and a message associated with that tuple
        List<BulkMessage<JSONObject>> messages = createMessages(1);

        // remove the GUID from the message
        assertNotNull(messages.get(0).getMessage().remove(Constants.GUID));

        // create a document writer which will successfully write all
        BulkDocumentWriterResults<MessageIdBasedDocument> results = new BulkDocumentWriterResults<>();
        results.addSuccess(createDocument(messages.get(0)));
        BulkDocumentWriter<MessageIdBasedDocument> docWriter = mock(BulkDocumentWriter.class);
        when(docWriter.write()).thenReturn(results);

        // attempt to write
        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        esWriter.setDocumentWriter(docWriter);
        esWriter.init(stormConf, topologyContext, writerConfiguration);
        BulkWriterResponse response = esWriter.write("bro", writerConfiguration, messages);

        // response should only contain successes
        assertFalse(response.hasErrors());
        assertTrue(response.getSuccesses().contains(new MessageId("message1")));
    }

    private MessageIdBasedDocument createDocument(BulkMessage<JSONObject> bulkWriterMessage) {
        MessageId messageId = bulkWriterMessage.getId();
        JSONObject message = bulkWriterMessage.getMessage();
        Long timestamp = (Long) bulkWriterMessage.getMessage().get(Constants.Fields.TIMESTAMP.getName());
        String guid = (String) message.get(Constants.GUID);
        String sensorType = (String) message.get(Constants.SENSOR_TYPE);
        return new MessageIdBasedDocument(message, guid, sensorType, timestamp, messageId);
    }

    private JSONObject message() {
        JSONObject message = new JSONObject();
        message.put(Constants.GUID, UUID.randomUUID().toString());
        message.put(Constants.Fields.TIMESTAMP.getName(), System.currentTimeMillis());
        message.put(Constants.Fields.SRC_ADDR.getName(), "192.168.1.1");
        message.put(Constants.SENSOR_TYPE, "sensor");
        return message;
    }

    private Map<String, Object> globals() {
        Map<String, Object> globals = new HashMap<>();
        globals.put("es.date.format", "yyyy.MM.dd.HH");
        return globals;
    }

    private List<BulkMessage<JSONObject>> createMessages(int count) {
        List<BulkMessage<JSONObject>> messages = new ArrayList<>();
        for(int i=0; i<count; i++) {
            messages.add(new BulkMessage<>(new MessageId("message" + (i + 1)), message()));
        }
        return messages;
    }
}
