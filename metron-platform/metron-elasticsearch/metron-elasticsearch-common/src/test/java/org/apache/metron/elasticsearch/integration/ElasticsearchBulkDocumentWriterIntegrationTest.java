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

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.elasticsearch.bulk.ElasticsearchBulkDocumentWriter;
import org.apache.metron.elasticsearch.client.ElasticsearchClient;
import org.apache.metron.elasticsearch.client.ElasticsearchClientFactory;
import org.apache.metron.elasticsearch.dao.ElasticsearchRetrieveLatestDao;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.Response;
import org.hamcrest.CoreMatchers;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ElasticsearchBulkDocumentWriterIntegrationTest {

    @ClassRule
    public static TemporaryFolder indexDir = new TemporaryFolder();
    private static String broTemplatePath = "../../../metron-deployment/packaging/ambari/metron-mpack/src/main/resources/common-services/METRON/CURRENT/package/files/bro_index.template";
    private static ElasticSearchComponent elasticsearch;
    private ElasticsearchClient client;
    private ElasticsearchBulkDocumentWriter<Document> writer;
    private ElasticsearchRetrieveLatestDao retrieveDao;

    @BeforeClass
    public static void setupElasticsearch() throws Exception {
        AccessConfig accessConfig = new AccessConfig();
        accessConfig.setGlobalConfigSupplier(() -> globals());

        elasticsearch = new ElasticSearchComponent.Builder()
                .withHttpPort(9211)
                .withIndexDir(indexDir.getRoot())
                .withAccessConfig(accessConfig)
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
    public void setup() throws Exception {
        client = ElasticsearchClientFactory.create(globals());
        retrieveDao = new ElasticsearchRetrieveLatestDao(client);
        writer = new ElasticsearchBulkDocumentWriter<>(client)
                .withRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);

        // add bro template
        JSONObject broTemplate = JSONUtils.INSTANCE.load(new File(broTemplatePath), JSONObject.class);
        String broTemplateJson = JSONUtils.INSTANCE.toJSON(broTemplate, true);
        HttpEntity broEntity = new NStringEntity(broTemplateJson, ContentType.APPLICATION_JSON);
        Response response = client
                .getLowLevelClient()
                .performRequest("PUT", "/_template/bro_template", Collections.emptyMap(), broEntity);
        assertThat(response.getStatusLine().getStatusCode(), CoreMatchers.equalTo(200));
    }

    @After
    public void tearDown() throws IOException {
        if(client != null) {
            client.close();
        }
    }

    @Test
    public void testWrite() throws Exception {
        // create some documents to write
        List<Document> documents = new ArrayList<>();
        for(int i=0; i<10; i++) {
            Document document = Document.fromJSON(createMessage());
            documents.add(document);
        }

        // write the documents
        for(Document doc: documents) {
            writer.addDocument(doc, "bro_index");
        }
        writer.write();

        // ensure the documents were written
        for(Document expected: documents) {
            Document actual = retrieveDao.getLatest(expected.getGuid(), expected.getSensorType());
            assertNotNull("No document found", actual);
            assertEquals(expected.getGuid(), actual.getGuid());
            assertEquals(expected.getSensorType(), actual.getSensorType());
            assertEquals(expected.getDocument(), actual.getDocument());
            assertTrue(actual.getDocumentID().isPresent());

            // the document ID and GUID should not be the same, since the document ID was auto-generated
            assertNotEquals(actual.getDocument(), actual.getGuid());
        }
    }

    private static Map<String, Object> globals() {
        Map<String, Object> globals = new HashMap<>();
        globals.put("es.clustername", "metron");
        globals.put("es.ip", "localhost");
        globals.put("es.port", "9200");
        globals.put("es.date.format", "yyyy.MM.dd.HH");
        return globals;
    }

    private JSONObject createMessage() {
        JSONObject message = new JSONObject();
        message.put(Constants.GUID, UUID.randomUUID().toString());
        message.put(Constants.Fields.TIMESTAMP.getName(), System.currentTimeMillis());
        message.put(Constants.Fields.SRC_ADDR.getName(), "192.168.1.1");
        message.put("source:type", "bro");
        return message;
    }
}
