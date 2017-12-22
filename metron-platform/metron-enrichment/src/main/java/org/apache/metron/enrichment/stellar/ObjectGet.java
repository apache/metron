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

package org.apache.metron.enrichment.stellar;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.utils.SerDeUtils;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Stellar(namespace="OBJECT"
        ,name="GET"
        ,description="Retrieve and deserialize a serialized object from HDFS.  " +
        "The cache can be specified via two properties in the global config: " +
        "\"" + ObjectGet.OBJECT_CACHE_SIZE_KEY + "\" (default " + ObjectGet.OBJECT_CACHE_SIZE_DEFAULT + ")," +
        "\"" + ObjectGet.OBJECT_CACHE_EXPIRATION_KEY+ "\" (default 1440).  Note, if these are changed in global config, " +
        "topology restart is required."
        , params = {
            "path - The path in HDFS to the serialized object"
          }
        , returns="The deserialized object."
)
public class ObjectGet implements StellarFunction {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String OBJECT_CACHE_SIZE_KEY = "object.cache.size";
  public static final String OBJECT_CACHE_EXPIRATION_KEY = "object.cache.expiration.minutes";
  public static final int OBJECT_CACHE_SIZE_DEFAULT = 1000;
  public static final long OBJECT_CACHE_EXPIRATION_MIN_DEFAULT = TimeUnit.HOURS.toMinutes(24);
  protected static LoadingCache<String, Object> cache;
  private static ReadWriteLock lock = new ReentrantReadWriteLock();

  public static class Loader extends CacheLoader<String, Object> {
    FileSystem fs;
    public Loader(Configuration hadoopConfig) throws IOException {
      this.fs = FileSystem.get(hadoopConfig);
    }
    @Override
    public Object load(String s) throws Exception {
      if(StringUtils.isEmpty(s)) {
        return null;
      }
      Path p = new Path(s);
      if(fs.exists(p)) {
        try(InputStream is = new BufferedInputStream(fs.open(p))) {
          byte[] serialized = IOUtils.toByteArray(is);
          if(serialized.length > 0) {
            Object ret = SerDeUtils.fromBytes(serialized, Object.class);
            return ret;
          }
        }
      }
      return null;
    }
  }

  @Override
  public Object apply(List<Object> args, Context context) throws ParseException {
    if(!isInitialized()) {
      return null;
    }
    if(args.size() < 1) {
      return null;
    }
    Object o = args.get(0);
    if(o == null) {
      return null;
    }
    if(o instanceof String) {
      try {
        return cache.get((String)o);
      } catch (ExecutionException e) {
        throw new IllegalStateException("Unable to retrieve " + o + " because " + e.getMessage(), e);
      }
    }
    else {
      throw new IllegalStateException("Unable to retrieve " + o + " as it is not a path");
    }
  }

  @Override
  public void initialize(Context context) {
    try {
      lock.writeLock().lock();
      Map<String, Object> config = getConfig(context);
      long size = ConversionUtils.convert(config.getOrDefault(OBJECT_CACHE_SIZE_KEY, OBJECT_CACHE_SIZE_DEFAULT), Long.class);
      long expiryMin = ConversionUtils.convert(config.getOrDefault(OBJECT_CACHE_EXPIRATION_KEY, OBJECT_CACHE_EXPIRATION_MIN_DEFAULT), Long.class);
      cache = setupCache(size, expiryMin);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to initialize: " + e.getMessage(), e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean isInitialized() {
    try {
      lock.readLock().lock();
      return cache != null;
    }
    finally {
      lock.readLock().unlock();
    }
  }

  protected LoadingCache<String, Object> setupCache(long size, long expiryMin) throws IOException {
    return CacheBuilder.newBuilder()
                       .maximumSize(size)
                       .expireAfterAccess(expiryMin, TimeUnit.MINUTES)
                       .build(new Loader(new Configuration()));
  }

  protected Map<String, Object> getConfig(Context context) {
      return (Map<String, Object>) context.getCapability(Context.Capabilities.GLOBAL_CONFIG, false).orElse(new HashMap<>());
    }
}
