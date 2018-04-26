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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchRequest {

  private List<String> indices;
  private String query;
  private int size;
  private int from;
  private List<SortField> sort;
  private List<String> fields;
  private List<String> facetFields;

  public SearchRequest() {
    SortField defaultSortField = new SortField();
    defaultSortField.setField("timestamp");
    defaultSortField.setSortOrder(SortOrder.DESC.toString());
    sort = new ArrayList<>();
    sort.add(defaultSortField);
  }

  /**
   * The list of indices to search.
   * @return
   */
  public List<String> getIndices() {
    return indices;
  }

  public void setIndices(List<String> indices) {
    this.indices = indices;
  }

  /**
   * The query to use to search the index
   * @return
   */
  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  /**
   * The size of the results returned.
   * @return
   */
  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  /**
   * The index to start the search from.
   * @return
   */
  public int getFrom() {
    return from;
  }

  public void setFrom(int from) {
    this.from = from;
  }

  /**
   * The search order by field.
   * @return
   */
  public List<SortField> getSort() {
    return sort;
  }

  public void setSort(List<SortField> sort) {
    this.sort = sort;
  }

  public List<String> getFields() {
    return fields;
  }

  public void setFields(List<String> fields) {
    this.fields = fields;
  }

  public List<String> getFacetFields() {
    return facetFields;
  }

  public void setFacetFields(List<String> facetFields) {
    this.facetFields = facetFields;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SearchRequest that = (SearchRequest) o;

    return (indices != null ? indices.equals(that.indices) : that.indices == null) &&
        (query != null ? query.equals(that.query) : that.query == null) && size == that.size &&
        from == that.from &&
        (sort != null ? sort.equals(that.sort) : that.sort == null) &&
        (fields != null ? fields.equals(that.fields) : that.fields == null) &&
        (facetFields != null ? facetFields.equals(that.facetFields) : that.facetFields == null);
  }

  @Override
  public int hashCode() {
    int result = indices != null ? indices.hashCode() : 0;
    result = 31 * result + (query != null ? query.hashCode() : 0);
    result = 31 * result + getSize();
    result = 31 * result + getFrom();
    result = 31 * result + (sort != null ? sort.hashCode() : 0);
    result = 31 * result + (fields != null ? fields.hashCode() : 0);
    result = 31 * result + (facetFields != null ? facetFields.hashCode() : 0);
    return result;
  }
}
