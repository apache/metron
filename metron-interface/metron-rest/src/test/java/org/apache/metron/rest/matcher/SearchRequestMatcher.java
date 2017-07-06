/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.rest.matcher;

import org.apache.metron.rest.model.SortField;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.mockito.ArgumentMatcher;

import java.util.Arrays;

public class SearchRequestMatcher extends ArgumentMatcher<SearchRequest> {

  private String[] expectedIndicies;
  private BytesReference expectedSource;

  public SearchRequestMatcher(String[] indices, String query, int size, int from, SortField[] sortFields) {
    expectedIndicies = indices;
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
            .size(size)
            .from(from)
            .query(new QueryStringQueryBuilder(query))
            .fetchSource(true)
            .trackScores(true);
    for(SortField sortField: sortFields) {
      FieldSortBuilder fieldSortBuilder = new FieldSortBuilder(sortField.getField());
      fieldSortBuilder.order(sortField.getSortOrder() == org.apache.metron.rest.model.SortOrder.DESC ? SortOrder.DESC : SortOrder.ASC);
      searchSourceBuilder = searchSourceBuilder.sort(fieldSortBuilder);
    }
    expectedSource = searchSourceBuilder.buildAsBytes(Requests.CONTENT_TYPE);
  }

  @Override
  public boolean matches(Object o) {
    SearchRequest searchRequest = (SearchRequest) o;
    boolean indiciesMatch = Arrays.equals(expectedIndicies, searchRequest.indices());
    boolean sourcesMatch = searchRequest.source().equals(expectedSource);
    return indiciesMatch && sourcesMatch;
  }
}
