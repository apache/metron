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
package org.apache.metron.solr.matcher;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.hamcrest.Description;
import org.mockito.ArgumentMatcher;

import java.util.Arrays;

public class SolrQueryMatcher extends ArgumentMatcher<ModifiableSolrParams> {

  private SolrQuery expectedSolrQuery;

  public SolrQueryMatcher(SolrQuery solrQuery) {
    this.expectedSolrQuery = solrQuery;
  }

  @Override
  public boolean matches(Object o) {
    SolrQuery solrQuery = (SolrQuery) o;
    return solrQuery.getStart().equals(expectedSolrQuery.getStart()) &&
            solrQuery.getRows().equals(expectedSolrQuery.getRows()) &&
            solrQuery.getQuery().equals(expectedSolrQuery.getQuery()) &&
            solrQuery.getSorts().equals(expectedSolrQuery.getSorts()) &&
            ((solrQuery.getFields() == null && expectedSolrQuery.getFields() == null) || solrQuery.getFields().equals(expectedSolrQuery.getFields())) &&
            Arrays.equals(solrQuery.getFacetFields(), expectedSolrQuery.getFacetFields()) &&
            ((solrQuery.get("stats") == null && expectedSolrQuery.get("stats") == null) || solrQuery.get("stats").equals(expectedSolrQuery.get("stats"))) &&
            ((solrQuery.get("stats.field") == null && expectedSolrQuery.get("stats.field") == null) || solrQuery.get("stats.field").equals(expectedSolrQuery.get("stats.field"))) &&
            ((solrQuery.get("facet") == null && expectedSolrQuery.get("facet") == null) || solrQuery.get("facet").equals(expectedSolrQuery.get("facet"))) &&
            ((solrQuery.get("facet.pivot") == null && expectedSolrQuery.get("facet.pivot") == null) || solrQuery.get("facet.pivot").equals(expectedSolrQuery.get("facet.pivot")));
  }

  @Override
  public void describeTo(Description description) {
    description.appendValue(expectedSolrQuery);
  }
}
