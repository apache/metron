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

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.Document;

public class MultiIndexDao implements IndexDao {
  private List<IndexDao> indices;

  public MultiIndexDao( IndexDao... composedDao) {
    indices = new ArrayList<>();
    Collections.addAll(indices, composedDao);
  }

  public MultiIndexDao(Iterable<IndexDao> composedDao) {
    this.indices = new ArrayList<>();
    Iterables.addAll(indices, composedDao);
  }

  public MultiIndexDao(Iterable<IndexDao> composedDao, Function<IndexDao, IndexDao> decoratorTransformation) {
    this(Iterables.transform(composedDao, x -> decoratorTransformation.apply(x)));
  }

  @Override
  public void update(final Document update, Optional<String> index) throws IOException {
    List<String> exceptions =
    indices.parallelStream().map(dao -> {
      try {
        dao.update(update, index);
        return null;
      } catch (Throwable e) {
        return dao.getClass() + ": " + e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e);
      }
    }).filter(e -> e != null).collect(Collectors.toList());
    if(exceptions.size() > 0) {
      throw new IOException(Joiner.on("\n").join(exceptions));
    }
  }

  @Override
  public void batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
    List<String> exceptions =
        indices.parallelStream().map(dao -> {
          try {
            dao.batchUpdate(updates);
            return null;
          } catch (Throwable e) {
            return dao.getClass() + ": " + e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e);
          }
        }).filter(e -> e != null).collect(Collectors.toList());
    if (exceptions.size() > 0) {
      throw new IOException(Joiner.on("\n").join(exceptions));
    }
  }

  @Override
  public Map<String, Map<String, FieldType>> getColumnMetadata(List<String> in) throws IOException {
    for(IndexDao dao : indices) {
      Map<String, Map<String, FieldType>> r = dao.getColumnMetadata(in);
      if(r != null) {
        return r;
      }
    }
    return null;
  }

  @Override
  public Map<String, FieldType> getCommonColumnMetadata(List<String> in) throws IOException {
    for(IndexDao dao : indices) {
      Map<String, FieldType> r = dao.getCommonColumnMetadata(in);
      if(r != null) {
        return r;
      }
    }
    return null;
  }

  private static class DocumentContainer {
    private Optional<Document> d = Optional.empty();
    private Optional<Throwable> t = Optional.empty();
    public DocumentContainer(Document d) {
      this.d = Optional.ofNullable(d);
    }
    public DocumentContainer(Throwable t) {
      this.t = Optional.ofNullable(t);
    }

    public Optional<Document> getDocument() {
      return d;
    }
    public Optional<Throwable> getException() {
      return t;
    }

  }

  private static class DocumentIterableContainer {
    private Optional<Iterable<Document>> d = Optional.empty();
    private Optional<Throwable> t = Optional.empty();
    public DocumentIterableContainer(Iterable<Document> d) {
      this.d = Optional.ofNullable(d);
    }
    public DocumentIterableContainer(Throwable t) {
      this.t = Optional.ofNullable(t);
    }

    public Optional<Iterable<Document>> getDocumentIterable() {
      return d;
    }
    public Optional<Throwable> getException() {
      return t;
    }

  }

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    for(IndexDao dao : indices) {
      SearchResponse s = dao.search(searchRequest);
      if(s != null) {
        return s;
      }
    }
    return null;
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    for(IndexDao dao : indices) {
      GroupResponse s = dao.group(groupRequest);
      if(s != null) {
        return s;
      }
    }
    return null;
  }

  @Override
  public void init(AccessConfig config) {
    for(IndexDao dao : indices) {
      dao.init(config);
    }
  }

  @Override
  public Document getLatest(final String guid, String sensorType) throws IOException {
    Document ret = null;
    List<DocumentContainer> output =
            indices.parallelStream().map(dao -> {
      try {
        return new DocumentContainer(dao.getLatest(guid, sensorType));
      } catch (Throwable e) {
        return new DocumentContainer(e);
      }
    }).collect(Collectors.toList());

    List<String> error = new ArrayList<>();
    for(DocumentContainer dc : output) {
      if(dc.getException().isPresent()) {
        Throwable e = dc.getException().get();
        error.add(e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
      }
      else {
        if(dc.getDocument().isPresent()) {
          Document d = dc.getDocument().get();
          if(ret == null || ret.getTimestamp() < d.getTimestamp()) {
            ret = d;
          }
        }
      }
    }
    if(error.size() > 0) {
      throw new IOException(Joiner.on("\n").join(error));
    }
    return ret;
  }

  @Override
  public Iterable<Document> getAllLatest(
      List<GetRequest> getRequests) throws IOException {
    Iterable<Document> ret = null;
    List<DocumentIterableContainer> output =
        indices.parallelStream().map(dao -> {
          try {
            return new DocumentIterableContainer(dao.getAllLatest(getRequests));
          } catch (Throwable e) {
            return new DocumentIterableContainer(e);
          }
        }).collect(Collectors.toList());

    List<String> error = new ArrayList<>();
    for(DocumentIterableContainer dc : output) {
      if(dc.getException().isPresent()) {
        Throwable e = dc.getException().get();
        error.add(e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
      }
      else {
        if(dc.getDocumentIterable().isPresent()) {
          Iterable<Document> documents = dc.getDocumentIterable().get();
          if(ret == null) {
            ret = documents;
          }
        }
      }
    }
    if(error.size() > 0) {
      throw new IOException(Joiner.on("\n").join(error));
    }
    return ret;
  }

  public List<IndexDao> getIndices() {
    return indices;
  }
}
