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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.solr.common.SolrDocument;
import org.junit.Test;

public class SolrUtilitiesTest {

  @Test
  public void toDocumentShouldProperlyReturnDocument() throws Exception {
    SolrDocument solrDocument = new SolrDocument();
    solrDocument.addField(SolrDao.VERSION_FIELD, 1.0);
    solrDocument.addField(Constants.GUID, "guid");
    solrDocument.addField(Constants.SENSOR_TYPE, "bro");
    solrDocument.addField("field", "value");

    Document expectedDocument = new Document(new HashMap<String, Object>() {{
      put("field", "value");
      put(Constants.GUID, "guid");
      put(Constants.SENSOR_TYPE, "bro");
    }}, "guid", "bro", 0L);

    Document actualDocument = SolrUtilities.toDocument(solrDocument);
    assertEquals(expectedDocument, actualDocument);
  }
}
