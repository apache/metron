package org.apache.metron.spout.pcap;

import java.io.Serializable;

public class HDFSWriterConfig implements Serializable {
    static final long serialVersionUID = 0xDEADBEEFL;
    private long numPackets;
    private long maxTimeMS;
    private String outputPath;

    public HDFSWriterConfig withOutputPath(String path) {
        outputPath = path;
        return this;
    }

    public HDFSWriterConfig withNumPackets(long n) {
        numPackets = n;
        return this;
    }

    public HDFSWriterConfig withMaxTimeMS(long t) {
        maxTimeMS = t;
        return this;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public long getNumPackets() {
        return numPackets;
    }

    public long getMaxTimeMS() {
        return maxTimeMS;
    }

    @Override
    public String toString() {
        return "HDFSWriterConfig{" +
                "numPackets=" + numPackets +
                ", maxTimeMS=" + maxTimeMS +
                ", outputPath='" + outputPath + '\'' +
                '}';
    }
}
