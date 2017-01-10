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
package org.apache.metron.maas.queue;

import org.apache.metron.maas.config.ModelRequest;

import java.util.Map;
import java.util.function.Function;

/**
 * The set of queue handlers implemented
 */
public enum QueueHandler {
  /**
   * A distributed queue using zookeeper.
   */
  ZOOKEEPER(config -> {
    Queue<ModelRequest> ret = new ZKQueue();
    ret.configure(config);
    return ret;
  });
  Function<Map<String, Object>, Queue<ModelRequest>> queueCreator;
  QueueHandler(Function<Map<String, Object>, Queue<ModelRequest>> creator) {
    this.queueCreator = creator;
  }

  /**
   * Create a queue handler of the specific type
   * @param config
   * @return
   */
  public Queue<ModelRequest> create(Map<String, Object> config) {
    return queueCreator.apply(config);
  }
}
