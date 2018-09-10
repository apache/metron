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
package org.apache.metron.indexing.dao;

import org.apache.metron.indexing.dao.metaalert.MetaAlertConfig;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertRetrieveLatestDao;
import org.apache.metron.indexing.dao.metaalert.lucene.AbstractLuceneMetaAlertUpdateDao;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class InMemoryMetaAlertUpdateDao extends AbstractLuceneMetaAlertUpdateDao {

  private IndexDao indexDao;

  public InMemoryMetaAlertUpdateDao(
          IndexDao indexDao,
          MetaAlertRetrieveLatestDao retrieveLatestDao,
          MetaAlertConfig config,
          int pageSize
  ) {
    super(indexDao, retrieveLatestDao, config);
    this.indexDao = indexDao;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Document createMetaAlert(MetaAlertCreateRequest request) throws InvalidCreateException, IOException {
    List<GetRequest> alertRequests = request.getAlerts();
    if (alertRequests.isEmpty()) {
      return null;
    }
    // Retrieve the documents going into the meta alert and build it
    Iterable<Document> alerts = indexDao.getAllLatest(alertRequests);

    Document metaAlert = buildCreateDocument(alerts, request.getGroups(),
            MetaAlertConstants.ALERT_FIELD);

    metaAlert.getDocument()
            .put(getConfig().getSourceTypeField(), MetaAlertConstants.METAALERT_TYPE);

    return metaAlert;
  }

  @Override
  public Document update(Document update, Optional<String> index) throws IOException {
    return indexDao.update(update, index);
  }

  @Override
  public Document addCommentToAlert(CommentAddRemoveRequest request) throws IOException {
    return null;
  }

  @Override
  public Document removeCommentFromAlert(CommentAddRemoveRequest request) throws IOException {
    return null;
  }

  @Override
  public Document addCommentToAlert(CommentAddRemoveRequest request, Document latest) throws IOException {
    return null;
  }

  @Override
  public Document removeCommentFromAlert(CommentAddRemoveRequest request, Document latest) throws IOException {
    return null;
  }
}
