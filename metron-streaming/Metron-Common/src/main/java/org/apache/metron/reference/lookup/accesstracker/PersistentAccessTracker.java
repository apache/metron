package org.apache.metron.reference.lookup.accesstracker;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;
import org.apache.metron.reference.lookup.LookupKey;

import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by cstella on 2/5/16.
 */
public class PersistentAccessTracker implements AccessTracker {
    private static final Logger LOG = Logger.getLogger(PersistentAccessTracker.class);
    private static final long serialVersionUID = 1L;

    private static class Persister extends TimerTask {
        PersistentAccessTracker tracker;
        public Persister(PersistentAccessTracker tracker) {
            this.tracker = tracker;
        }
        /**
         * The action to be performed by this timer task.
         */
        @Override
        public void run() {
            tracker.persist(false);
        }
    }

    Object sync = new Object();
    FileSystem fs;
    Path basePath;
    AccessTracker underlyingTracker;
    long timestamp = System.currentTimeMillis();
    String name;
    private Timer timer;
    long maxMillisecondsBetweenPersists;

    public PersistentAccessTracker(String name
                                  , FileSystem fs
                                  , Path basePath
                                  , AccessTracker underlyingTracker
                                  , long maxMillisecondsBetweenPersists
                                  )
    {
        this.fs = fs;
        this.name = name;
        this.basePath = basePath;
        this.underlyingTracker = underlyingTracker;
        this.maxMillisecondsBetweenPersists = maxMillisecondsBetweenPersists;
        timer = new Timer();
        timer.scheduleAtFixedRate(new Persister(this), maxMillisecondsBetweenPersists, maxMillisecondsBetweenPersists);
    }

    public void persist(boolean force) {
        synchronized(sync) {
            if(force || (System.currentTimeMillis() - timestamp) >= maxMillisecondsBetweenPersists) {
                //persist
                Path savePath = new Path(basePath, getName() + "_" + timestamp);
                //persist
                try {
                    Util.INSTANCE.persistTracker(fs, savePath, underlyingTracker);
                    timestamp = System.currentTimeMillis();
                    reset();
                } catch (IOException e) {
                    LOG.error("Unable to persist access tracker.", e);
                }
            }
        }
    }

    @Override
    public void logAccess(LookupKey key) {
        synchronized (sync) {
            underlyingTracker.logAccess(key);
            if (isFull()) {
                persist(true);
            }
        }
    }

    @Override
    public void configure(Map<String, Object> config) {
        underlyingTracker.configure(config);
    }

    @Override
    public boolean hasSeen(LookupKey key) {
        synchronized(sync) {
            return underlyingTracker.hasSeen(key);
        }
    }

    @Override
    public String getName() {
        return underlyingTracker.getName();
    }

    @Override
    public AccessTracker union(AccessTracker tracker) {
        PersistentAccessTracker t1 = (PersistentAccessTracker)tracker;
        underlyingTracker = underlyingTracker.union(t1.underlyingTracker);
        return this;
    }

    @Override
    public void reset() {
        synchronized(sync) {
            underlyingTracker.reset();
        }
    }

    @Override
    public boolean isFull() {
        synchronized (sync) {
            return underlyingTracker.isFull();
        }
    }
}
