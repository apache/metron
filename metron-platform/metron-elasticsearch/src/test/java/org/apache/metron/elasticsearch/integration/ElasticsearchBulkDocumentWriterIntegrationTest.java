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
package org.apache.metron.elasticsearch.integration;

import org.apache.metron.common.Constants;
import org.apache.metron.elasticsearch.bulk.ElasticsearchBulkDocumentWriter;
import org.apache.metron.elasticsearch.client.ElasticsearchClient;
import org.apache.metron.elasticsearch.client.ElasticsearchClientFactory;
import org.apache.metron.elasticsearch.dao.ElasticsearchRetrieveLatestDao;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.integration.UnableToStartException;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.metron.integration.utils.TestUtils.assertEventually;
import static org.junit.Assert.assertEquals;

public class ElasticsearchBulkDocumentWriterIntegrationTest {

    @ClassRule
    public static TemporaryFolder indexDir = new TemporaryFolder();
    static ElasticSearchComponent elasticsearch;
    ElasticsearchClient client;
    ElasticsearchBulkDocumentWriter<Document> writer;
    ElasticsearchRetrieveLatestDao retrieveDao;

    @BeforeClass
    public static void setupElasticsearch() throws UnableToStartException {
        elasticsearch = new ElasticSearchComponent.Builder()
                .withHttpPort(9211)
                .withIndexDir(indexDir.getRoot())
                .build();
        elasticsearch.start();
    }

    @AfterClass
    public static void tearDownElasticsearch() {
        if(elasticsearch != null) {
            elasticsearch.stop();
        }
    }

    @Before
    public void setup(){
        client = ElasticsearchClientFactory.create(globals());
        writer = new ElasticsearchBulkDocumentWriter<>(client);
        retrieveDao = new ElasticsearchRetrieveLatestDao(client);
    }

    @After
    public void tearDown() throws IOException {
        if(client != null) {
            client.close();
        }
    }

    @Test
    public void testWriteOne() throws Exception {
        Document toWrite = createDocument(createMessage());
        writer.addDocument(toWrite, "bro_index");
        writer.write();
        assertEventually(() -> assertEquals(toWrite, retrieveDao.getLatest(toWrite.getGuid(), "bro")));
    }

    @Test
    public void testWriteAFew() throws Exception {
        // create some documents to write
        List<Document> documents = new ArrayList<>();
        for(int i=0; i<3; i++) {
            documents.add(createDocument(createMessage()));
        }

        // write the documents
        for(Document doc: documents) {
            writer.addDocument(doc, "bro_index");
        }
        writer.write();

        // ensure the documents were indexed
        for(Document doc: documents) {
            assertEventually(() -> assertEquals(doc, retrieveDao.getLatest(doc.getGuid(), "bro")));
        }
    }

    Map<String, Object> globals() {
        Map<String, Object> globals = new HashMap<>();
        globals.put("es.clustername", "metron");
        globals.put("es.ip", "localhost");
        globals.put("es.port", "9200");
        globals.put("es.date.format", "yyyy.MM.dd.HH");
        return globals;
    }

    private Document createDocument(JSONObject message) {
        String guid = UUID.randomUUID().toString();
        String sensorType = "bro";
        Long timestamp = System.currentTimeMillis();
        return new Document(message, guid, sensorType, timestamp);
    }

    private JSONObject createMessage() {
        JSONObject message = new JSONObject();
        message.put(Constants.GUID, UUID.randomUUID().toString());
        message.put(Constants.Fields.TIMESTAMP.getName(), System.currentTimeMillis());
        message.put(Constants.Fields.SRC_ADDR.getName(), "192.168.1.1");
        return message;
    }
}
