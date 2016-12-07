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
package org.apache.metron.maas.service;

import com.google.common.collect.Maps;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.metron.maas.config.Model;
import org.apache.metron.maas.config.ModelRequest;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class ContainerTracker {
  private final TreeMap<Integer, BlockingQueue<Container>> acceptedContainersByResource = Maps.newTreeMap();
  private HashMap<Model, List<Container>> launchedContainers = Maps.newHashMap();

  int minimumContainerSize;
  public ContainerTracker(int minimumContainerSize) {
    this.minimumContainerSize = minimumContainerSize;
  }

  public int getAdjustedSize(int size) {
    return (int)(minimumContainerSize * Math.ceil(1.0*size/minimumContainerSize));
  }

  public BlockingQueue<Container> getQueue(Resource resource) {
    synchronized(acceptedContainersByResource) {
      int key = getAdjustedSize(resource.getMemory());
      BlockingQueue<Container> queue = acceptedContainersByResource.get(key);
      if(queue == null) {
        queue = new LinkedBlockingDeque<>();
        acceptedContainersByResource.put(key,queue );
      }
      return queue;
    }
  }

  public void removeContainer(ContainerId container) {
    synchronized(acceptedContainersByResource) {
      for(Map.Entry<Model, List<Container>> kv : launchedContainers.entrySet()) {
        for(Iterator<Container> it = kv.getValue().iterator();it.hasNext();) {
          Container c = it.next();
          if(c.getId().equals(container)) {
            it.remove();
          }
        }
      }
    }
  }
  public List<Container> getList(ModelRequest request) {
    synchronized(acceptedContainersByResource) {
      List<Container> containers = launchedContainers.get(new Model(request.getName(), request.getVersion()));
      if(containers == null) {
        containers = new ArrayList<>();
        launchedContainers.put(new Model(request.getName(), request.getVersion()), containers);
      }
      return containers;
    }
  }
  public void registerContainer(Resource resource, Container container ) {
    synchronized(acceptedContainersByResource) {
      BlockingQueue<Container> queue = getQueue(resource);
      queue.add(container);
    }
  }
  public void registerRequest(Container container, ModelRequest request) {
    synchronized (acceptedContainersByResource) {
      getList(request).add(container);
    }
  }
}
