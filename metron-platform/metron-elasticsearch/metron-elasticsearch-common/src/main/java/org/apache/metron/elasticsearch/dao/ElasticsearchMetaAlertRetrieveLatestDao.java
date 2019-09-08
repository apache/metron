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

package org.apache.metron.elasticsearch.dao;

import java.io.IOException;
import java.util.List;
import org.apache.metron.indexing.dao.RetrieveLatestDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertRetrieveLatestDao;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.update.Document;

public class ElasticsearchMetaAlertRetrieveLatestDao implements MetaAlertRetrieveLatestDao {
  private RetrieveLatestDao retrieveLatestDao;

  public ElasticsearchMetaAlertRetrieveLatestDao(RetrieveLatestDao retrieveLatestDao) {
    this.retrieveLatestDao = retrieveLatestDao;
  }

  @Override
  public Document getLatest(String guid, String sensorType) throws IOException {
    return retrieveLatestDao.getLatest(guid, sensorType);
  }

  @Override
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
    return retrieveLatestDao.getAllLatest(getRequests);
  }
}
