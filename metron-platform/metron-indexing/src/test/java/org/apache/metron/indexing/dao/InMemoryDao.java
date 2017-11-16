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
import java.util.Map.Entry;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.search.*;
import org.apache.metron.indexing.dao.update.Document;

import java.io.IOException;
import java.util.*;

public class InMemoryDao implements IndexDao {
  // Map from index to list of documents as JSON strings
  public static Map<String, List<String>> BACKING_STORE = new HashMap<>();
  public static Map<String, Map<String, FieldType>> COLUMN_METADATA = new HashMap<>();
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

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    GroupResponse groupResponse = new GroupResponse();
    groupResponse.setGroupedBy(groupRequest.getGroups().get(0).getField());
    groupResponse.setGroupResults(getGroupResults(groupRequest.getGroups(), 0));
    return groupResponse;
  }

  private List<GroupResult> getGroupResults(List<Group> groups, int index) {
    Group group = groups.get(index);
    GroupResult groupResult = new GroupResult();
    groupResult.setKey(group.getField() + "_value");
    if (index < groups.size() - 1) {
      groupResult.setGroupedBy(groups.get(index + 1).getField());
      groupResult.setGroupResults(getGroupResults(groups, index + 1));
    } else {
      groupResult.setScore(50.0);
    }
    groupResult.setTotal(10);
    return Collections.singletonList(groupResult);
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
    if (query == null) {
      return false;
    }
    if(query.equals("*")) {
      return true;
    }
    if(query.contains(":")) {
      Iterable<String> splits = Splitter.on(":").split(query.trim());
      String field = Iterables.getFirst(splits, "");
      String val = Iterables.getLast(splits, "");

      // Immediately quit if there's no value ot find
      if (val == null) {
        return false;
      }

      // Check if we're looking into a nested field.  The '|' is arbitrarily chosen.
      String nestingField = null;
      if (field.contains("|")) {
        Iterable<String> fieldSplits = Splitter.on('|').split(field);
        nestingField = Iterables.getFirst(fieldSplits, null);
        field = Iterables.getLast(fieldSplits, null);
      }
      if (nestingField == null) {
        // Just grab directly
        Object o = doc.get(field);
        return val.equals(o);
      } else {
        // We need to look into a nested field for the value
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nestedList = (List<Map<String, Object>>) doc.get(nestingField);
        if (nestedList == null) {
          return false;
        } else {
          for (Map<String, Object> nestedEntry : nestedList) {
            if (val.equals(nestedEntry.get(field))) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  public static Map<String, Object> parse(String doc) {
    try {
      return JSONUtils.INSTANCE.load(doc, new TypeReference<Map<String, Object>>() {});
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }

  }

  @Override
  public void init(AccessConfig config) {
    this.config = config;
  }

  @Override
  public Document getLatest(String guid, String sensorType) throws IOException {
    for(Map.Entry<String, List<String>> kv: BACKING_STORE.entrySet()) {
      if(kv.getKey().startsWith(sensorType)) {
        for(String doc : kv.getValue()) {
          Map<String, Object> docParsed = parse(doc);
          if(docParsed.getOrDefault(Constants.GUID, "").equals(guid)) {
            return new Document(doc, guid, sensorType, 0L);
          }
        }
      }
    }
    return null;
  }

  @Override
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
    List<Document> documents = new ArrayList<>();
    for(Map.Entry<String, List<String>> kv: BACKING_STORE.entrySet()) {
      for(String doc : kv.getValue()) {
        Map<String, Object> docParsed = parse(doc);
        String guid = (String) docParsed.getOrDefault(Constants.GUID, "");
        for (GetRequest getRequest: getRequests) {
          if(getRequest.getGuid().equals(guid)) {
            documents.add(new Document(doc, guid, getRequest.getSensorType(), 0L));
          }
        }

      }
    }
    return documents;
  }

  @Override
  public void update(Document update, Optional<String> index) throws IOException {
    for (Map.Entry<String, List<String>> kv : BACKING_STORE.entrySet()) {
      if (kv.getKey().startsWith(update.getSensorType())) {
        for (Iterator<String> it = kv.getValue().iterator(); it.hasNext(); ) {
          String doc = it.next();
          Map<String, Object> docParsed = parse(doc);
          if (docParsed.getOrDefault(Constants.GUID, "").equals(update.getGuid())) {
            it.remove();
          }
        }
        kv.getValue().add(JSONUtils.INSTANCE.toJSON(update.getDocument(), true));
      }
    }
  }

  @Override
  public void batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
    for (Map.Entry<Document, Optional<String>> update : updates.entrySet()) {
      update(update.getKey(), update.getValue());
    }
  }

  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> indices) throws IOException {
    Map<String, FieldType> indexColumnMetadata = new HashMap<>();
    for(String index: indices) {
      if (COLUMN_METADATA.containsKey(index)) {
        Map<String, FieldType> columnMetadata = COLUMN_METADATA.get(index);
        for (Entry entry: columnMetadata.entrySet()) {
          String field = (String) entry.getKey();
          FieldType type = (FieldType) entry.getValue();
          if (indexColumnMetadata.containsKey(field)) {
            if (!type.equals(indexColumnMetadata.get(field))) {
              indexColumnMetadata.put(field, FieldType.OTHER);
            }
          } else {
            indexColumnMetadata.put(field, type);
          }
        }
      }
    }
    return indexColumnMetadata;
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
