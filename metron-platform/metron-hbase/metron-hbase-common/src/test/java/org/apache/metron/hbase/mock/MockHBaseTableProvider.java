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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.hbase.TableProvider;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MockHBaseTableProvider implements Serializable, TableProvider {
  private static Map<String, HTableInterface> _cache = new HashMap<>();
  public HTableInterface getTable(Configuration config, String tableName) throws IOException {
    HTableInterface ret = _cache.get(tableName);
    return ret;
  }

  public static HTableInterface getFromCache(String tableName) {
    return _cache.get(tableName);
  }

  public static HTableInterface addToCache(String tableName, String... columnFamilies) {
    MockHTable ret =  new MockHTable(tableName, columnFamilies);
    _cache.put(tableName, ret);
    return ret;
  }

  public static void clear() {
    _cache.clear();
  }
}
