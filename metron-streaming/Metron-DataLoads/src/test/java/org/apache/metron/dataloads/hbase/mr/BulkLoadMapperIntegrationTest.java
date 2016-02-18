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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.Job;
import org.apache.metron.dataloads.ThreatIntelBulkLoader;
import org.apache.metron.threatintel.ThreatIntelResults;
import org.apache.metron.threatintel.hbase.Converter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BulkLoadMapperIntegrationTest {
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

  @Test
  public void test() throws IOException, ClassNotFoundException, InterruptedException {
 /**
         {
            "config" : {
                        "columns" : {
                                "host" : 0
                                ,"meta" : 2
                                    }
                       ,"indicator_column" : "host"
                       ,"separator" : ","
                       }
            ,"extractor" : "CSV"
         }
         */
        final String extractorConfig = "{\n" +
                "            \"config\" : {\n" +
                "                        \"columns\" : [\"host:0\",\"meta:2\"]\n" +
                "                       ,\"indicator_column\" : \"host\"\n" +
                "                       ,\"separator\" : \",\"\n" +
                "                       }\n" +
                "            ,\"extractor\" : \"CSV\"\n" +
                "         }";
    Assert.assertNotNull(testTable);
    FileSystem fs = FileSystem.get(config);
    String contents = "google.com,1,foo";
    HBaseUtil.INSTANCE.writeFile(contents, new Path("input.csv"), fs);
    Job job = ThreatIntelBulkLoader.createJob(config, "input.csv", tableName, cf, extractorConfig, 0L);
    Assert.assertTrue(job.waitForCompletion(true));
    ResultScanner scanner = testTable.getScanner(Bytes.toBytes(cf));
    List<Map.Entry<ThreatIntelResults, Long>> results = new ArrayList<>();
    for(Result r : scanner) {
      results.add(Converter.INSTANCE.fromResult(r, cf));
    }
    Assert.assertEquals(1, results.size());
    Assert.assertEquals(0L, (long)results.get(0).getValue());
    Assert.assertEquals(results.get(0).getKey().getKey().indicator, "google.com");
    Assert.assertEquals(results.get(0).getKey().getValue().size(), 2);
    Assert.assertEquals(results.get(0).getKey().getValue().get("meta"), "foo");
    Assert.assertEquals(results.get(0).getKey().getValue().get("host"), "google.com");
  }
}
