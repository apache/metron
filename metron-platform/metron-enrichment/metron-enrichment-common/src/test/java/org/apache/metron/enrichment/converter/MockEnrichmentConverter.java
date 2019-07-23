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
package org.apache.metron.enrichment.converter;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.enrichment.lookup.EnrichmentResult;
import org.apache.metron.hbase.client.FakeHBaseConnectionFactory;

import java.io.IOException;

public class MockEnrichmentConverter extends EnrichmentConverter {

  public MockEnrichmentConverter() {
    super("tableName", new FakeHBaseConnectionFactory(), HBaseConfiguration.create());
  }

  // TODO need to implement each of these in a way that is useful for testing.

  @Override
  public void put(String columnFamily, EnrichmentKey key, EnrichmentValue values) throws IOException {
    super.put(columnFamily, key, values);
  }

  @Override
  public Result toResult(String columnFamily, EnrichmentKey key, EnrichmentValue values) throws IOException {
    return super.toResult(columnFamily, key, values);
  }

  @Override
  public EnrichmentResult fromResult(Result result, String columnFamily, EnrichmentKey key, EnrichmentValue value) throws IOException {
    return super.fromResult(result, columnFamily, key, value);
  }

  @Override
  public Get toGet(String columnFamily, EnrichmentKey key) {
    return super.toGet(columnFamily, key);
  }

  @Override
  public EnrichmentResult fromResult(Result result, String columnFamily) throws IOException {
    return super.fromResult(result, columnFamily);
  }
}
