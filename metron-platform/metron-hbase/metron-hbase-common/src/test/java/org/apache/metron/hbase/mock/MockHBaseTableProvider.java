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
package org.apache.metron.hbase.mock;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Table;
import org.apache.metron.hbase.TableProvider;

public class MockHBaseTableProvider implements Serializable, TableProvider {
  private static Map<String, Table> _cache = new HashMap<>();
  public Table getTable(Configuration configuration, String tableName) throws IOException {
    Table ret = _cache.get(tableName);
    return ret;
  }

  public static Table getFromCache(String tableName) {
    return _cache.get(tableName);
  }

  public static Table addToCache(String tableName, String... columnFamilies) {
    MockHTable ret =  new MockHTable(tableName, columnFamilies);
    _cache.put(tableName, ret);
    return ret;
  }

  public static void clear() {
    _cache.clear();
  }

}
