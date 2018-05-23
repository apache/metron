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

import org.apache.metron.indexing.dao.search.SearchDao;
import org.apache.metron.indexing.dao.update.UpdateDao;

/**
 * The IndexDao provides a common interface for retrieving and storing data in a variety of persistent stores.
 * Document reads and writes require a GUID and sensor type with an index being optional.
 */
public interface IndexDao extends UpdateDao, SearchDao, RetrieveLatestDao, ColumnMetadataDao {

  /**
   * Initialize the DAO with the AccessConfig object.
   * @param config The config to use for initialization
   */
  void init(AccessConfig config);
}
