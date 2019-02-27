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

package org.apache.metron.common.writer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.apache.metron.common.configuration.writer.WriterConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class contains the results of a {@link org.apache.metron.common.writer.BulkMessageWriter#write(String, WriterConfiguration, List)}
 * call.  Each message in a batch either succeeds or fails and is represented in the response as a
 * {@link org.apache.metron.common.writer.MessageId}.
 */
public class BulkWriterResponse {
    private Multimap<Throwable, MessageId> errors = ArrayListMultimap.create();
    private List<MessageId> successes = new ArrayList<>();

    public void addError(Throwable error, MessageId id) {
        errors.put(error, id);
    }

  /**
   * Adds provided errors and associated tuples.
   *
   * @param error The error to add
   * @param ids Iterable of all messages with the error
   */
    public void addAllErrors(Throwable error, Iterable<MessageId> ids) {
        if(ids != null) {
            errors.putAll(error, ids);
        }
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void addSuccess(MessageId success) {
        successes.add(success);
    }

  /**
   * Adds all provided successes.
   *
   * @param allSuccesses Successes to add
   */
    public void addAllSuccesses(Iterable<MessageId> allSuccesses) {
        if(allSuccesses != null) {
            Iterables.addAll(successes, allSuccesses);
        }
    }

    public Map<Throwable, Collection<MessageId>> getErrors() {
        return errors.asMap();
    }

    public List<MessageId> getSuccesses() {
        return successes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BulkWriterResponse that = (BulkWriterResponse) o;

        if (!errors.equals(that.errors)) return false;
        return successes.equals(that.successes);

    }

    @Override
    public int hashCode() {
        int result = errors.hashCode();
        result = 31 * result + successes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BulkWriterResponse{" +
                "errors=" + errors +
                ", successes=" + successes +
                '}';
    }
}
