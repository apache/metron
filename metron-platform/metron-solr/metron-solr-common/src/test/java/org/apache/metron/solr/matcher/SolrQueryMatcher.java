/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.solr.matcher;

import org.apache.solr.client.solrj.SolrQuery;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.mockito.ArgumentMatcher;

import java.util.Arrays;
import java.util.Objects;

// This somewhat awkwardly coexists with Mockito's Matchers and Hamcrest's. So use both.
public class SolrQueryMatcher implements ArgumentMatcher<SolrQuery> {
  private SolrQuery expectedSolrQuery;
  private SolrQueryHamcrestMatcher hamcrestMatcher;

  public SolrQueryMatcher(SolrQuery solrQuery) {
    this.expectedSolrQuery = solrQuery;
    this.hamcrestMatcher = new SolrQueryHamcrestMatcher();
  }

  @Override
  public boolean matches(SolrQuery solrQuery) {
      return hamcrestMatcher.matches(solrQuery);
  }

  @Override
  public String toString() {
    return expectedSolrQuery.toString();
  }

  public Matcher<SolrQuery> asHamcrestMatcher() {
      return hamcrestMatcher;
  }

  private class SolrQueryHamcrestMatcher extends BaseMatcher<SolrQuery>{
    @Override
    public boolean matches(Object o) {
      SolrQuery solrQuery = (SolrQuery) o;
      return Objects.equals(solrQuery.getStart(), expectedSolrQuery.getStart())
              && Objects.equals(solrQuery.getRows(), expectedSolrQuery.getRows())
              && Objects.equals(solrQuery.getQuery(), expectedSolrQuery.getQuery())
              && Objects.equals(solrQuery.getSorts(), expectedSolrQuery.getSorts())
              && Objects.equals(solrQuery.getFields(), expectedSolrQuery.getFields())
              && Arrays.equals(solrQuery.getFacetFields(), expectedSolrQuery.getFacetFields())
              && Objects.equals(solrQuery.get("collection"), expectedSolrQuery.get("collection"))
              && Objects.equals(solrQuery.get("stats"), expectedSolrQuery.get("stats"))
              && Objects.equals(solrQuery.get("stats.field"), expectedSolrQuery.get("stats.field"))
              && Objects.equals(solrQuery.get("facet"), expectedSolrQuery.get("facet"))
              && Objects.equals(solrQuery.get("facet.pivot"), expectedSolrQuery.get("facet.pivot"));
    }

    @Override
    public void describeTo(Description description) {
      description.appendValue(expectedSolrQuery);
    }
  }
}
