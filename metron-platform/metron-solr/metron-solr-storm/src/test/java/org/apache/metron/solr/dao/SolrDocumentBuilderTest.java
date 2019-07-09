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
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.search.AlertComment;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.solr.common.SolrDocument;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
  public void shouldCreateDocument() {
    final String expectedGUID = UUID.randomUUID().toString();
    final String expectedIP = "192.168.1.12";
    SolrDocument original = createSolrDocument(expectedGUID, expectedIP);

    // 'deserialize' a document retrieved from Solr; SolrDocument -> Document
    Document actual = builder.toDocument(original);
    assertEquals(expectedGUID, actual.getGuid());
    assertEquals(expectedGUID, actual.getDocument().get(Constants.GUID));
    assertEquals("bro", actual.getSensorType());
    assertEquals("bro", actual.getDocument().get(Constants.SENSOR_TYPE));
    assertEquals(123456789L, (long) actual.getTimestamp());
    assertEquals(123456789L, actual.getDocument().get(Constants.Fields.TIMESTAMP.getName()));
    assertEquals(expectedIP, actual.getDocument().get("ip_src_addr"));
  }

  @Test
  public void shouldCreateSolrDocument() {
    Document original = createDocument();

    // 'serialize' a document so that it can be written to Solr; Document -> SolrDocument
    SolrDocument solrDocument = builder.fromDocument(original);

    // validate the Solr document
    assertFalse(solrDocument.hasChildDocuments());
    assertFalse(solrDocument.isEmpty());
    assertEquals("192.168.1.12", solrDocument.get("ip_src_addr"));
    assertEquals("192.168.1.24", solrDocument.get("ip_dst_addr"));
    assertEquals(original.getTimestamp(), solrDocument.get("timestamp"));

    // 'deserialize' the SolrDocument should result in the original; SolrDocument -> Document
    assertEquals(original, builder.toDocument(solrDocument));
  }

  @Test
  public void shouldHandleComments() {
    Document expected = createDocument();
    expected.addComment(new AlertComment("This is a comment.", "johndoe", 12345678));
    expected.addComment(new AlertComment("This is also a comment.", "johndoe", 23456789));

    SolrDocument solrDocument = builder.fromDocument(expected);
    assertNotNull(solrDocument.get(IndexDao.COMMENTS_FIELD));

    Document actual = builder.toDocument(solrDocument);
    assertEquals(expected.getComments(), actual.getComments());
    assertEquals(expected, actual);
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
    solrDocument.addField(Constants.GUID, guid);
    solrDocument.addField(Constants.SENSOR_TYPE, "bro");
    solrDocument.addField(Constants.Fields.TIMESTAMP.getName(), 123456789L);
    solrDocument.addField("ip_src_addr", ipAddress);
    return solrDocument;
  }

  private Document createDocument() {
    Map<String, Object> fields = new HashMap<String, Object>() {{
      put(Constants.Fields.SRC_ADDR.getName(), "192.168.1.12");
      put(Constants.Fields.DST_ADDR.getName(), "192.168.1.24");
      put(Constants.Fields.TIMESTAMP.getName(), System.currentTimeMillis());
      put(Constants.Fields.GUID.getName(), UUID.randomUUID().toString());
      put(Constants.Fields.SENSOR_TYPE.getName(), "bro");
    }};
    return Document.fromJSON(fields);
  }
}
