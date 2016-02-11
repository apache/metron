package org.apache.metron.threatintel;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.reference.lookup.accesstracker.BloomAccessTracker;
import org.apache.metron.reference.lookup.accesstracker.PersistentAccessTracker;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

/**
 * Created by cstella on 2/10/16.
 */
public class ThreatIntelAdapter implements EnrichmentAdapter<String>,Serializable {
    protected static final Logger _LOG = LoggerFactory.getLogger(ThreatIntelAdapter.class);
    protected ThreatIntelConfig config;
    protected PersistentAccessTracker accessTracker;


    public ThreatIntelAdapter withConfig(ThreatIntelConfig config) {
        this.config = config;
        return this;
    }

    @Override
    public void logAccess(String value) {
        accessTracker.logAccess(new ThreatIntelKey(value));
    }

    @Override
    public JSONObject enrich(String value) {
        JSONObject enriched = new JSONObject();
        boolean isThreat = accessTracker.hasSeen(new ThreatIntelKey(value));
        if(isThreat) {
            enriched.put("threat_source", config.getHBaseTable());
        }
        return enriched;
    }

    @Override
    public boolean initializeAdapter() {
        String hbaseTable = config.getHBaseTable();
        int expectedInsertions = config.getExpectedInsertions();
        double falsePositives = config.getFalsePositiveRate();
        String trackerHBaseTable = config.getTrackerHBaseTable();
        String trackerHBaseCF = config.getTrackerHBaseCF();
        long millisecondsBetweenPersist = config.getMillisecondsBetweenPersists();
        BloomAccessTracker bat = new BloomAccessTracker(hbaseTable, expectedInsertions, falsePositives);
        try {
            accessTracker = new PersistentAccessTracker( hbaseTable
                                                        , UUID.randomUUID().toString()
                                                        , new HTable(HBaseConfiguration.create(), trackerHBaseTable)
                                                        , trackerHBaseCF
                                                        , bat
                                                        , millisecondsBetweenPersist
                                                        );
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize ThreatIntelAdapter", e);
        }
        return true;
    }

    @Override
    public void cleanup() {
        try {
            accessTracker.cleanup();
        } catch (IOException e) {
            throw new RuntimeException("Unable to cleanup access tracker", e);
        }
    }
}
