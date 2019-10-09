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
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MultiIndexDao implements IndexDao {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private List<IndexDao> indices;

  public MultiIndexDao(IndexDao... composedDao) {
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
  public Document update(final Document update, Optional<String> index) throws IOException {
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
    return update;
  }

  @Override
  public Map<Document, Optional<String>> batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
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
    return updates;
  }

  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> in) throws IOException {
    for(IndexDao dao : indices) {
      Map<String, FieldType> r = dao.getColumnMetadata(in);
      if(r != null) {
        return r;
      }
    }
    return null;
  }

  @Override
  public Document addCommentToAlert(CommentAddRemoveRequest request) throws IOException {
    Document latest = getLatest(request.getGuid(), request.getSensorType());
    return addCommentToAlert(request, latest);
  }

  /**
   * Adds comments to an alert.  Updates are written to each Dao in parallel with the assumption that all updates
   * are identical.  The first update to be applied is returned as the current version of the alert with comments added.
   * @param request Request to add comments
   * @param latest The latest version of the alert the comments will be added to.
   * @return The complete alert document with comments added.
   * @throws IOException
   */
  @Override
  public Document addCommentToAlert(CommentAddRemoveRequest request, Document latest) throws IOException {
    List<DocumentContainer> output = indices
            .parallelStream()
            .map(dao -> addCommentToAlert(dao, request, latest))
            .collect(Collectors.toList());
    return getLatestDocument(output);
  }

  private DocumentContainer addCommentToAlert(IndexDao indexDao, CommentAddRemoveRequest request, Document latest) {
    DocumentContainer container;
    try {
      Document document = indexDao.addCommentToAlert(request, latest);
      container = new DocumentContainer(document);
      LOG.debug("Added comment to alert; indexDao={}, guid={}, sensorType={}, document={}",
              ClassUtils.getShortClassName(indexDao.getClass()), document.getGuid(), document.getSensorType(), document);

    } catch (Throwable e) {
      container = new DocumentContainer(e);
      LOG.error("Unable to add comment to alert; indexDao={}, error={}",
              ClassUtils.getShortClassName(indexDao.getClass()), ExceptionUtils.getRootCauseMessage(e));
    }

    return container;
  }

  @Override
  public Document removeCommentFromAlert(CommentAddRemoveRequest request) throws IOException {
    Document latest = getLatest(request.getGuid(), request.getSensorType());
    return removeCommentFromAlert(request, latest);
  }

  /**
   * Removes comments from an alert.  Updates are written to each Dao in parallel with the assumption that all updates
   * are identical.  The first update to be applied is returned as the current version of the alert with comments removed.
   * @param request Request to remove comments
   * @param latest The latest version of the alert the comments will be removed from.
   * @return The complete alert document with comments removed.
   * @throws IOException
   */
  @Override
  public Document removeCommentFromAlert(CommentAddRemoveRequest request, Document latest) throws IOException {
    List<DocumentContainer> output = indices
            .parallelStream()
            .map(dao -> removeCommentFromAlert(dao, request, latest))
            .collect(Collectors.toList());
    return getLatestDocument(output);
  }

  private DocumentContainer removeCommentFromAlert(IndexDao indexDao, CommentAddRemoveRequest request, Document latest) {
    DocumentContainer container;
    try {
      Document document = indexDao.removeCommentFromAlert(request, latest);
      container = new DocumentContainer(document);
      LOG.debug("Removed comment from alert; indexDao={}, guid={}, sensorType={}, document={}",
              ClassUtils.getShortClassName(indexDao.getClass()), document.getGuid(), document.getSensorType(), document);

    } catch (Throwable e) {
      container = new DocumentContainer(e);
      LOG.error("Unable to remove comment from alert; indexDao={}, error={}",
              ClassUtils.getShortClassName(indexDao.getClass()), ExceptionUtils.getRootCauseMessage(e));
    }

    return container;
  }

  protected static class DocumentContainer {
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
    List<DocumentContainer> output = indices
            .parallelStream()
            .map(dao -> getLatest(dao, guid, sensorType))
            .collect(Collectors.toList());
    return getLatestDocument(output);
  }

  private DocumentContainer getLatest(IndexDao indexDao, String guid, String sensorType) {
    DocumentContainer container;
    try {
      Document document = indexDao.getLatest(guid, sensorType);
      container = new DocumentContainer(document);
      LOG.debug("Found latest document; indexDao={}, guid={}, sensorType={}, document={}",
              ClassUtils.getShortClassName(indexDao.getClass()), guid, sensorType, document);

    } catch (Throwable e) {
      container = new DocumentContainer(e);
      LOG.error("Unable to find latest document; indexDao={}, error={}",
              ClassUtils.getShortClassName(indexDao.getClass()), ExceptionUtils.getRootCauseMessage(e));
    }

    return container;
  }

  @Override
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
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

  /**
   * Returns the most recent {@link Document} from a list of {@link DocumentContainer}s.
   *
   * @param documentContainers A list of containers; each retrieved from a separate index.
   * @return The latest {@link Document} found.
   * @throws IOException If any of the {@link DocumentContainer}s contain an exception.
   */
  private Document getLatestDocument(List<DocumentContainer> documentContainers) throws IOException {
    Document latestDocument = null;
    List<String> error = new ArrayList<>();

    for(DocumentContainer dc : documentContainers) {
      if(dc.getException().isPresent()) {
        // collect each exception; multiple can occur, one in each index
        Throwable e = dc.getException().get();
        error.add(e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));

      } else if(dc.getDocument().isPresent()) {
        Document d = dc.getDocument().get();
        // is this the latest document so far?
        if(latestDocument == null || latestDocument.getTimestamp() < d.getTimestamp()) {
          latestDocument = d;
        }

      } else {
        // no document was found in the index
      }
    }
    if(error.size() > 0) {
      // report all of the errors encountered
      throw new IOException(Joiner.on("\n").join(error));
    }
    return latestDocument;
  }
}
