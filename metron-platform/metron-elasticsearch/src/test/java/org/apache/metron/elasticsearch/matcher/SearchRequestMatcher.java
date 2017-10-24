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
package org.apache.metron.elasticsearch.matcher;

import org.apache.metron.indexing.dao.search.SortField;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.hamcrest.Description;
import org.mockito.ArgumentMatcher;

import java.util.Arrays;

public class SearchRequestMatcher extends ArgumentMatcher<SearchRequest> {

  private String[] expectedIndices;
  private String[] actualIndices;

  private BytesReference expectedSource;
  private BytesReference actualSource;

  private boolean indicesMatch;
  private boolean sourcesMatch;

  public SearchRequestMatcher(String[] indices, String query, int size, int from, SortField[] sortFields) {
    expectedIndices = indices;
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
            .size(size)
            .from(from)
            .query(new QueryStringQueryBuilder(query))
            .fetchSource(true)
            .trackScores(true);
    for(SortField sortField: sortFields) {
      FieldSortBuilder fieldSortBuilder = new FieldSortBuilder(sortField.getField());
      fieldSortBuilder.order(sortField.getSortOrder() == org.apache.metron.indexing.dao.search.SortOrder.DESC ? SortOrder.DESC : SortOrder.ASC);
      searchSourceBuilder = searchSourceBuilder.sort(fieldSortBuilder);
    }
    expectedSource = searchSourceBuilder.buildAsBytes(Requests.CONTENT_TYPE);
  }

  @Override
  public boolean matches(Object o) {
    SearchRequest searchRequest = (SearchRequest) o;

    actualIndices = searchRequest.indices();
    actualSource = searchRequest.source();

    indicesMatch = Arrays.equals(expectedIndices, actualIndices);
    sourcesMatch = expectedSource.equals(actualSource);

    return indicesMatch && sourcesMatch;
  }

  @Override
  public void describeTo(Description description) {
    if(!indicesMatch) {
      description.appendText("Bad search request indices: ");
      description.appendText(" expected=");
      description.appendValue(expectedIndices);
      description.appendText(", got=");
      description.appendValue(actualIndices);
      description.appendText("  ");
    }

    if(!sourcesMatch) {
      description.appendText("Bad search request sources: ");
      description.appendText(" expected=");
      description.appendValue(expectedSource);
      description.appendText(", got=");
      description.appendValue(actualSource);
      description.appendText("  ");
    }
  }
}
