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
package org.apache.metron.zookeeper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.nio.charset.StandardCharsets;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * This is a simple convenience implementation of a TreeCacheListener.
 * It allows multiple callbacks to be called with one listener.
 */
public class SimpleEventListener implements TreeCacheListener {

  private static final Logger LOG =  LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The callback interface.  This is to be implemented for all callbacks bound to a SimpleEventListener
   */
  public interface Callback {
    /**
     * Called upon an event.
     * @param client The zookeeper client
     * @param path The zookeeper path changed
     * @param data The data that changed.
     * @throws IOException
     */
    void apply(CuratorFramework client, String path, byte[] data) throws IOException;
  }

  /**
   * Builder to create a SimpleEventListener
   */
  public static class Builder {
    private EnumMap<TreeCacheEvent.Type, List<Callback>> callbacks = new EnumMap<>(TreeCacheEvent.Type.class);

    /**
     * Add a callback bound to one or more TreeCacheEvent.Type.
     * @param callback The callback to be called when an event of each of types happens
     * @param types The zookeeper event types to bind to
     * @return The Builder
     */
    public Builder with(Callback callback, TreeCacheEvent.Type... types) {
      return with(ImmutableList.of(callback), types);
    }

    /**
     * Add a callback bound to one or more TreeCacheEvent.Type.
     * @param callback The iterable of callbacks to be called when an event of each of types happens
     * @param types The zookeeper event types to bind to
     * @return The Builder
     */
    public Builder with(Iterable<? extends Callback> callback, TreeCacheEvent.Type... types) {
      for(TreeCacheEvent.Type t : types) {
        List<Callback> cbs = callbacks.get(t);
        if(cbs == null) {
          cbs = new ArrayList<>();
        }
        Iterables.addAll(cbs, callback);
        callbacks.put(t, cbs);
      }
      return this;
    }

    /**
     * Create the listener.
     * @return The SimpleEventListener
     */
    public SimpleEventListener build() {
      return new SimpleEventListener(callbacks);
    }

  }

  EnumMap<TreeCacheEvent.Type, List<Callback>> callbacks;

  private SimpleEventListener(EnumMap<TreeCacheEvent.Type, List<Callback>> callbacks) {
    this.callbacks = callbacks;
  }

  @Override
  public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
    String path = null;
    byte[] data = null;
    if(event != null && event.getData() != null) {
      path = event.getData().getPath();
      data = event.getData().getData();
    }
    LOG.debug("Type: {}, Path: {}, Data: {}", event.getType(), (path == null?"":path) , (data == null?"":new String(data,
        StandardCharsets.UTF_8)));
    List<Callback> callback = callbacks.get(event.getType());
    if(callback != null) {
      for(Callback cb : callback) {
        cb.apply(client, path, data);
      }
    }
  }

}
