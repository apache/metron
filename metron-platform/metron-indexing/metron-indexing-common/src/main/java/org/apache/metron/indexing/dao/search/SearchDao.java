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
package org.apache.metron.indexing.dao.search;

public interface SearchDao {

  /**
   * Return search response based on the search request
   *
   * @param searchRequest The request defining the search parameters.
   * @return A response containing the results of the search.
   * @throws InvalidSearchException If the search request is malformed.
   */
  SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException;

  /**
   * Return group response based on the group request
   * @param groupRequest The request defining the grouping parameters.
   * @return A response containing the results of the grouping operation.
   * @throws InvalidSearchException If the grouping request is malformed.
   */
  GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException;
}
