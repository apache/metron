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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MultiIndexDao;
import org.apache.metron.indexing.dao.RetrieveLatestDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConfig;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaAlertDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.solr.client.solrj.SolrClient;

public class SolrMetaAlertDao implements MetaAlertDao {

  public static final String METAALERTS_COLLECTION = "metaalert";

  private IndexDao indexDao;
  private SolrDao solrDao;
  private SolrMetaAlertSearchDao metaAlertSearchDao;
  private SolrMetaAlertUpdateDao metaAlertUpdateDao;
  private SolrMetaAlertRetrieveLatestDao metaAlertRetrieveLatestDao;
  protected String metaAlertsCollection = METAALERTS_COLLECTION;
  protected String threatTriageField = MetaAlertConstants.THREAT_FIELD_DEFAULT;
  protected String threatSort = MetaAlertConstants.THREAT_SORT_DEFAULT;

  /**
   * Wraps an {@link org.apache.metron.indexing.dao.IndexDao} to handle meta alerts.
   * @param indexDao The Dao to wrap
   */
  public SolrMetaAlertDao(IndexDao indexDao, SolrMetaAlertSearchDao metaAlertSearchDao,
      SolrMetaAlertUpdateDao metaAlertUpdateDao,
      SolrMetaAlertRetrieveLatestDao metaAlertRetrieveLatestDao) {
    this(indexDao, metaAlertSearchDao, metaAlertUpdateDao, metaAlertRetrieveLatestDao,
        METAALERTS_COLLECTION,
        MetaAlertConstants.THREAT_FIELD_DEFAULT,
        MetaAlertConstants.THREAT_SORT_DEFAULT);
  }

  /**
   * Wraps an {@link org.apache.metron.indexing.dao.IndexDao} to handle meta alerts.
   * @param indexDao The Dao to wrap
   * @param triageLevelField The field name to use as the threat scoring field
   * @param threatSort The summary aggregation of all child threat triage scores used
   *                   as the overall threat triage score for the metaalert. This
   *                   can be either max, min, average, count, median, or sum.
   */
  public SolrMetaAlertDao(IndexDao indexDao, SolrMetaAlertSearchDao metaAlertSearchDao,
      SolrMetaAlertUpdateDao metaAlertUpdateDao,
      SolrMetaAlertRetrieveLatestDao metaAlertRetrieveLatestDao,
      String metaAlertsCollection,
      String triageLevelField,
      String threatSort) {
    init(indexDao, Optional.of(threatSort));
    this.metaAlertSearchDao = metaAlertSearchDao;
    this.metaAlertUpdateDao = metaAlertUpdateDao;
    this.metaAlertRetrieveLatestDao = metaAlertRetrieveLatestDao;
    this.metaAlertsCollection = metaAlertsCollection;
    this.threatTriageField = triageLevelField;
    this.threatSort = threatSort;
  }

  public SolrMetaAlertDao() {
    //uninitialized.
  }

  /**
   * Initializes this implementation by setting the supplied IndexDao and also setting a separate SolrDao.
   * This is needed for some specific Solr functions (looking up an index from a GUID for example).
   * @param indexDao The DAO to wrap for our queries
   * @param threatSort The summary aggregation of the child threat triage scores used
   *                   as the overall threat triage score for the metaalert. This
   *                   can be either max, min, average, count, median, or sum.
   */
  @Override
  public void init(IndexDao indexDao, Optional<String> threatSort) {
    if (indexDao instanceof MultiIndexDao) {
      this.indexDao = indexDao;
      MultiIndexDao multiIndexDao = (MultiIndexDao) indexDao;
      for (IndexDao childDao : multiIndexDao.getIndices()) {
        if (childDao instanceof SolrDao) {
          this.solrDao = (SolrDao) childDao;
        }
      }
    } else if (indexDao instanceof SolrDao) {
      this.indexDao = indexDao;
      this.solrDao = (SolrDao) indexDao;
    } else {
      throw new IllegalArgumentException(
          "Need a SolrDao when using SolrMetaAlertDao"
      );
    }

    MetaAlertConfig config = new MetaAlertConfig(
        metaAlertsCollection,
        threatTriageField,
        this.threatSort,
        Constants.SENSOR_TYPE
    );

    SolrClient solrClient = solrDao.getSolrClient(solrDao.getZkHost());
    this.metaAlertSearchDao = new SolrMetaAlertSearchDao(solrClient, solrDao.getSolrSearchDao());
    this.metaAlertRetrieveLatestDao = new SolrMetaAlertRetrieveLatestDao(solrDao);
    this.metaAlertUpdateDao = new SolrMetaAlertUpdateDao(
        solrDao,
        metaAlertSearchDao,
        metaAlertRetrieveLatestDao,
        config);

    if (threatSort.isPresent()) {
      this.threatSort = threatSort.get();
    }
  }

  @Override
  public void init(AccessConfig config) {
    // Do nothing. We're just wrapping a child dao
  }

  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> indices) throws IOException {
    return indexDao.getColumnMetadata(indices);
  }

  @Override
  public Document getLatest(String guid, String sensorType) throws IOException {
    return metaAlertRetrieveLatestDao.getLatest(guid, sensorType);
  }

  @Override
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
    return metaAlertRetrieveLatestDao.getAllLatest(getRequests);
  }

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    return metaAlertSearchDao.search(searchRequest);
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    return metaAlertSearchDao.group(groupRequest);
  }

  @Override
  public void update(Document update, Optional<String> index) throws IOException {
    metaAlertUpdateDao.update(update, index);
  }

  @Override
  public void batchUpdate(Map<Document, Optional<String>> updates) {
    metaAlertUpdateDao.batchUpdate(updates);
  }

  @Override
  public void patch(RetrieveLatestDao retrieveLatestDao, PatchRequest request,
      Optional<Long> timestamp)
      throws OriginalNotFoundException, IOException {
    metaAlertUpdateDao.patch(retrieveLatestDao, request, timestamp);
  }

  @Override
  public SearchResponse getAllMetaAlertsForAlert(String guid) throws InvalidSearchException {
    return metaAlertSearchDao.getAllMetaAlertsForAlert(guid);
  }

  @Override
  public MetaAlertCreateResponse createMetaAlert(MetaAlertCreateRequest request)
      throws InvalidCreateException, IOException {
    return metaAlertUpdateDao.createMetaAlert(request);
  }

  @Override
  public boolean addAlertsToMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests)
      throws IOException {
    return metaAlertUpdateDao.addAlertsToMetaAlert(metaAlertGuid, alertRequests);
  }

  @Override
  public boolean removeAlertsFromMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests)
      throws IOException {
    return metaAlertUpdateDao.removeAlertsFromMetaAlert(metaAlertGuid, alertRequests);
  }

  @Override
  public boolean updateMetaAlertStatus(String metaAlertGuid, MetaAlertStatus status)
      throws IOException {
    return metaAlertUpdateDao.updateMetaAlertStatus(metaAlertGuid, status);
  }
}
