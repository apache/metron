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

import org.apache.solr.common.SolrInputDocument;
import org.hamcrest.Description;
import org.mockito.ArgumentMatcher;

import java.util.Objects;

public class SolrInputDocumentMatcher extends ArgumentMatcher<SolrInputDocument> {

  private SolrInputDocument expectedSolrInputDocument;

  public SolrInputDocumentMatcher(SolrInputDocument solrInputDocument) {
    this.expectedSolrInputDocument = solrInputDocument;
  }

  @Override
  public boolean matches(Object o) {
    boolean matches = false;

    SolrInputDocument actual = (SolrInputDocument) o;
    for(String expectedField: expectedSolrInputDocument.getFieldNames()) {
      Object expectedValue = expectedSolrInputDocument.getField(expectedField).getValue();

      // does it even have the expected field?
      boolean hasField = actual.getField(expectedField) != null;
      if(hasField) {

        // is the field value the same?
        Object actualValue = actual.getField(expectedField).getValue();
        if(Objects.equals(expectedValue, actualValue)) {
          matches = true;
        }
      }
    }
    return matches;
  }

  @Override
  public void describeTo(Description description) {
    description.appendValue(expectedSolrInputDocument);
  }
}
