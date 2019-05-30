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
package org.apache.metron.indexing.dao.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchResponse {

  private long total;
  private List<SearchResult> results = new ArrayList<>();
  private Map<String, Map<String, Long>> facetCounts;

  /**
   * The total number of results
   * @return
   */
  public long getTotal() {
    return total;
  }

  public void setTotal(long total) {
    this.total = total;
  }

  /**
   * The list of results
   * @return
   */
  public List<SearchResult> getResults() {
    return results;
  }

  public void setResults(List<SearchResult> results) {
    this.results = results;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Map<String, Map<String, Long>> getFacetCounts() {
    return facetCounts;
  }

  public void setFacetCounts(Map<String, Map<String, Long>> facetCounts) {
    this.facetCounts = facetCounts;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SearchResponse that = (SearchResponse) o;

    return getTotal() == that.getTotal() &&
            (getResults() != null ? getResults().equals(that.getResults()) : that.getResults() != null) &&
            (getFacetCounts() != null ? getFacetCounts().equals(that.getFacetCounts()) : that.getFacetCounts() != null);
  }

  @Override
  public int hashCode() {
    int result = 31 * (int) getTotal() + (getResults() != null ? getResults().hashCode() : 0);
    result = 31 * result + (getFacetCounts() != null ? getFacetCounts().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "SearchResponse{" +
        "total=" + total +
        ", results=" + results +
        ", facetCounts=" + facetCounts +
        '}';
  }
}
