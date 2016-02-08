package org.apache.metron.dataloads.hbase.mr;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.Job;
import org.apache.metron.dataloads.LeastRecentlyUsedPruner;
import org.apache.metron.reference.lookup.LookupKey;
import org.apache.metron.reference.lookup.accesstracker.AccessTrackerUtil;
import org.apache.metron.reference.lookup.accesstracker.BloomAccessTracker;
import org.apache.metron.threatintel.ThreatIntelKey;
import org.apache.metron.threatintel.hbase.Converter;
import org.apache.metron.threatintel.hbase.ThreatIntelLookup;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cstella on 2/8/16.
 */
public class LeastRecentlyUsedPrunerIntegrationTest {
    /** The test util. */
    private HBaseTestingUtility testUtil;

    /** The test table. */
    private HTable testTable;
    String tableName = "malicious_domains";
    String cf = "cf";
    Configuration config = null;
    @Before
    public void setup() throws Exception {
        Map.Entry<HBaseTestingUtility, Configuration> kv = HBaseUtil.INSTANCE.create(true);
        config = kv.getValue();
        testUtil = kv.getKey();
        testTable = testUtil.createTable(Bytes.toBytes(tableName), Bytes.toBytes(cf));
    }
    @After
    public void teardown() throws Exception {
        HBaseUtil.INSTANCE.teardown(testUtil);
    }
    public List<LookupKey> getKeys(int start, int end) {
        List<LookupKey> keys = new ArrayList<>();
        for(int i = start;i < end;++i) {
            keys.add(new ThreatIntelKey("key-" + i));
        }
        return keys;
    }
    @Test
    public void test() throws Exception {
        ThreatIntelLookup lookup = new ThreatIntelLookup(testTable, cf, new BloomAccessTracker("tracker1", 100, 0.03));
        List<LookupKey> goodKeys = getKeys(0, 10);
        List<LookupKey> badKey = getKeys(10, 11);
        for(LookupKey k : goodKeys) {
            testTable.put(Converter.INSTANCE.toPut(cf, (ThreatIntelKey) k
                                                  , new HashMap<String, String>() {{
                                                    put("k", "dummy");
                                                    }}
                                                  , 1L
                                                  )
                         );
            Assert.assertTrue(lookup.exists((ThreatIntelKey)k, testTable));
        }
        {
            testTable.put(Converter.INSTANCE.toPut(cf, (ThreatIntelKey) badKey.get(0)
                    , new HashMap<String, String>() {{
                        put("k", "dummy");
                    }}
                    , 1L
                    )
            );
        }
        testUtil.flush();
        Assert.assertFalse(lookup.getAccessTracker().hasSeen(badKey.get(0)));
        for(LookupKey k : goodKeys) {
            Assert.assertTrue(lookup.getAccessTracker().hasSeen(k));
        }
        FileSystem fs = FileSystem.get(config);
        Path basePath = new Path("/tmp/trackers");
        fs.mkdirs(basePath);
        AccessTrackerUtil.INSTANCE.persistTracker(fs, AccessTrackerUtil.INSTANCE.getSavePath(basePath, lookup.getAccessTracker(), 1), lookup.getAccessTracker());

        Job job = LeastRecentlyUsedPruner.createJob(config, tableName, cf, basePath.toString(), 0L);
        Assert.assertTrue(job.waitForCompletion(true));
        for(LookupKey k : goodKeys) {
            Assert.assertTrue(lookup.exists((ThreatIntelKey)k, testTable));
        }
        for(LookupKey k : badKey) {
            Assert.assertFalse(lookup.exists((ThreatIntelKey)k, testTable));
        }

    }

}
