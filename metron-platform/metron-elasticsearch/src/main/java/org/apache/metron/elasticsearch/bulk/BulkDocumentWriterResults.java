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

import java.util.ArrayList;
import java.util.List;

/**
 * The result of writing documents in bulk using a {@link BulkDocumentWriter}.
 * @param <D> The type of documents to write.
 */
public class BulkDocumentWriterResults<D extends Document> {

  private List<WriteSuccess<D>> successes;
  private List<WriteFailure<D>> failures;

  public BulkDocumentWriterResults() {
    this.successes = new ArrayList<>();
    this.failures = new ArrayList<>();
  }

  public void add(WriteSuccess<D> success) {
    this.successes.add(success);
  }

  public void addSuccess(D success) {
    add(new WriteSuccess<D>(success));
  }

  public void addSuccesses(List<D> successes) {
    for(D success: successes) {
      addSuccess(success);
    }
  }

  public List<WriteSuccess<D>> getSuccesses() {
    return successes;
  }

  public void add(WriteFailure<D> failure) {
    this.failures.add(failure);
  }

  public void addFailure(D document, Throwable cause, String message) {
    add(new WriteFailure(document, cause, message));
  }

  public List<WriteFailure<D>> getFailures() {
    return failures;
  }
}
