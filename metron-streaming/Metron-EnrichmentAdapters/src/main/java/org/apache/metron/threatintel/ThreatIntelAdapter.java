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
package org.apache.metron.threatintel;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.hbase.converters.threatintel.ThreatIntelKey;
import org.apache.metron.reference.lookup.accesstracker.BloomAccessTracker;
import org.apache.metron.reference.lookup.accesstracker.PersistentAccessTracker;
import org.apache.metron.threatintel.hbase.ThreatIntelLookup;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

public class ThreatIntelAdapter implements EnrichmentAdapter<String>,Serializable {
    protected static final Logger _LOG = LoggerFactory.getLogger(ThreatIntelAdapter.class);
    protected ThreatIntelConfig config;
    protected ThreatIntelLookup lookup;

    public ThreatIntelAdapter() {
    }
    public ThreatIntelAdapter(ThreatIntelConfig config) {
        withConfig(config);
    }

    public ThreatIntelAdapter withConfig(ThreatIntelConfig config) {
        this.config = config;
        return this;
    }

    @Override
    public void logAccess(String value) {
        lookup.getAccessTracker().logAccess(new ThreatIntelKey(value));
    }

    @Override
    public JSONObject enrich(String value) {
        JSONObject enriched = new JSONObject();
        boolean isThreat = false;
        try {
            isThreat = lookup.exists(new ThreatIntelKey(value), lookup.getTable(), false);
        } catch (IOException e) {
            throw new RuntimeException("Unable to retrieve value", e);
        }
        if(isThreat) {
            enriched.put("threat_source", config.getHBaseTable());
            _LOG.trace("Enriched value => " + enriched);
        }
        //throw new RuntimeException("Unable to retrieve value " + value);
        return enriched;
    }

    @Override
    public boolean initializeAdapter() {
        PersistentAccessTracker accessTracker;
        String hbaseTable = config.getHBaseTable();
        int expectedInsertions = config.getExpectedInsertions();
        double falsePositives = config.getFalsePositiveRate();
        String trackerHBaseTable = config.getTrackerHBaseTable();
        String trackerHBaseCF = config.getTrackerHBaseCF();
        long millisecondsBetweenPersist = config.getMillisecondsBetweenPersists();
        BloomAccessTracker bat = new BloomAccessTracker(hbaseTable, expectedInsertions, falsePositives);
        Configuration hbaseConfig = HBaseConfiguration.create();
        try {
            accessTracker = new PersistentAccessTracker( hbaseTable
                                                        , UUID.randomUUID().toString()
                                                        , config.getProvider().getTable(hbaseConfig, trackerHBaseTable)
                                                        , trackerHBaseCF
                                                        , bat
                                                        , millisecondsBetweenPersist
                                                        );
            lookup = new ThreatIntelLookup(config.getProvider().getTable(hbaseConfig, hbaseTable), config.getHBaseCF(), accessTracker);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize ThreatIntelAdapter", e);
        }

        return true;
    }

    @Override
    public void cleanup() {
        try {
            lookup.close();
        } catch (Exception e) {
            throw new RuntimeException("Unable to cleanup access tracker", e);
        }
    }
}
