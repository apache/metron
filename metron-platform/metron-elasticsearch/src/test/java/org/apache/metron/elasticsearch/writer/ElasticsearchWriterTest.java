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
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Tuple;
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
import static org.junit.Assert.fail;
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
        // create a writer where all writes will be successful
        float probabilityOfSuccess = 1.0F;
        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        esWriter.setDocumentWriter( new BulkDocumentWriterStub<>(probabilityOfSuccess));
        esWriter.init(stormConf, topologyContext, writerConfiguration);

        // create a tuple and a message associated with that tuple
        List<Tuple> tuples = createTuples(1);
        List<JSONObject> messages = createMessages(1);

        BulkWriterResponse response = esWriter.write("bro", writerConfiguration, tuples, messages);

        // response should only contain successes
        assertFalse(response.hasErrors());
        assertTrue(response.getSuccesses().contains(tuples.get(0)));
    }

    @Test
    public void shouldWriteManySuccessfully() {
        // create a writer where all writes will be successful
        float probabilityOfSuccess = 1.0F;
        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        esWriter.setDocumentWriter(new BulkDocumentWriterStub<>(probabilityOfSuccess));
        esWriter.init(stormConf, topologyContext, writerConfiguration);

        // create a few tuples and the messages associated with the tuples
        List<Tuple> tuples = createTuples(3);
        List<JSONObject> messages = createMessages(3);

        BulkWriterResponse response = esWriter.write("bro", writerConfiguration, tuples, messages);

        // response should only contain successes
        assertFalse(response.hasErrors());
        assertTrue(response.getSuccesses().contains(tuples.get(0)));
        assertTrue(response.getSuccesses().contains(tuples.get(1)));
        assertTrue(response.getSuccesses().contains(tuples.get(2)));
    }

    @Test
    public void shouldHandleWriteFailure() {
        // create a writer where all writes will fail
        float probabilityOfSuccess = 0.0F;
        BulkDocumentWriterStub<TupleBasedDocument> docWriter = new BulkDocumentWriterStub<>(probabilityOfSuccess);

        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        esWriter.setDocumentWriter(docWriter);
        esWriter.init(stormConf, topologyContext, writerConfiguration);

        // create a tuple and a message associated with that tuple
        List<Tuple> tuples = createTuples(1);
        List<JSONObject> messages = createMessages(1);

        BulkWriterResponse response = esWriter.write("bro", writerConfiguration, tuples, messages);

        // response should only contain failures
        assertTrue(response.hasErrors());
        Collection<Tuple> errors = response.getErrors().get(docWriter.getException());
        assertTrue(errors.contains(tuples.get(0)));
    }

    @Test
    public void shouldHandleManyWriteFailures() {
        // create a writer where all writes will fail
        float probabilityOfSuccess = 0.0F;
        BulkDocumentWriterStub<TupleBasedDocument> docWriter = new BulkDocumentWriterStub<>(probabilityOfSuccess);

        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        esWriter.setDocumentWriter(docWriter);
        esWriter.init(stormConf, topologyContext, writerConfiguration);

        // create a few tuples and the messages associated with the tuples
        List<Tuple> tuples = createTuples(3);
        List<JSONObject> messages = createMessages(3);

        BulkWriterResponse response = esWriter.write("bro", writerConfiguration, tuples, messages);

        // response should only contain failures
        assertTrue(response.hasErrors());
        Collection<Tuple> errors = response.getErrors().get(docWriter.getException());
        assertTrue(errors.contains(tuples.get(0)));
        assertTrue(errors.contains(tuples.get(1)));
        assertTrue(errors.contains(tuples.get(2)));
    }

    @Test
    public void shouldHandlePartialFailures() {
        // create a writer where some will fails and some will succeed
        float probabilityOfSuccess = 0.5F;
        BulkDocumentWriterStub<TupleBasedDocument> docWriter = new BulkDocumentWriterStub<>(probabilityOfSuccess);

        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        esWriter.setDocumentWriter(docWriter);
        esWriter.init(stormConf, topologyContext, writerConfiguration);

        // create some tuples and the messages associated with the tuples
        int count = 100;
        List<Tuple> tuples = createTuples(count);
        List<JSONObject> messages = createMessages(count);

        BulkWriterResponse response = esWriter.write("bro", writerConfiguration, tuples, messages);

        // response should contain some successes and some failures
        int successes = response.getSuccesses().size();
        int failures = response.getErrors().get(docWriter.getException()).size();
        assertTrue(response.hasErrors());
        assertTrue(successes > 0);
        assertTrue(failures > 0);
        assertEquals(count, successes + failures);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldCheckIfNumberOfMessagesMatchNumberOfTuples() {
        // create a writer where all writes will be successful
        float probabilityOfSuccess = 1.0F;
        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        esWriter.setDocumentWriter( new BulkDocumentWriterStub<>(probabilityOfSuccess));
        esWriter.init(stormConf, topologyContext, writerConfiguration);

        // there are 5 tuples and only 1 message; there should be 5 messages to match the number of tuples
        List<Tuple> tuples = createTuples(5);
        List<JSONObject> messages = createMessages(1);

        esWriter.write("bro", writerConfiguration, tuples, messages);
        fail("expected exception");
    }

    @Test
    public void shouldWriteSuccessfullyWhenMessageTimestampIsString() {
        // create a writer where all writes will be successful
        float probabilityOfSuccess = 1.0F;
        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        esWriter.setDocumentWriter(new BulkDocumentWriterStub<>(probabilityOfSuccess));
        esWriter.init(stormConf, topologyContext, writerConfiguration);

        // create a message where the timestamp is expressed as a string
        List<Tuple> tuples = createTuples(1);
        List<JSONObject> messages = createMessages(1);
        messages.get(0).put(Constants.Fields.TIMESTAMP.getName(), new Long(System.currentTimeMillis()).toString());

        BulkWriterResponse response = esWriter.write("bro", writerConfiguration, tuples, messages);

        // response should only contain successes
        assertFalse(response.hasErrors());
        assertTrue(response.getSuccesses().contains(tuples.get(0)));
    }

    @Test
    public void shouldWriteSuccessfullyWhenMissingGUID() {
        // create a writer where all writes will be successful
        float probabilityOfSuccess = 1.0F;
        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        esWriter.setDocumentWriter(new BulkDocumentWriterStub<>(probabilityOfSuccess));
        esWriter.init(stormConf, topologyContext, writerConfiguration);

        // create a message where the GUID is missing
        List<Tuple> tuples = createTuples(1);
        List<JSONObject> messages = createMessages(1);
        assertNotNull(messages.get(0).remove(Constants.GUID));

        BulkWriterResponse response = esWriter.write("bro", writerConfiguration, tuples, messages);

        // response should only contain successes
        assertFalse(response.hasErrors());
        assertTrue(response.getSuccesses().contains(tuples.get(0)));
    }

    private JSONObject message() {
        JSONObject message = new JSONObject();
        message.put(Constants.GUID, UUID.randomUUID().toString());
        message.put(Constants.Fields.TIMESTAMP.getName(), System.currentTimeMillis());
        message.put(Constants.Fields.SRC_ADDR.getName(), "192.168.1.1");
        return message;
    }

    private Map<String, Object> globals() {
        Map<String, Object> globals = new HashMap<>();
        globals.put("es.date.format", "yyyy.MM.dd.HH");
        return globals;
    }

    private List<Tuple> createTuples(int count) {
        List<Tuple> tuples = new ArrayList<>();
        for(int i=0; i<count; i++) {
            tuples.add(mock(Tuple.class));
        }
        return tuples;
    }

    private List<JSONObject> createMessages(int count) {
        List<JSONObject> messages = new ArrayList<>();
        for(int i=0; i<count; i++) {
            messages.add(message());
        }
        return messages;
    }
}
