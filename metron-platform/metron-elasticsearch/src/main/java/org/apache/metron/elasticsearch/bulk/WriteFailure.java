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
package org.apache.metron.elasticsearch.bulk;

import org.apache.metron.indexing.dao.update.Document;

/**
 * Indicates that a document failed to be written by a {@link BulkDocumentWriter}.
 * @param <D> The type of document that failed to write.
 */
public class WriteFailure <D extends Document> {
  private D document;
  private Throwable cause;
  private String message;

  public WriteFailure(D document, Throwable cause, String message) {
    this.document = document;
    this.cause = cause;
    this.message = message;
  }

  public D getDocument() {
    return document;
  }

  public Throwable getCause() {
    return cause;
  }

  public String getMessage() {
    return message;
  }
}
