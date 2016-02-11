package org.apache.metron.threatintel;

/**
 * Created by cstella on 2/10/16.
 */
public class ThreatIntelConfig {
    private String hBaseTable;
    private double falsePositiveRate;
    private int expectedInsertions;
    private String trackerHBaseTable;
    private String trackerHBaseCF;
    private long millisecondsBetweenPersists;

    public String getHBaseTable() {
        return hBaseTable;
    }

    public int getExpectedInsertions() {
        return expectedInsertions;
    }

    public double getFalsePositiveRate() {
        return falsePositiveRate;
    }

    public String getTrackerHBaseTable() {
        return trackerHBaseTable;
    }

    public void setTrackerHBaseTable(String trackerHBaseTable) {
        this.trackerHBaseTable = trackerHBaseTable;
    }

    public String getTrackerHBaseCF() {
        return trackerHBaseCF;
    }

    public void setTrackerHBaseCF(String trackerHBaseCF) {
        this.trackerHBaseCF = trackerHBaseCF;
    }

    public long getMillisecondsBetweenPersists() {
        return millisecondsBetweenPersists;
    }

    public ThreatIntelConfig withHBaseTable(String hBaseTable) {
        this.hBaseTable = hBaseTable;
        return this;
    }

    public ThreatIntelConfig withFalsePositiveRate(double falsePositiveRate) {
        this.falsePositiveRate = falsePositiveRate;
        return this;
    }

    public ThreatIntelConfig withExpectedInsertions(int expectedInsertions) {
        this.expectedInsertions = expectedInsertions;
        return this;
    }

    public ThreatIntelConfig withMillisecondsBetweenPersists(long millisecondsBetweenPersists) {
        this.millisecondsBetweenPersists = millisecondsBetweenPersists;
        return this;
    }
}
