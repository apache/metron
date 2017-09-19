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

package org.apache.metron.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.apache.metron.rest.converter.JsonConverter;

@Entity
public class AlertProfile {

  @Id
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String id;

  @Convert(converter = JsonConverter.class)
  private List<String> tableColumns;

  @Convert(converter = JsonConverter.class)
  private List<SavedSearch> savedSearches;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<String> getTableColumns() {
    return tableColumns;
  }

  public void setTableColumns(List<String> tableColumns) {
    this.tableColumns = tableColumns;
  }

  public List<SavedSearch> getSavedSearches() {
    return savedSearches;
  }

  public void setSavedSearches(List<SavedSearch> savedSearches) {
    this.savedSearches = savedSearches;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AlertProfile that = (AlertProfile) o;

    return id != null ? id.equals(that.id) : that.id == null &&
        (tableColumns != null ? tableColumns.equals(that.tableColumns) : that.tableColumns == null &&
        (savedSearches != null ? savedSearches.equals(that.savedSearches) : that.savedSearches == null));
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (tableColumns != null ? tableColumns.hashCode() : 0);
    result = 31 * result + (savedSearches != null ? savedSearches.hashCode() : 0);
    return result;
  }
}
