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

package org.apache.metron.enrichment.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.utils.SerDeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ObjectCache {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected LoadingCache<String, Object> cache;
  private static ReadWriteLock lock = new ReentrantReadWriteLock();
  protected LoadingCache<String, Object> getCache() {
    return cache;
  }

  public class Loader extends CacheLoader<String, Object> {
    FileSystem fs;

    public Loader(Configuration hadoopConfig) throws IOException {
      this.fs = FileSystem.get(hadoopConfig);
    }

    @Override
    public Object load(String s) throws Exception {
      LOG.debug("Loading object from path '{}'", s);
      if (StringUtils.isEmpty(s)) {
        return null;
      }
      Path p = new Path(s);
      if (fs.exists(p)) {
        try (InputStream is = new BufferedInputStream(fs.open(p))) {
          byte[] serialized = IOUtils.toByteArray(is);
          if (serialized.length > 0) {
            return SerDeUtils.fromBytes(serialized, Object.class);
          }
        }
      }
      LOG.warn("Path '{}' could not be found in HDFS", s);
      return null;
    }
  }

  public Object get(String path) {
    try {
      return cache.get(path);
    } catch (ExecutionException e) {
      throw new IllegalStateException("Unable to retrieve " + path + " because " + e.getMessage(), e);
    }
  }

  public void initialize(ObjectCacheConfig config) {
    try {
      lock.writeLock().lock();

      cache = setupCache(config);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to initialize: " + e.getMessage(), e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public boolean isInitialized() {
    try {
      lock.readLock().lock();
      return cache != null;
    } finally {
      lock.readLock().unlock();
    }
  }

  protected LoadingCache<String, Object> setupCache(ObjectCacheConfig config) throws IOException {
    LOG.info("Building ObjectCache with {}", config);
    return CacheBuilder.newBuilder()
            .maximumSize(config.getCacheSize())
            .expireAfterWrite(config.getCacheExpiration(), config.getTimeUnit())
            .removalListener(removalNotification -> {
              LOG.debug("Object retrieved from path '{}' was removed with cause {}", removalNotification.getKey(), removalNotification.getCause());
            })
            .build(new Loader(new Configuration()));
  }
}
