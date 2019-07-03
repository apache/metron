/*
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
package org.apache.metron.hbase.client;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Table;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A mock {@link HBaseConnectionFactory} useful for testing.
 */
public class FakeHBaseConnectionFactory extends HBaseConnectionFactory {

  /**
   * A set of {@link Table}s that will be returned by all {@link Connection}
   * objects created by this factory.
   */
  private Map<TableName, Table> tables;

  public FakeHBaseConnectionFactory() {
    this.tables = new HashMap<>();
  }

  /**
   * The {@link Connection} returned by this factory will return the given table by
   * name when {@link Connection#getTable(TableName)} is called.
   *
   * @param tableName The name of the table.
   * @param table The table.
   * @return
   */
  public FakeHBaseConnectionFactory withTable(String tableName, Table table) {
    this.tables.put(TableName.valueOf(tableName), table);
    return this;
  }

  /**
   * The {@link Connection} returned by this factory will return a table by
   * name when {@link Connection#getTable(TableName)} is called.
   *
   * @param tableName The name of the table.
   * @return
   */
  public FakeHBaseConnectionFactory withTable(String tableName) {
    return withTable(tableName, mock(Table.class));
  }

  public Table getTable(String tableName) {
    return tables.get(TableName.valueOf(tableName));
  }

  @Override
  public Connection createConnection(Configuration configuration) throws IOException {
    Connection connection = mock(Connection.class);

    // the connection must return each of the given tables by name
    for(Map.Entry<TableName, Table> entry: tables.entrySet()) {
      TableName tableName = entry.getKey();
      Table table = entry.getValue();
      when(connection.getTable(eq(tableName))).thenReturn(table);
    }

    return connection;
  }
}
