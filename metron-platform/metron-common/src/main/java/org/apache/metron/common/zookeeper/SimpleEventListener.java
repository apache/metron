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
package org.apache.metron.common.zookeeper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class SimpleEventListener implements TreeCacheListener {

  public interface Callback {
    void apply(CuratorFramework client, String path, byte[] data) throws IOException;
  }

  public static class Builder {
    private EnumMap<TreeCacheEvent.Type, List<Callback>> callbacks = new EnumMap<>(TreeCacheEvent.Type.class);
    public Builder with(Callback callback, TreeCacheEvent.Type... types) {
      return with(ImmutableList.of(callback), types);
    }

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
    List<Callback> callback = callbacks.get(event.getType());
    if(callback != null) {
      for(Callback cb : callback) {
        cb.apply(client, path, data);
      }
    }
  }

}
