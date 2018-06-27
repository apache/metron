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

package org.apache.metron.elasticsearch.dao;

import static org.apache.metron.common.Constants.GUID;
import static org.apache.metron.elasticsearch.utils.ElasticsearchUtils.queryAllResults;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.constantScoreQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import org.apache.lucene.search.join.ScoreMode;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConfig;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertSearchDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.elasticsearch.index.query.InnerHitBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;

public class ElasticsearchMetaAlertSearchDao implements MetaAlertSearchDao {

  protected ElasticsearchDao elasticsearchDao;
  private MetaAlertConfig config;
  private int pageSize;

  public ElasticsearchMetaAlertSearchDao(ElasticsearchDao elasticsearchDao,
      MetaAlertConfig config, int pageSize) {
    this.elasticsearchDao = elasticsearchDao;
    this.config = config;
    this.pageSize = pageSize;
  }

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    // Wrap the query to also get any meta-alerts.
    QueryBuilder qb = constantScoreQuery(boolQuery()
        .must(boolQuery()
            .should(new QueryStringQueryBuilder(searchRequest.getQuery()))
            .should(nestedQuery(
                MetaAlertConstants.ALERT_FIELD,
                new QueryStringQueryBuilder(searchRequest.getQuery()),
                ScoreMode.None
                )
            )
        )
        // Ensures that it's a meta alert with active status or that it's an alert (signified by
        // having no status field)
        .must(boolQuery()
            .should(termQuery(MetaAlertConstants.STATUS_FIELD,
                MetaAlertStatus.ACTIVE.getStatusString()))
            .should(boolQuery().mustNot(existsQuery(MetaAlertConstants.STATUS_FIELD)))
        )
        .mustNot(existsQuery(MetaAlertConstants.METAALERT_FIELD))
    );
    return elasticsearchDao.search(searchRequest, qb);
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    // Wrap the query to hide any alerts already contained in meta alerts
    QueryBuilder qb = QueryBuilders.boolQuery()
        .must(new QueryStringQueryBuilder(groupRequest.getQuery()))
        .mustNot(existsQuery(MetaAlertConstants.METAALERT_FIELD));
    return elasticsearchDao.group(groupRequest, qb);
  }

  @Override
  public SearchResponse getAllMetaAlertsForAlert(String guid) throws InvalidSearchException {
    if (guid == null || guid.trim().isEmpty()) {
      throw new InvalidSearchException("Guid cannot be empty");
    }
    // Searches for all alerts containing the meta alert guid in it's "metalerts" array
    QueryBuilder qb = boolQuery()
        .must(
            nestedQuery(
                MetaAlertConstants.ALERT_FIELD,
                boolQuery()
                    .must(termQuery(MetaAlertConstants.ALERT_FIELD + "." + GUID, guid)),
                ScoreMode.None
            ).innerHit(new InnerHitBuilder())
        )
        .must(termQuery(MetaAlertConstants.STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString()));
    return queryAllResults(elasticsearchDao.getClient(), qb, config.getMetaAlertIndex(),
        pageSize);
  }
}
