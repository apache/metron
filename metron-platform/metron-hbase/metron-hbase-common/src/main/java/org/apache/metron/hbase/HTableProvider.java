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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;

public class HTableProvider implements TableProvider {

  private static class RetryingConnection {

    private Configuration config;
    private Connection conn;

    RetryingConnection(Configuration config) {
      this.config = config;
    }

    public Connection getUnderlying() throws IOException {
      if (conn == null || conn.isClosed()) {
        conn = ConnectionFactory.createConnection(config);
      }
      return conn;
    }
  }

  /**
   * We have to handle serialization issues with Storm via indirections. Rather than re-implement
   * the interface everywhere we touch HBase, we can use a lazy initialization scheme to encapsulate
   * this within the HTableProvider. This is a sort of poor man's connection pool.
   */
  private static Map<Configuration, ThreadLocal<RetryingConnection>> connMap = new ConcurrentHashMap<>();

  @Override
  public Table getTable(Configuration config, String tableName)
      throws IOException {
    return getConnection(config).getTable(TableName.valueOf(tableName));
  }

  private Connection getConnection(Configuration config) throws IOException {
    ThreadLocal<RetryingConnection> threadLocal = connMap.computeIfAbsent(config, c -> ThreadLocal.withInitial(() -> new RetryingConnection(config)));
    return threadLocal.get().getUnderlying();
  }

}
