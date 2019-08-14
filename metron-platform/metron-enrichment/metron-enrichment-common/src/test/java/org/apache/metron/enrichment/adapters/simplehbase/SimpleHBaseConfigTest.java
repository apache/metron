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

import org.apache.metron.enrichment.lookup.FakeEnrichmentLookupFactory;
import org.apache.metron.hbase.client.HBaseConnectionFactory;
import org.junit.Assert;
import org.junit.Test;

public class SimpleHBaseConfigTest {
    private final String cf ="cf";
    private final String table = "threatintel";

    @Test
    public void test() {
        FakeEnrichmentLookupFactory factory = new FakeEnrichmentLookupFactory();

        SimpleHBaseConfig shc = new SimpleHBaseConfig();
        shc.withHBaseCF(cf);
        shc.withHBaseTable(table);
        shc.withConnectionFactoryImpl(HBaseConnectionFactory.class.getName());
        shc.withEnrichmentLookupFactory(factory);

        Assert.assertEquals(cf, shc.getHBaseCF());
        Assert.assertEquals(table, shc.getHBaseTable());
        Assert.assertTrue(shc.getConnectionFactory() instanceof HBaseConnectionFactory);
        Assert.assertEquals(factory, shc.getEnrichmentLookupFactory());
    }
}
