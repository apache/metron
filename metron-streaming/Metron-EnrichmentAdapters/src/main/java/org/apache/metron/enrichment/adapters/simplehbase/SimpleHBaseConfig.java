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
package org.apache.metron.enrichment.adapters.simplehbase;

import org.apache.metron.enrichment.utils.EnrichmentUtils;
import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.TableProvider;

import java.io.Serializable;


public class SimpleHBaseConfig implements Serializable {
  private String hBaseTable;
  private String hBaseCF;
  private TableProvider provider = new HTableProvider();
  public String getHBaseTable() {
    return hBaseTable;
  }
  public String getHBaseCF() {
    return hBaseCF;
  }

  public TableProvider getProvider() {
    return provider;
  }

  public SimpleHBaseConfig withProviderImpl(String connectorImpl) {
    provider = EnrichmentUtils.getTableProvider(connectorImpl, new HTableProvider());
    return this;
  }
  public SimpleHBaseConfig withHBaseTable(String hBaseTable) {
    this.hBaseTable = hBaseTable;
    return this;
  }

  public SimpleHBaseConfig withHBaseCF(String cf) {
    this.hBaseCF= cf;
    return this;
  }
}
