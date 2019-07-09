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
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Responsible for building a {@link Document} from a {@link SolrDocument}.
 */
public class SolrDocumentBuilder implements DocumentBuilder<SolrDocument>, Serializable {

  @Override
  public SolrDocument fromDocument(Document document) {
    SolrDocument solrDocument = new SolrDocument();
    for (Map.Entry<String, Object> field : document.getDocument().entrySet()) {
      if (field.getKey().equals(MetaAlertConstants.ALERT_FIELD)) {
        // We have a children, that needs to be translated as a child doc, not a field.
        List<Map<String, Object>> alerts = (List<Map<String, Object>>) field.getValue();
        for (Map<String, Object> alert : alerts) {
          SolrDocument childDocument = new SolrDocument();
          for (Map.Entry<String, Object> alertField : alert.entrySet()) {
            childDocument.addField(alertField.getKey(), alertField.getValue());
          }
          solrDocument.addChildDocument(childDocument);
        }
      } else {
        solrDocument.addField(field.getKey(), field.getValue());
      }
    }
    return solrDocument;
  }

  @Override
  public Document toDocument(SolrDocument solrDocument) {
    Map<String, Object> fields = new HashMap<>();
    solrDocument.getFieldNames()
            .stream()
            .filter(name -> !name.equals(SolrDao.VERSION_FIELD))
            .forEach(name -> fields.put(name, solrDocument.getFieldValue(name)));

    insertChildAlerts(solrDocument, fields);

    String guid = (String) solrDocument.getFieldValue(Constants.GUID);
    String sensorType = (String) solrDocument.getFieldValue(Constants.SENSOR_TYPE);
    return new Document(fields, guid, sensorType, getTimestamp(solrDocument));
  }

  private Long getTimestamp(SolrDocument solrDocument) {
    Long timestamp;
    Object fieldValue = solrDocument.getFieldValue(Constants.Fields.TIMESTAMP.getName());
    if(fieldValue instanceof Long) {
      timestamp = (Long) fieldValue;
    } else if(fieldValue instanceof String) {
      timestamp = Long.valueOf((String) fieldValue);
    } else {
      throw new IllegalStateException(String.format("Unexpected value; expected String or Long; field=%s, type=%s",
              Constants.Fields.TIMESTAMP.getName(), fieldValue.getClass().getName()));
    }
    return timestamp;
  }

  public static SolrInputDocument toSolrInputDocument(SolrDocument in) {
    SolrInputDocument out = new SolrInputDocument();

    // copy fields
    for(String name: in.getFieldNames()) {
      out.addField( name, in.getFieldValue(name), 1.0f);
    }

    // copy children documents
    if(in.getChildDocuments() != null) {
      for(SolrDocument childDocument: in.getChildDocuments()) {
        // check to avoid potential infinite loops; not sure if this is necessary
        if(!childDocument.equals(in)) {
          out.addChildDocument(toSolrInputDocument(childDocument));
        }
      }
    }
    return out;
  }

  protected static void insertChildAlerts(SolrDocument solrDocument, Map<String, Object> document) {
    // Make sure to put child alerts in
    if (solrDocument.hasChildDocuments() && isMetaAlert(solrDocument)) {
      List<Map<String, Object>> childDocuments = new ArrayList<>();
      for (SolrDocument childDoc : solrDocument.getChildDocuments()) {
        Map<String, Object> childDocMap = new HashMap<>();
        childDoc.getFieldNames()
                .stream()
                .filter(name -> !name.equals(SolrDao.VERSION_FIELD))
                .forEach(name -> childDocMap.put(name, childDoc.getFieldValue(name)));
        childDocuments.add(childDocMap);
      }

      document.put(MetaAlertConstants.ALERT_FIELD, childDocuments);
    }
  }

  private static boolean isMetaAlert(SolrDocument document) {
    Object sensorType = document.getFieldValue(Constants.SENSOR_TYPE);
    return Objects.equals(sensorType, MetaAlertConstants.METAALERT_TYPE);
  }
}
