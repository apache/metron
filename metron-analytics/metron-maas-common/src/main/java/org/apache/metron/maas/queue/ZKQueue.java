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

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.queue.SimpleDistributedQueue;
import org.apache.metron.maas.config.ModelRequest;
import org.apache.metron.maas.util.ConfigUtil;

import java.util.Map;

public class ZKQueue implements Queue<ModelRequest> {
  public static String ZK_PATH = "zk_path";
  public static String ZK_CLIENT = "zk_client";
  private SimpleDistributedQueue queue;
  @Override
  public ModelRequest dequeue() {
    try {
      byte[] payload = queue.take();
      return ConfigUtil.INSTANCE.read(payload, ModelRequest.class);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to dequeue: " + e.getMessage(), e);
    }
  }

  @Override
  public void enqueue(ModelRequest request) {
    try {
      byte[] payload = ConfigUtil.INSTANCE.toBytes(request);
      queue.offer(payload);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to enqueue: " + e.getMessage(), e);
    }
  }

  @Override
  public void configure(Map<String, Object> config) {
    String path = (String)config.get(ZK_PATH);
    if(path == null) {
      throw new IllegalStateException("You must specify " + ZK_PATH + " for a zk queue");
    }
    CuratorFramework client = (CuratorFramework) config.get(ZK_CLIENT);
    queue = new SimpleDistributedQueue(client, path);
  }
}
