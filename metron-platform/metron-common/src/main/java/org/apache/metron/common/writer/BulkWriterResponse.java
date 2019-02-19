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

import org.apache.storm.tuple.Tuple;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BulkWriterResponse {
    private Multimap<Throwable, Tuple> errors = ArrayListMultimap.create();
    private List<Tuple> successes = new ArrayList<>();

    public void addError(Throwable error, Tuple tuple) {
        errors.put(error, tuple);
    }

    /**
     * Adds provided errors and associated tuples.
     *
     * @param error The error to add
     * @param tuples Iterable of all tuples with the error
     */
    public void addAllErrors(Throwable error, Iterable<Tuple> tuples) {
        if(tuples != null) {
            errors.putAll(error, tuples);
        }
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void addSuccess(Tuple success) {
        successes.add(success);
    }

    /**
     * Adds all provided successes.
     *
     * @param allSuccesses Successes to add
     */
    public void addAllSuccesses(Iterable<Tuple> allSuccesses) {
        if(allSuccesses != null) {
            Iterables.addAll(successes, allSuccesses);
        }
    }

    public Map<Throwable, Collection<Tuple>> getErrors() {
        return errors.asMap();
    }

    public List<Tuple> getSuccesses() {
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
