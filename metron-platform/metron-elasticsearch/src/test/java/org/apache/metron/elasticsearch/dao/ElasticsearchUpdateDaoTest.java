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

import static org.mockito.Mockito.mock;

import org.apache.metron.elasticsearch.client.ElasticsearchClient;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.UpdateDaoTest;
import org.apache.metron.indexing.dao.update.UpdateDao;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Before;

/**
 * This class returns the ElasticsearchUpdateDao implementation to be used in UpdateDaoTest.  UpdateDaoTest contains a
 * common set of tests that all Dao implementations must pass.
 */
public class ElasticsearchUpdateDaoTest extends UpdateDaoTest {

  private AccessConfig accessConfig;
  private ElasticsearchRetrieveLatestDao retrieveLatestDao;
  private ElasticsearchUpdateDao updateDao;

  @Before
  public void setup() {
    accessConfig = new AccessConfig();
    retrieveLatestDao = mock(ElasticsearchRetrieveLatestDao.class);
    RestHighLevelClient highLevel = mock(RestHighLevelClient.class);
    ElasticsearchClient client = new ElasticsearchClient(mock(RestClient.class), highLevel);
    updateDao = new ElasticsearchUpdateDao(client, accessConfig, retrieveLatestDao);
  }

  @Override
  public UpdateDao getUpdateDao() {
    return updateDao;
  }
}
