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
package org.apache.metron.solr.dao;

import org.apache.metron.indexing.dao.update.Document;

/**
 * Responsible for building and deconstructing a {@link Document}
 * from an intermediate form.
 */
public interface DocumentBuilder<T> {

  /**
   * Builds a {@link Document} from the source document.
   *
   * @param sourceDocument The source document.
   * @return A {@link Document}.
   */
  Document toDocument(T sourceDocument);

  /**
   * Deconstructs a {@link Document} into the alternate intermediate form.
   *
   * @param document The document to deconstruct.
   * @return The alternate, intermediate document form.
   */
  T fromDocument(Document document);
}
