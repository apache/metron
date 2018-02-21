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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.collections4.MapUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.MetaAlertDao;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

public class SolrUtilities {
  public static SearchResult getSearchResult(SolrDocument solrDocument,
      Optional<List<String>> fields) {
    SearchResult searchResult = new SearchResult();
    searchResult.setId((String) solrDocument.getFieldValue(Constants.GUID));
    Map<String, Object> docSource = toDocument(solrDocument).getDocument();
    final Map<String, Object> source = new HashMap<>();
    if (fields.isPresent()) {
      fields.get().forEach(field -> source.put(field, docSource.get(field)));
    } else {
      source.putAll(docSource);
    }
    searchResult.setSource(source);
    return searchResult;
  }

  public static Document toDocument(SolrDocument solrDocument) {
    Map<String, Object> document = new HashMap<>();
    solrDocument.getFieldNames().stream()
        .filter(name -> !name.equals(SolrDao.VERSION_FIELD))
        .forEach(name -> document.put(name, solrDocument.getFieldValue(name)));
    // Make sure to put child alerts in
    // TODO fix this whole replace nonsense.
    if (solrDocument.hasChildDocuments() && solrDocument
        .getFieldValue(Constants.SENSOR_TYPE.replace('.', ':'))
        .equals(SolrMetaAlertDao.METAALERT_TYPE)) {
      List<Map<String, Object>> childDocuments = new ArrayList<>();
      for (SolrDocument childDoc : solrDocument.getChildDocuments()) {
        Map<String, Object> childDocMap = new HashMap<>();
        childDoc.getFieldNames().stream()
            .filter(name -> !name.equals(SolrDao.VERSION_FIELD))
            .forEach(name -> childDocMap.put(name, childDoc.getFieldValue(name)));
        childDocuments.add(childDocMap);
      }

      document.put(MetaAlertDao.ALERT_FIELD, childDocuments);
    }
    // TODO fix this whole replace nonsense.
    return new Document(document,
        (String) solrDocument.getFieldValue(Constants.GUID),
        (String) solrDocument.getFieldValue(Constants.SENSOR_TYPE.replace('.', ':')),
        0L);
  }

  public static SolrInputDocument toSolrInputDocument(Document document) {
    SolrInputDocument solrInputDocument = new SolrInputDocument();
    for (Map.Entry<String, Object> field : document.getDocument().entrySet()) {
      if (field.getKey().equals(MetaAlertDao.ALERT_FIELD)) {
        // We have a children, that needs to be translated as a child doc, not a field.
        List<Map<String, Object>> alerts = (List<Map<String, Object>>) field.getValue();
        for (Map<String, Object> alert : alerts) {
          SolrInputDocument childDocument = new SolrInputDocument();
          for (Map.Entry<String, Object> alertField : alert.entrySet()) {
            childDocument.addField(alertField.getKey(), alertField.getValue());
          }
          solrInputDocument.addChildDocument(childDocument);
        }
      } else {
        solrInputDocument.addField(field.getKey(), field.getValue());
      }
    }
    return solrInputDocument;
  }
}
