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

import static org.apache.metron.solr.dao.SolrMetaAlertDao.METAALERTS_COLLECTION;

import java.io.IOException;
import java.util.List;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertRetrieveLatestDao;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

public class SolrMetaAlertRetrieveLatestDao implements
    MetaAlertRetrieveLatestDao {

  private SolrDao solrDao;
  private SolrClient solrClient;

  public SolrMetaAlertRetrieveLatestDao(SolrClient client, SolrDao solrDao) {
    this.solrClient = client;
    this.solrDao = solrDao;
  }

  @Override
  public Document getLatest(String guid, String sensorType) throws IOException {
    if (MetaAlertConstants.METAALERT_TYPE.equals(sensorType)) {
      // Unfortunately, we can't just defer to the indexDao for this. Child alerts in Solr end up
      // having to be dug out.
      String guidClause = Constants.GUID + ":" + guid;
      SolrQuery query = new SolrQuery();
      query.setQuery(guidClause)
          .setFields("*", "[child parentFilter=" + guidClause + " limit=999]");

      try {
        QueryResponse response = solrClient.query(METAALERTS_COLLECTION, query);
        // GUID is unique, so it's definitely the first result
        if (response.getResults().size() == 1) {
          SolrDocument result = response.getResults().get(0);

          return SolrUtilities.toDocument(result);
        } else {
          return null;
        }
      } catch (SolrServerException e) {
        throw new IOException("Unable to retrieve metaalert", e);
      }
    } else {
      return solrDao.getLatest(guid, sensorType);
    }
  }

  @Override
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
    return solrDao.getAllLatest(getRequests);
  }
}
