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
package org.apache.metron.solr.dao;

import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

public class SolrColumnMetadataTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private SolrColumnMetadataDao solrColumnMetadataDao;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    solrColumnMetadataDao = new SolrColumnMetadataDao(null);
  }

  @Test
  public void getColumnMetadataShouldProperlyReturnColumnMetadata() throws Exception {
    List<Map<String, Object>> broFields = new ArrayList<>();
    broFields.add(new HashMap<String, Object>(){{
      put("name", "string");
      put("type", "string");
    }});
    broFields.add(new HashMap<String, Object>(){{
      put("name", "int");
      put("type", "pint");
    }});
    broFields.add(new HashMap<String, Object>(){{
      put("name", "float");
      put("type", "pfloat");
    }});
    broFields.add(new HashMap<String, Object>(){{
      put("name", "double");
      put("type", "pdouble");
    }});
    broFields.add(new HashMap<String, Object>(){{
      put("name", "boolean");
      put("type", "boolean");
    }});
    broFields.add(new HashMap<String, Object>(){{
      put("name", "broField");
      put("type", "string");
    }});
    broFields.add(new HashMap<String, Object>(){{
      put("name", "conflict");
      put("type", "string");
    }});


    List<Map<String, Object>> snortFields = new ArrayList<>();
    snortFields.add(new HashMap<String, Object>(){{
      put("name", "long");
      put("type", "plong");
    }});
    snortFields.add(new HashMap<String, Object>(){{
      put("name", "snortField");
      put("type", "plong");
    }});
    snortFields.add(new HashMap<String, Object>(){{
      put("name", "unknown");
      put("type", "unknown");
    }});
    broFields.add(new HashMap<String, Object>(){{
      put("name", "conflict");
      put("type", "plong");
    }});

    solrColumnMetadataDao = spy(new SolrColumnMetadataDao(null));
    doReturn(broFields).when(solrColumnMetadataDao).getIndexFields("bro");
    doReturn(snortFields).when(solrColumnMetadataDao).getIndexFields("snort");

    Map<String, FieldType> columnMetadata = solrColumnMetadataDao.getColumnMetadata(Arrays.asList("bro", "snort"));

    assertEquals(FieldType.BOOLEAN, columnMetadata.get("boolean"));
    assertEquals(FieldType.TEXT, columnMetadata.get("string"));
    assertEquals(FieldType.TEXT, columnMetadata.get("broField"));
    assertEquals(FieldType.DOUBLE, columnMetadata.get("double"));
    assertEquals(FieldType.LONG, columnMetadata.get("long"));
    assertEquals(FieldType.FLOAT, columnMetadata.get("float"));
    assertEquals(FieldType.INTEGER, columnMetadata.get("int"));
    assertEquals(FieldType.LONG, columnMetadata.get("snortField"));
    assertEquals(FieldType.OTHER, columnMetadata.get("conflict"));
    assertEquals(FieldType.OTHER, columnMetadata.get("unknown"));

  }

  @Test
  public void getColumnMetadataShouldThrowSolrException() throws Exception {
    exception.expect(IOException.class);
    exception.expectMessage("solr exception");

    solrColumnMetadataDao = spy(new SolrColumnMetadataDao(null));
    doThrow(new SolrServerException("solr exception")).when(solrColumnMetadataDao).getIndexFields("bro");

    solrColumnMetadataDao.getColumnMetadata(Arrays.asList("bro", "snort"));
  }

  @Test
  public void getColumnMetadataShouldHandle400Exception() throws Exception {
    solrColumnMetadataDao = spy(new SolrColumnMetadataDao(null));
    SolrException solrException = new SolrException(SolrException.ErrorCode.BAD_REQUEST, "solr exception");

    doThrow(solrException).when(solrColumnMetadataDao).getIndexFields("bro");

    Map<String, FieldType> columnMetadata = solrColumnMetadataDao.getColumnMetadata(Collections.singletonList("bro"));

    assertNotNull(columnMetadata);
  }

}
