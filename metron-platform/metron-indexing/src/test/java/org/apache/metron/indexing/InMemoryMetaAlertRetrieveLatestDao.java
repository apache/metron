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
package org.apache.metron.indexing;

import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertRetrieveLatestDao;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.update.Document;

import java.io.IOException;
import java.util.List;

/**
 * This class is needed to compose an InMemoryMetaAlertUpdateDao implementation.  This allows the
 * InMemoryMetaAlertUpdateDao class to extend AbstractLuceneMetaAlertUpdateDao and reuse common logic in that class.
 */
public class InMemoryMetaAlertRetrieveLatestDao implements MetaAlertRetrieveLatestDao {

  private IndexDao indexDao;

  public InMemoryMetaAlertRetrieveLatestDao(IndexDao indexDao) {
    this.indexDao = indexDao;
  }

  @Override
  public Document getLatest(String guid, String sensorType) throws IOException {
    return indexDao.getLatest(guid, sensorType);
  }

  @Override
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
    return indexDao.getAllLatest(getRequests);
  }
}
