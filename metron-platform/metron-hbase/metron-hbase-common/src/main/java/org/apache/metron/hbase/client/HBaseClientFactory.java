/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.metron.hbase.client;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

/**
 * Responsible for creating an {@link HBaseTableClient}.
 */
public interface HBaseClientFactory extends Serializable {
  Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * @param factory The connection factory for creating connections to HBase.
   * @param configuration The HBase configuration.
   * @param tableName The name of the HBase table.
   * @return An {@link HBaseTableClient}.
   */
  HBaseClient create(HBaseConnectionFactory factory, Configuration configuration, String tableName);

  /**
   * Instantiates a new {@link HBaseClientFactory} by class name.
   *
   * @param className The class name of the {@link HBaseClientFactory} to instantiate.
   * @param defaultImpl The default instance to instantiate if the className is invalid.
   * @return A new {@link HBaseClientFactory}.
   */
  static HBaseClientFactory byName(String className, Supplier<HBaseClientFactory> defaultImpl) {
    LOG.debug("Creating HBase client creator; className={}", className);

    if(className == null || className.length() == 0 || className.charAt(0) == '$') {
      LOG.debug("Using default hbase client creator");
      return defaultImpl.get();

    } else {
      try {
        Class<? extends HBaseClientFactory> clazz = (Class<? extends HBaseClientFactory>) Class.forName(className);
        return clazz.getConstructor().newInstance();

      } catch(InstantiationException | IllegalAccessException | InvocationTargetException |
              NoSuchMethodException | ClassNotFoundException e) {
        throw new IllegalStateException("Unable to instantiate connector.", e);
      }
    }
  }
}
