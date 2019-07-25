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

package org.apache.metron.hbase.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;

public class HelperDao {

  public static void insertRecord(Table table, EnrichmentKey key, String cf, String value)
      throws IOException {
    Put put = createPut(key, cf, value);
    table.put(put);
  }

  private static Put createPut(EnrichmentKey rowKey, String cf, String value) throws IOException {
    return new EnrichmentConverter().toPut(cf, rowKey,
        new EnrichmentValue(JSONUtils.INSTANCE.load(value, JSONUtils.MAP_SUPPLIER)));
  }

  public static List<String> readRecords(Table table) throws Exception {
    Scan scan = new Scan();
    ResultScanner scanner = table.getScanner(scan);
    List<String> rows = new ArrayList<>();
    for (Result r = scanner.next(); r != null; r = scanner.next()) {
      rows.add(Bytes.toString(r.getRow()));
    }
    return rows;
  }

}
