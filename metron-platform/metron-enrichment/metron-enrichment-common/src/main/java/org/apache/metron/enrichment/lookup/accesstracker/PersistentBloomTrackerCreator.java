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
package org.apache.metron.enrichment.lookup.accesstracker;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.stellar.common.utils.ConversionUtils;

public class PersistentBloomTrackerCreator implements AccessTrackerCreator {

  public static class Config {
    public static final String PERSISTENT_BLOOM_TABLE = "pbat.table";
    public static final String PERSISTENT_BLOOM_CF = "pbat.cf";
    public static final String PERSISTENT_BLOOM_FP = "pbat.false_positive";
    public static final String PERSISTENT_BLOOM_EI = "pbat.expected_insertions";
    public static final String PERSISTENT_BLOOM_MS_BETWEEN_PERSISTS = "pbat.ms_between_persists";
    public static final long MS_IN_HOUR = 10000*60*60;
    private String hBaseTable;
    private String hBaseCF;
    private double falsePositiveRate = 0.03;
    private int expectedInsertions = 100000;
    private long millisecondsBetweenPersists = 2*MS_IN_HOUR;
    public Config(Map<String, Object> config) {
      hBaseTable = (String) config.get(PERSISTENT_BLOOM_TABLE);
      hBaseCF = (String) config.get(PERSISTENT_BLOOM_CF);
      Object fpObj = config.get(PERSISTENT_BLOOM_FP);
      if (fpObj != null) {
        falsePositiveRate = ConversionUtils.convert(fpObj, Double.class);
      }
      Object eiObj = config.get(PERSISTENT_BLOOM_EI);
      if (eiObj != null) {
        expectedInsertions = ConversionUtils.convert(eiObj, Integer.class);
      }
      Object msObj = config.get(PERSISTENT_BLOOM_MS_BETWEEN_PERSISTS);
      if(msObj != null) {
        millisecondsBetweenPersists = ConversionUtils.convert(msObj, Long.class);
      }
    }

    public String getHBaseTable() {
      return hBaseTable;
    }

    public String getHBaseCF() {
      return hBaseCF;
    }

    public double getFalsePositiveRate() {
      return falsePositiveRate;
    }

    public int getExpectedInsertions() {
      return expectedInsertions;
    }


    public long getMillisecondsBetweenPersists() {
      return millisecondsBetweenPersists;
    }
  }

  @Override
  public AccessTracker create(Map<String, Object> config, TableProvider provider) throws IOException {
    Config patConfig = new Config(config);
    String hbaseTable = patConfig.getHBaseTable();
    int expectedInsertions = patConfig.getExpectedInsertions();
    double falsePositives = patConfig.getFalsePositiveRate();
    long millisecondsBetweenPersist = patConfig.getMillisecondsBetweenPersists();
    BloomAccessTracker bat = new BloomAccessTracker(hbaseTable, expectedInsertions, falsePositives);
    Configuration hbaseConfig = HBaseConfiguration.create();

    AccessTracker ret = new PersistentAccessTracker( hbaseTable
                                                   , UUID.randomUUID().toString()
                                                   , provider.getTable(hbaseConfig, hbaseTable)
                                                   , patConfig.getHBaseCF()
                                                   , bat
                                                   , millisecondsBetweenPersist
                                                   );
    return ret;
  }
}
