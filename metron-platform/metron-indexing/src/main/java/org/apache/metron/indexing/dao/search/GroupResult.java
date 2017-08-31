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
import java.util.List;

public class GroupResult {

  private String key;
  private long total;
  private Double score;
  private String groupedBy;
  private List<GroupResult> groupResults;

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public long getTotal() {
    return total;
  }

  public void setTotal(long total) {
    this.total = total;
  }

  public Double getScore() {
    return score;
  }

  public void setScore(Double score) {
    this.score = score;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String getGroupedBy() {
    return groupedBy;
  }

  public void setGroupedBy(String groupedBy) {
    this.groupedBy = groupedBy;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public List<GroupResult> getGroupResults() {
    return groupResults;
  }

  public void setGroupResults(List<GroupResult> groups) {
    this.groupResults = groups;
  }
}
