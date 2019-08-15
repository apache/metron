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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;

public class HBaseTableProvider implements TableProvider {

  private ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<>();

  @Override
  public Table getTable(Configuration config, String tableName)
      throws IOException {
    return getConnection(config).getTable(TableName.valueOf(tableName));
  }

  private Connection getConnection(Configuration config) throws IOException {
    Connection connection = connectionThreadLocal.get();
    if (null == connection || connection.isClosed()) {
      connectionThreadLocal.set(ConnectionFactory.createConnection(config));
    }
    return connectionThreadLocal.get();
  }

  @Override
  public void close() throws IOException {
    Connection connection = connectionThreadLocal.get();
    if (null != connection) {
      connection.close();
    }
  }

}
