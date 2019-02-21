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

package org.apache.metron.hbase.coprocessor;

import com.github.benmanes.caffeine.cache.CacheWriter;
import com.github.benmanes.caffeine.cache.RemovalCause;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.hbase.coprocessor.config.CoprocessorOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caffeine cache writer implementation that will write to an HBase table.
 */
public class HBaseCacheWriter implements CacheWriter<String, String> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private Connection connection;
  private Function<Configuration, Connection> connectionFactory;
  private final Configuration config;
  private final String tableName;
  private final String columnFamily;
  private final String columnQualifier;

  public HBaseCacheWriter(Configuration config,
      Function<Configuration, Connection> connectionFactory) {
    this.config = config;
    this.connectionFactory = connectionFactory;
    this.tableName = config.get(CoprocessorOptions.TABLE_NAME.getKey());
    this.columnFamily = config.get(CoprocessorOptions.COLUMN_FAMILY.getKey());
    this.columnQualifier = config.get(CoprocessorOptions.COLUMN_QUALIFIER.getKey());
  }

  @Override
  public void write(@Nonnull String key, @Nonnull String value) {
    Table enrichmentListTable = null;
    try {
      enrichmentListTable = getTable(tableName);
      LOG.debug("enrichment_list table retrieved '{}'",
          enrichmentListTable.getTableDescriptor().getFamilies());
      Put eListPut = new Put(Bytes.toBytes(key));
      eListPut.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(columnQualifier),
          Bytes.toBytes(value));
      LOG.debug("Performing put into '{}' of '{}'", tableName, eListPut);
      enrichmentListTable.put(eListPut);
      LOG.debug("Done with put in enrichment list table");
    } catch (IOException e) {
      throw new RuntimeException("Error writing to HBase table", e);
    } finally {
      LOG.info("Closing table");
      if (null != enrichmentListTable) {
        try {
          enrichmentListTable.close();
        } catch (IOException e) {
          // ignore
        }
      }
      LOG.debug("Done closing table");
    }
  }

  private Table getTable(String tableName) throws IOException {
    return getConnection().getTable(TableName.valueOf(tableName));
  }

  private Connection getConnection() throws IOException {
    if (null == this.connection || this.connection.isClosed()) {
      if (null == this.config) {
        throw new IOException("Config cannot be null.");
      }
      this.connection = connectionFactory.apply(this.config);
    }

    return this.connection;
  }

  @Override
  public void delete(@Nonnull String key, @Nullable String value, @Nonnull RemovalCause cause) {
    // not implemented
  }

}
