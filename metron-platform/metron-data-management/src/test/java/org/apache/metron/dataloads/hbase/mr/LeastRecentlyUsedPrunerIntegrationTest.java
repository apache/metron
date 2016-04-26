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
package org.apache.metron.dataloads.hbase.mr;

import com.google.common.collect.Iterables;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.Job;
import org.apache.metron.dataloads.bulk.LeastRecentlyUsedPruner;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.EnrichmentLookup;
import org.apache.metron.enrichment.lookup.LookupKey;
import org.apache.metron.enrichment.lookup.accesstracker.BloomAccessTracker;
import org.apache.metron.enrichment.lookup.accesstracker.PersistentAccessTracker;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeastRecentlyUsedPrunerIntegrationTest {
    /** The test util. */
    private HBaseTestingUtility testUtil;

    /** The test table. */
    private HTable testTable;
    private HTable atTable;
    String tableName = "malicious_domains";
    String cf = "cf";
    String atTableName = "access_trackers";
    String atCF= "cf";
    Configuration config = null;
    @Before
    public void setup() throws Exception {
        Map.Entry<HBaseTestingUtility, Configuration> kv = HBaseUtil.INSTANCE.create(true);
        config = kv.getValue();
        testUtil = kv.getKey();
        testTable = testUtil.createTable(Bytes.toBytes(tableName), Bytes.toBytes(cf));
        atTable = testUtil.createTable(Bytes.toBytes(atTableName), Bytes.toBytes(atCF));
    }
    @After
    public void teardown() throws Exception {
        HBaseUtil.INSTANCE.teardown(testUtil);
    }
    public List<LookupKey> getKeys(int start, int end) {
        List<LookupKey> keys = new ArrayList<>();
        for(int i = start;i < end;++i) {
            keys.add(new EnrichmentKey("type", "key-" + i));
        }
        return keys;
    }
    @Test
    public void test() throws Exception {
        long ts = System.currentTimeMillis();
        BloomAccessTracker bat = new BloomAccessTracker("tracker1", 100, 0.03);
        PersistentAccessTracker pat = new PersistentAccessTracker(tableName, "0", atTable, atCF, bat, 0L);
        EnrichmentLookup lookup = new EnrichmentLookup(testTable, cf, pat);
        List<LookupKey> goodKeysHalf = getKeys(0, 5);
        List<LookupKey> goodKeysOtherHalf = getKeys(5, 10);
        Iterable<LookupKey> goodKeys = Iterables.concat(goodKeysHalf, goodKeysOtherHalf);
        List<LookupKey> badKey = getKeys(10, 11);
        EnrichmentConverter converter = new EnrichmentConverter();
        for(LookupKey k : goodKeysHalf) {
            testTable.put(converter.toPut(cf, (EnrichmentKey) k
                                            , new EnrichmentValue(
                                                  new HashMap<String, String>() {{
                                                    put("k", "dummy");
                                                    }}
                                                  )
                                          )
                         );
            Assert.assertTrue(lookup.exists((EnrichmentKey)k, testTable, true));
        }
        pat.persist(true);
        for(LookupKey k : goodKeysOtherHalf) {
            testTable.put(converter.toPut(cf, (EnrichmentKey) k
                                            , new EnrichmentValue(new HashMap<String, String>() {{
                                                    put("k", "dummy");
                                                    }}
                                                                  )
                                         )
                         );
            Assert.assertTrue(lookup.exists((EnrichmentKey)k, testTable, true));
        }
        testUtil.flush();
        Assert.assertFalse(lookup.getAccessTracker().hasSeen(goodKeysHalf.get(0)));
        for(LookupKey k : goodKeysOtherHalf) {
            Assert.assertTrue(lookup.getAccessTracker().hasSeen(k));
        }
        pat.persist(true);
        {
            testTable.put(converter.toPut(cf, (EnrichmentKey) badKey.get(0)
                    , new EnrichmentValue(new HashMap<String, String>() {{
                        put("k", "dummy");
                    }}
                    )
                    )
            );
        }
        testUtil.flush();
        Assert.assertFalse(lookup.getAccessTracker().hasSeen(badKey.get(0)));


        Job job = LeastRecentlyUsedPruner.createJob(config, tableName, cf, atTableName, atCF, ts);
        Assert.assertTrue(job.waitForCompletion(true));
        for(LookupKey k : goodKeys) {
            Assert.assertTrue(lookup.exists((EnrichmentKey)k, testTable, true));
        }
        for(LookupKey k : badKey) {
            Assert.assertFalse(lookup.exists((EnrichmentKey)k, testTable, true));
        }

    }

}
