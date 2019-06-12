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
package org.apache.metron.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterface;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

public interface TableProvider extends Serializable {
  HTableInterface getTable(Configuration config, String tableName) throws IOException;

  /**
   * Factory method that creates TableProviders.
   *
   * @param impl attempt to create this type of TableProvider
   * @param defaultSupplier provides default implementation if impl is null
   * @return New table provider
   */
  static TableProvider create(String impl, Supplier<TableProvider> defaultSupplier) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    if(impl == null) {
      return defaultSupplier.get();
    }
    Class<? extends TableProvider> clazz = (Class<? extends TableProvider>) Class.forName(impl);
    return clazz.getConstructor().newInstance();
  }
}
