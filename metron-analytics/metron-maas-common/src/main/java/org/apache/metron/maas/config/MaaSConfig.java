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
package org.apache.metron.maas.config;

import org.apache.metron.maas.queue.Queue;
import org.apache.metron.maas.queue.QueueHandler;
import org.apache.metron.maas.queue.ZKQueue;

import java.util.HashMap;
import java.util.Map;

/**
 * The base configuration for Model as a Service
 */
public class MaaSConfig {
  private QueueHandler queue = QueueHandler.ZOOKEEPER;
  private Map<String, Object> queueConfig = new HashMap<String, Object>() {{
    put(ZKQueue.ZK_PATH, "/maas/queue");
  }};
  private String serviceRoot = "/maas/service";

  /**
   * Return the zookeeper path for the discovery service.  This is defaulted to /maas/service
   * @return serviceRoot
   */
  public String getServiceRoot() {
    return serviceRoot;
  }

  public void setServiceRoot(String serviceRoot) {
    this.serviceRoot = serviceRoot;
  }


  /**
   * Get the distributed queue implementation handler.  By default, we use a queue in zookeeper
   * as implemented by Apache Curator.
   * @return queue implementation handler
   */
  public QueueHandler getQueue() {
    return queue;
  }

  public void setQueue(QueueHandler queue) {
    this.queue = queue;
  }

  public Map<String, Object> getQueueConfig() {
    return queueConfig;
  }

  public void setQueueConfig(Map<String, Object> queueConfig) {
    this.queueConfig = queueConfig;
  }
  public Queue<ModelRequest> createQueue(Map<String, Object> additionalConfig) {
    Map<String, Object> configs = new HashMap<>(getQueueConfig());
    configs.putAll(additionalConfig);
    return getQueue().create(configs);
  }

}
