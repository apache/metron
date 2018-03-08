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

import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MetaAlertDao;
import org.apache.metron.indexing.dao.MultiIndexDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.search.*;
import org.apache.metron.indexing.dao.update.Document;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SolrMetaAlertDao implements MetaAlertDao {

    private SolrDao solrDao;

    @Override
    public SearchResponse getAllMetaAlertsForAlert(String guid) throws InvalidSearchException {
        return null;
    }

    @Override
    public MetaAlertCreateResponse createMetaAlert(MetaAlertCreateRequest request) throws InvalidCreateException, IOException {
        return null;
    }

    @Override
    public boolean addAlertsToMetaAlert(String metaAlertGuid, List<GetRequest> getRequests) throws IOException {
        return false;
    }

    @Override
    public boolean removeAlertsFromMetaAlert(String metaAlertGuid, List<GetRequest> getRequests) throws IOException {
        return false;
    }

    @Override
    public boolean updateMetaAlertStatus(String metaAlertGuid, MetaAlertStatus status) throws IOException {
        return false;
    }

    @Override
    public void init(IndexDao indexDao) {

    }

    @Override
    public void init(IndexDao indexDao, Optional<String> threatSort) {
        if (indexDao instanceof MultiIndexDao) {
            MultiIndexDao multiIndexDao = (MultiIndexDao) indexDao;
            for (IndexDao childDao : multiIndexDao.getIndices()) {
                if (childDao instanceof SolrDao) {
                    this.solrDao = (SolrDao) childDao;
                }
            }
        } else if (indexDao instanceof SolrDao) {
            this.solrDao = (SolrDao) indexDao;
        } else {
            throw new IllegalArgumentException(
                    "Need an SolrDao when using SolrMetaAlertDao"
            );
        }
    }

    @Override
    public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
        return solrDao.search(searchRequest);
    }

    @Override
    public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
        return solrDao.group(groupRequest);
    }

    @Override
    public void init(AccessConfig config) {

    }

    @Override
    public Document getLatest(String guid, String sensorType) throws IOException {
        return solrDao.getLatest(guid, sensorType);
    }

    @Override
    public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
        return solrDao.getAllLatest(getRequests);
    }

    @Override
    public void update(Document update, Optional<String> index) throws IOException {
        solrDao.update(update, index);
    }

    @Override
    public void batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
        solrDao.batchUpdate(updates);
    }

    @Override
    public Map<String, FieldType> getColumnMetadata(List<String> indices) throws IOException {
        return solrDao.getColumnMetadata(indices);
    }
}
