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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.hadoop.conf.Configuration;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.hbase.client.HBaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caffeine cache writer implementation that will write to an HBase table.
 */
public class HBaseCacheWriter implements CacheWriter<String, String> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private TableProvider tableProvider;
  private final Configuration config;
  private final String tableName;
  private final String columnFamily;
  private final String columnQualifier;

  public HBaseCacheWriter(Configuration config, TableProvider tableProvider, String tableName,
      String columnFamily, String columnQualifier) {
    this.config = config;
    this.tableProvider = tableProvider;
    this.tableName = tableName;
    this.columnFamily = columnFamily;
    this.columnQualifier = columnQualifier;
  }

  /**
   * Writes a rowkey as provided by 'key' to the configured hbase table.
   * @param key value to use as a row key.
   * @param value not used.
   */
  @Override
  public void write(@Nonnull String key, @Nonnull String value) {
    LOG.debug("Calling hbase cache writer with key='{}', value='{}'", key, value);
    try (HBaseClient hbClient = new HBaseClient(this.tableProvider, this.config, this.tableName)) {
      LOG.debug("rowKey={}, columnFamily={}, columnQualifier={}, value={}", key, columnFamily,
          columnQualifier, value);
      hbClient.put(key, columnFamily, columnQualifier, value);
      LOG.debug("Done with put");
    } catch (IOException e) {
      throw new RuntimeException("Error writing to HBase table", e);
    }
    LOG.debug("Done calling hbase cache writer");
  }

  @Override
  public void delete(@Nonnull String key, @Nullable String value, @Nonnull RemovalCause cause) {
    // not implemented
  }

}
