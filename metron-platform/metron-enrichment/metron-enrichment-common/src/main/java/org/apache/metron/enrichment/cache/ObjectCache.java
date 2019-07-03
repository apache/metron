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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;
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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ObjectCache {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected LoadingCache<String, Object> cache;
  private static ReadWriteLock lock = new ReentrantReadWriteLock();

  public class Loader implements CacheLoader<String, Object> {
    FileSystem fs;
    ObjectCacheConfig objectCacheConfig;

    public Loader(Configuration hadoopConfig, ObjectCacheConfig objectCacheConfig) throws IOException {
      this.fs = FileSystem.get(hadoopConfig);
      this.objectCacheConfig = objectCacheConfig;
    }

    @Override
    public Object load(String s) throws Exception {
      LOG.debug("Loading object from path '{}'", s);
      if (StringUtils.isEmpty(s)) {
        throw new IllegalArgumentException("Path cannot be empty");
      }
      Object object = null;
      Path p = new Path(s);
      if (fs.exists(p)) {
        if (fs.getFileStatus(p).getLen() <= objectCacheConfig.getMaxFileSize()) {
          try (InputStream is = new BufferedInputStream(fs.open(p))) {
            byte[] serialized = IOUtils.toByteArray(is);
            if (serialized.length > 0) {
              object = SerDeUtils.fromBytes(serialized, Object.class);
            }
          }
        } else {
          throw new IllegalArgumentException(String.format("File at path '%s' is larger than the configured max file size of %s", p, objectCacheConfig.getMaxFileSize()));
        }
      } else {
        throw new IllegalArgumentException(String.format("Path '%s' could not be found in HDFS", s));
      }
      return object;
    }
  }

  public Object get(String path) {
    return cache.get(path);
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
    return Caffeine.newBuilder().maximumSize(config.getCacheSize())
            .expireAfterWrite(config.getCacheExpiration(), config.getTimeUnit())
            .removalListener((path, value, removalCause) -> {
              LOG.debug("Object retrieved from path '{}' was removed with cause {}", path, removalCause);
            })
            .build(new Loader(new Configuration(), config));
  }

  public boolean isEmpty() {
      return cache == null || cache.estimatedSize() == 0;
  }

  public boolean containsKey(String key) {
      return cache != null && cache.asMap().containsKey(key);
  }
}
