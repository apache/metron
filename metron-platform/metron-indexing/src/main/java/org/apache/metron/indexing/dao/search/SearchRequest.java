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
import java.util.List;
import java.util.Optional;

public class SearchRequest {

  private List<String> indices;
  private String query;
  private int size;
  private int from;
  private List<SortField> sort;
  private List<String> fields;

  public SearchRequest() {
    SortField defaultSortField = new SortField();
    defaultSortField.setField("timestamp");
    defaultSortField.setSortOrder(SortOrder.DESC.toString());
    sort = new ArrayList<>();
    sort.add(defaultSortField);
  }

  public List<String> getIndices() {
    return indices;
  }

  public void setIndices(List<String> indices) {
    this.indices = indices;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public int getFrom() {
    return from;
  }

  public void setFrom(int from) {
    this.from = from;
  }

  public List<SortField> getSort() {
    return sort;
  }

  public void setSort(List<SortField> sort) {
    this.sort = sort;
  }

  public Optional<List<String>> getFields() {
    return fields == null || fields.size() == 0 ? Optional.empty() : Optional.of(fields);
  }

  public void setFields(List<String> fields) {
    this.fields = fields;
  }
}
