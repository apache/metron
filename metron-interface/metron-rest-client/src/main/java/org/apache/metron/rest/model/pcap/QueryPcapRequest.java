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
package org.apache.metron.rest.model.pcap;

import org.apache.metron.pcap.config.PcapOptions;
import org.apache.metron.pcap.filter.query.QueryPcapFilter;

public class QueryPcapRequest extends PcapRequest {

  public QueryPcapRequest() {
    PcapOptions.FILTER_IMPL.put(this, new QueryPcapFilter.Configurator());
  }

  public String getQuery() {
    return QueryPcapOptions.QUERY.get(this, String.class);
  }

  public void setQuery(String query) {
    QueryPcapOptions.QUERY.put(this, query);
  }

  @Override
  public void setFields() {
    PcapOptions.FIELDS.put(this, getQuery());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    QueryPcapRequest queryPcapRequest = (QueryPcapRequest) o;

    return (super.equals(o)) &&
            (getQuery() != null ? getQuery().equals(queryPcapRequest.getQuery()) : queryPcapRequest.getQuery() != null);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (getQuery() != null ? getQuery().hashCode() : 0);
    return result;
  }
}
