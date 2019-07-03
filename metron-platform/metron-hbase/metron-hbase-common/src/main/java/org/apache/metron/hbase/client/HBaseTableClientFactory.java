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

import java.lang.invoke.MethodHandles;

/**
 * Creates an {@link HBaseTableClient}.
 */
public class HBaseTableClientFactory implements HBaseClientFactory {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * @param factory The factory that creates connections to HBase.
   * @param configuration The HBase configuration.
   * @param tableName The name of the HBase table.
   * @return An {@link HBaseTableClient} that behaves synchronously.
   */
  @Override
  public HBaseClient create(HBaseConnectionFactory factory,
                            Configuration configuration,
                            String tableName) {
    try {
      LOG.debug("Creating HBase client; table={}", tableName);
      return new HBaseTableClient(factory, configuration, tableName);

    } catch (Exception e) {
      String msg = String.format("Unable to open connection to HBase for table '%s'", tableName);
      LOG.error(msg, e);
      throw new RuntimeException(msg, e);
    }
  }
}
