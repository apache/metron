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

import java.util.Map;

public class SearchResult {

  private String id;
  private Map<String, Object> source;
  private float score;
  private String index;

  /**
   * The index that the result comes from
   * @return
   */
  public String getIndex() {
    return index;
  }

  public void setIndex(String index) {
    this.index = index;
  }

  /**
   * The ID of the document from the index.
   * @return
   */
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  /**
   * The source (the actual result).
   * @return
   */
  public Map<String, Object> getSource() {
    return source;
  }

  public void setSource(Map<String, Object> source) {
    this.source = source;
  }

  /**
   * The score from the index.
   * @return
   */
  public float getScore() {
    return score;
  }

  public void setScore(float score) {
    this.score = score;
  }
}
