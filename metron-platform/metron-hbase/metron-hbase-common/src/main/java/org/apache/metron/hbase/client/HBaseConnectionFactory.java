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
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;

/**
 * Establishes a {@link Connection} to HBase.
 */
public class HBaseConnectionFactory implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public Connection createConnection(Configuration configuration) throws IOException {
    return ConnectionFactory.createConnection(configuration);
  }

  /**
   * Creates an {@link HBaseConnectionFactory} based on a fully-qualified class name.
   *
   * @param className The fully-qualified class name to instantiate.
   * @return A {@link HBaseConnectionFactory}.
   */
  public static HBaseConnectionFactory byName(String className) {
    LOG.debug("Creating HBase connection factory; className={}", className);
    try {
      Class<? extends HBaseConnectionFactory> clazz = (Class<? extends HBaseConnectionFactory>) Class.forName(className);
      return clazz.getConstructor().newInstance();

    } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException | InvocationTargetException e) {
      throw new IllegalStateException("Unable to instantiate HBaseConnectionFactory.", e);
    }
  }
}
