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

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.mutation.Mutation;
import org.apache.metron.indexing.mutation.MutationException;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public interface IndexDao {
  SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException;
  void init(Map<String, Object> globalConfig, AccessConfig config);
  Document getLatest(String uuid, String sensorType) throws IOException;
  void update(Document update, WriterConfiguration configurations) throws IOException;

  default void update( final Document original
                     , Mutation mutation
                     , Optional<Long> timestamp
                     , WriterConfiguration configurations
                     ) throws IOException, MutationException
  {
    String mutated = null;
    try {
      mutated =
      mutation.apply(() -> {
        try {
          return JSONUtils.INSTANCE.load(original.document, JsonNode.class);
        } catch (IOException e) {
          throw new IllegalStateException(e.getMessage(), e);
        }
      });
    }
    catch(Exception ex) {
      throw new MutationException(ex.getMessage(), ex);
    }
    Document updated = new Document(mutated, original.getUuid(), original.getSensorType(), timestamp.orElse(null));
    update(updated, configurations);
  }

  default void update(String uuid, String sensorType, Mutation mutation, Optional<Long> timestamp, WriterConfiguration configurations) throws IOException, MutationException
  {
    Document latest = getLatest(uuid, sensorType);
    if(latest == null) {
      throw new IllegalStateException("Unable to retrieve message with UUID: " + uuid + " please use the update() method that specifies the document.");
    }
    update(latest, mutation, timestamp, configurations);
  }
}
