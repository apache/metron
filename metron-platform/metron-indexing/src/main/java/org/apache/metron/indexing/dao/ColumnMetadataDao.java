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

import org.apache.metron.indexing.dao.search.FieldType;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Responsible for retrieving column-level metadata about search indices.
 */
public interface ColumnMetadataDao {

  /**
   * Retrieves column metadata for one or more search indices.
   * @param indices The search indices to retrieve column metadata for.
   * @return The column metadata, one set for each search index.
   * @throws IOException
   */
  Map<String, FieldType> getColumnMetadata(List<String> indices) throws IOException;
}
