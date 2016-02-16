package org.apache.metron.reference.lookup.accesstracker;

import org.apache.metron.reference.lookup.LookupKey;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * Created by cstella on 2/5/16.
 */
public interface AccessTracker extends Serializable{
    void logAccess(LookupKey key);
    void configure(Map<String, Object> config);
    boolean hasSeen(LookupKey key);
    String getName();
    AccessTracker union(AccessTracker tracker);
    void reset();
    boolean isFull();
    void cleanup() throws IOException;
}
