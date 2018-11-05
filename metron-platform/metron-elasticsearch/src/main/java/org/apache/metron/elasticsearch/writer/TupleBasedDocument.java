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
package org.apache.metron.elasticsearch.writer;

import org.apache.metron.indexing.dao.update.Document;
import org.apache.storm.tuple.Tuple;

import java.util.Map;

/**
 * An {@link Document} that is created from the contents of a {@link Tuple}.
 */
public class TupleBasedDocument extends Document {

    private Tuple tuple;

    public TupleBasedDocument(Map<String, Object> document,
                              String guid,
                              String sensorType,
                              Long timestamp,
                              Tuple tuple) {
        super(document, guid, sensorType, timestamp);
        this.tuple = tuple;
    }

    public Tuple getTuple() {
        return tuple;
    }
}
