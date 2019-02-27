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

import org.apache.metron.common.writer.MessageId;
import org.apache.metron.indexing.dao.update.Document;

import java.util.Map;

/**
 * A {@link Document} that is created from message id.
 */
public class MessageIdBasedDocument extends Document {

    private MessageId messageId;

    public MessageIdBasedDocument(Map<String, Object> document,
                                  String guid,
                                  String sensorType,
                                  Long timestamp,
                                  MessageId messageId) {
        super(document, guid, sensorType, timestamp);
        this.messageId = messageId;
    }

    public MessageId getMessageId() {
        return messageId;
    }
}
