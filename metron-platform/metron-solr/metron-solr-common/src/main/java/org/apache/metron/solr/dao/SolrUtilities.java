/*
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

package org.apache.metron.solr.dao;

import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.solr.common.SolrDocument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class SolrUtilities {

  public static SearchResult getSearchResult(SolrDocument solrDocument, List<String> fields, Function<String, String> indexSupplier) {
    SolrDocumentBuilder documentBuilder = new SolrDocumentBuilder();
    Document document = documentBuilder.toDocument(solrDocument);

    String index = indexSupplier.apply(document.getSensorType());

    SearchResult searchResult = new SearchResult();
    searchResult.setId(document.getGuid());
    searchResult.setIndex(index);

    Map<String, Object> docSource = document.getDocument();
    final Map<String, Object> source = new HashMap<>();
    if (fields != null) {
      fields.forEach(field -> source.put(field, docSource.get(field)));
    } else {
      source.putAll(docSource);
    }
    searchResult.setSource(source);

    return searchResult;
  }

  /**
   * Gets the actual collection for the given sensor type
   * @param indexSupplier The function to employ in the lookup
   * @param sensorName The sensor type to be looked up
   * @param index An index to use, if present.
   * @return An Optional containing the actual collection
   */
  public static Optional<String> getIndex(Function<String, String> indexSupplier, String sensorName, Optional<String> index) {
    if (index.isPresent()) {
      return index;
    } else {
      String realIndex = indexSupplier.apply(sensorName);
      return Optional.ofNullable(realIndex);
    }
  }
}
