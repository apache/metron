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
package org.apache.metron.solr.dao;

import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.solr.common.SolrDocument;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Tests the {@link SolrDocumentBuilder}.
 */
public class SolrDocumentBuilderTest {

  private SolrDocumentBuilder builder;

  @Before
  public void setup() {
    builder = new SolrDocumentBuilder();
  }

  @Test
  public void shouldCreateSolrDocument() {
    Document document = document();
    SolrDocument solrDocument = builder.fromDocument(document);

    assertFalse(solrDocument.hasChildDocuments());
    assertFalse(solrDocument.isEmpty());
    assertEquals("192.168.1.12", solrDocument.get("ip_src_addr"));
    assertEquals("192.168.1.24", solrDocument.get("ip_dst_addr"));
    assertEquals("123456789", solrDocument.get("timestamp"));
  }

  @Test
  public void shouldCreateDocumentFromSolr() {
    final String expectedGUID = UUID.randomUUID().toString();
    final String expectedIP = "192.168.1.12";
    SolrDocument solrDocument = createSolrDocument(expectedGUID, expectedIP);

    Document actual = builder.toDocument(solrDocument);
    assertEquals(expectedGUID, actual.getGuid());
    assertEquals(expectedGUID, actual.getDocument().get(Constants.GUID));
    assertEquals("bro", actual.getSensorType());
    assertEquals("bro", actual.getDocument().get(Constants.SENSOR_TYPE));
    assertEquals(123456789L, (long) actual.getTimestamp());
    assertEquals(123456789L, actual.getDocument().get(Constants.Fields.TIMESTAMP.getName()));
    assertEquals(expectedIP, actual.getDocument().get("ip_src_addr"));
  }

  @Test
  public void shouldHandleMissingSensorType() {
    final String expectedGUID = UUID.randomUUID().toString();
    final String expectedIP = "192.168.1.12";
    SolrDocument solrDocument = createSolrDocument(expectedGUID, expectedIP);

    // create a child alert so that the document represents a meta-alert
    SolrDocument childAlert = new SolrDocument();
    solrDocument.addChildDocument(childAlert);

    // remove the sensor type field to create a malformed document
    solrDocument.removeFields(Constants.SENSOR_TYPE);

    Document actual = builder.toDocument(solrDocument);
    assertEquals(expectedGUID, actual.getGuid());
    assertEquals(expectedGUID, actual.getDocument().get(Constants.GUID));
    assertEquals(123456789L, (long) actual.getTimestamp());
    assertEquals(123456789L, actual.getDocument().get(Constants.Fields.TIMESTAMP.getName()));
    assertEquals(expectedIP, actual.getDocument().get("ip_src_addr"));

    // the sensor type is unknown
    assertNull(actual.getSensorType());
    assertNull(actual.getDocument().get(Constants.SENSOR_TYPE));
  }

  private SolrDocument createSolrDocument(String guid, String ipAddress) {
    SolrDocument solrDocument = new SolrDocument();
    solrDocument.addField(SolrDao.VERSION_FIELD, 1.0);
    solrDocument.addField(Constants.GUID, guid);
    solrDocument.addField(Constants.SENSOR_TYPE, "bro");
    solrDocument.addField(Constants.Fields.TIMESTAMP.getName(), 123456789L);
    solrDocument.addField("ip_src_addr", ipAddress);
    return solrDocument;
  }

  private Document document() {
    return new Document(fields(), UUID.randomUUID().toString(), "bro", System.currentTimeMillis());
  }

  private Map<String, Object> fields() {
    return new HashMap<String, Object>() {{
      put("ip_src_addr", "192.168.1.12");
      put("ip_dst_addr", "192.168.1.24");
      put("timestamp", "123456789");
    }};
  }
}
