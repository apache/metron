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
package org.apache.metron.indexing.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.search.*;

import java.io.IOException;
import java.util.*;

public class InMemoryDao implements IndexDao {
  public static Map<String, List<String>> BACKING_STORE = new HashMap<>();
  public static Map<String, Map<String, FieldType>> COLUMN_METADATA;
  private AccessConfig config;

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    if(config.getMaxSearchResults() != null && searchRequest.getSize() > config.getMaxSearchResults()) {
      throw new InvalidSearchException("Search result size must be less than " + config.getMaxSearchResults());
    }
    List<SearchResult> response = new ArrayList<>();
    for(String index : searchRequest.getIndices()) {
      String i = null;
      for(String storedIdx : BACKING_STORE.keySet()) {
        if(storedIdx.equals(index) || storedIdx.startsWith(index + "_")) {
          i = storedIdx;
        }
      }
      if(i == null) {
        continue;
      }
      for (String doc : BACKING_STORE.get(i)) {
        Map<String, Object> docParsed = parse(doc);
        if (isMatch(searchRequest.getQuery(), docParsed)) {
          SearchResult result = new SearchResult();
          result.setSource(docParsed);
          result.setScore((float) Math.random());
          result.setId(docParsed.getOrDefault(Constants.GUID, UUID.randomUUID()).toString());
          response.add(result);
        }
      }
    }

    if(searchRequest.getSort().size() != 0) {
      Collections.sort(response, sorted(searchRequest.getSort()));
    }
    SearchResponse ret = new SearchResponse();
    List<SearchResult> finalResp = new ArrayList<>();
    int maxSize = config.getMaxSearchResults() == null?searchRequest.getSize():config.getMaxSearchResults();
    for(int i = searchRequest.getFrom();i < response.size()&& finalResp.size() <= maxSize;++i) {
      finalResp.add(response.get(i));
    }
    ret.setTotal(response.size());
    ret.setResults(finalResp);
    return ret;
  }


  private static class ComparableComparator implements Comparator<Comparable>  {
    SortOrder order = null;
    public ComparableComparator(SortOrder order) {
      this.order = order;
    }
    @Override
    public int compare(Comparable o1, Comparable o2) {
      int result = ComparisonChain.start().compare(o1, o2).result();
      return order == SortOrder.ASC?result:-1*result;
    }
  }

  private static Comparator<SearchResult> sorted(final List<SortField> fields) {
    return (o1, o2) -> {
      ComparisonChain chain = ComparisonChain.start();
      for(SortField field : fields) {
        Comparable f1 = (Comparable) o1.getSource().get(field.getField());
        Comparable f2 = (Comparable) o2.getSource().get(field.getField());
        chain = chain.compare(f1, f2, new ComparableComparator(field.getSortOrder()));
      }
      return chain.result();
    };
  }

  private static boolean isMatch(String query, Map<String, Object> doc) {
    if(query.equals("*")) {
      return true;
    }
    if(query.contains(":")) {
      Iterable<String> splits = Splitter.on(":").split(query.trim());
      String field = Iterables.getFirst(splits, "");
      String val = Iterables.getLast(splits, "");
      Object o = doc.get(field);
      if(o == null) {
        return false;
      }
      else {
        return o.equals(val);
      }
    }
    return false;
  }

  private static Map<String, Object> parse(String doc) {
    try {
      return JSONUtils.INSTANCE.load(doc, new TypeReference<Map<String, Object>>() {});
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }

  }

  @Override
  public void init(Map<String, Object> globalConfig, AccessConfig config) {
    this.config = config;
  }

  @Override
  public Map<String, Map<String, FieldType>> getColumnMetadata(List<String> indices) throws IOException {
    Map<String, Map<String, FieldType>> columnMetadata = new HashMap<>();
    for(String index: indices) {
      columnMetadata.put(index, new HashMap<>(COLUMN_METADATA.get(index)));
    }
    return columnMetadata;
  }

  @Override
  public Map<String, FieldType> getCommonColumnMetadata(List<String> indices) throws IOException {
    Map<String, FieldType> commonColumnMetadata = new HashMap<>();
    for(String index: indices) {
      if (commonColumnMetadata.isEmpty()) {
        commonColumnMetadata = new HashMap<>(COLUMN_METADATA.get(index));
      } else {
        commonColumnMetadata.entrySet().retainAll(COLUMN_METADATA.get(index).entrySet());
      }
    }
    return commonColumnMetadata;
  }

  public static void setColumnMetadata(Map<String, Map<String, FieldType>> columnMetadata) {
    Map<String, Map<String, FieldType>> columnMetadataMap = new HashMap<>();
    for (Map.Entry<String, Map<String, FieldType>> e: columnMetadata.entrySet()) {
      columnMetadataMap.put(e.getKey(), Collections.unmodifiableMap(e.getValue()));
    }
    COLUMN_METADATA = columnMetadataMap;
  }

  public static void load(Map<String, List<String>> backingStore) {
    BACKING_STORE = backingStore;
  }

  public static void clear() {
    BACKING_STORE.clear();
    COLUMN_METADATA.clear();
  }
}
