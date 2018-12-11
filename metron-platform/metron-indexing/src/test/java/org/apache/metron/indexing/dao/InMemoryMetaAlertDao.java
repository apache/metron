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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.InMemoryMetaAlertRetrieveLatestDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConfig;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertRetrieveLatestDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.metaalert.MetaAlertUpdateDao;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.apache.metron.common.Constants.SENSOR_TYPE;

public class InMemoryMetaAlertDao implements MetaAlertDao {

  public static final String METAALERT_INDEX = "metaalert_index";

  private IndexDao indexDao;
  private MetaAlertRetrieveLatestDao metaAlertRetrieveLatestDao;
  private MetaAlertUpdateDao metaAlertUpdateDao;
  private int pageSize = 10;

  /**
   * {
   * "indices": ["metaalert"],
   * "query": "metron_alert|guid:${GUID}",
   * "from": 0,
   * "size": 10,
   * "sort": [
   *   {
   *     "field": "guid",
   *     "sortOrder": "desc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String metaAlertsForAlertQuery;

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    return indexDao.search(searchRequest);
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    return indexDao.group(groupRequest);
  }

  @Override
  public void init(AccessConfig config) {
    // Do nothing
  }

  @Override
  public void init(IndexDao indexDao, Optional<String> threatSort) {
    this.indexDao = indexDao;
    this.metaAlertRetrieveLatestDao = new InMemoryMetaAlertRetrieveLatestDao(indexDao);
    Supplier<Map<String, Object>> globalConfigSupplier = () -> new HashMap<>();
    MetaAlertConfig config = new MetaAlertConfig(
            METAALERT_INDEX,
            null,
            globalConfigSupplier
    ) {
      @Override
      protected String getDefaultThreatTriageField() {
        return MetaAlertConstants.THREAT_FIELD_DEFAULT;
      }

      @Override
      protected String getDefaultSourceTypeField() {
        return SENSOR_TYPE;
      }
    };
    this.metaAlertUpdateDao = new InMemoryMetaAlertUpdateDao(indexDao, metaAlertRetrieveLatestDao, config, -1);
    // Ignore threatSort for test.
  }


  @Override
  public Document getLatest(String guid, String sensorType) throws IOException {
    return indexDao.getLatest(guid, sensorType);
  }

  @Override
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
    return indexDao.getAllLatest(getRequests);
  }

  @Override
  public Document update(Document update, Optional<String> index) throws IOException {
    return indexDao.update(update, index);
  }

  @Override
  public Map<Document, Optional<String>> batchUpdate(Map<Document, Optional<String>> updates) {
    throw new UnsupportedOperationException("InMemoryMetaAlertDao can't do bulk updates");
  }

  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> indices)
      throws IOException {
    return indexDao.getColumnMetadata(indices);
  }

  @Override
  public Document addCommentToAlert(CommentAddRemoveRequest request) {
    return null;
  }

  @Override
  public Document removeCommentFromAlert(CommentAddRemoveRequest request) {
    return null;
  }

  @Override
  public Document addCommentToAlert(CommentAddRemoveRequest request, Document latest) {
    return null;
  }

  @Override
  public Document removeCommentFromAlert(CommentAddRemoveRequest request, Document latest) {
    return null;
  }

  @Override
  public Optional<Map<String, Object>> getLatestResult(GetRequest request) throws IOException {
    return indexDao.getLatestResult(request);
  }

  @Override
  public Document patch(RetrieveLatestDao retrieveLatestDao, PatchRequest request,
      Optional<Long> timestamp)
      throws OriginalNotFoundException, IOException {
    return indexDao.patch(retrieveLatestDao, request, timestamp);
  }

  @Override
  public SearchResponse getAllMetaAlertsForAlert(String guid) throws InvalidSearchException {
    SearchRequest request;
    try {
      String replacedQuery = metaAlertsForAlertQuery.replace("${GUID}", guid);
      request = JSONUtils.INSTANCE.load(replacedQuery, SearchRequest.class);
    } catch (IOException e) {
      throw new InvalidSearchException("Unable to process query:", e);
    }
    return search(request);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Document createMetaAlert(MetaAlertCreateRequest request)
      throws InvalidCreateException, IOException {
    return metaAlertUpdateDao.createMetaAlert(request);
  }

  @Override
  public Document addAlertsToMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests) throws IOException {
    return metaAlertUpdateDao.addAlertsToMetaAlert(metaAlertGuid, alertRequests);
  }

  @Override
  public Document removeAlertsFromMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests) throws IOException {
    return metaAlertUpdateDao.removeAlertsFromMetaAlert(metaAlertGuid, alertRequests);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Document updateMetaAlertStatus(String metaAlertGuid, MetaAlertStatus status)
      throws IOException {
    return metaAlertUpdateDao.updateMetaAlertStatus(metaAlertGuid, status);
  }

  public static void clear() {
    InMemoryDao.clear();
  }
}
