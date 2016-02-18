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
package org.apache.metron.reference.lookup.accesstracker;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.log4j.Logger;
import org.apache.metron.reference.lookup.LookupKey;

import java.io.*;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class PersistentAccessTracker implements AccessTracker {
    private static final Logger LOG = Logger.getLogger(PersistentAccessTracker.class);
    private static final long serialVersionUID = 1L;

    public static class AccessTrackerKey {
        String name;
        String containerName;
        long timestamp;
        public AccessTrackerKey(String name, String containerName, long timestamp) {
            this.name = name;
            this.containerName = containerName;
            this.timestamp = timestamp;
        }

        public byte[] toRowKey() {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            try {
                dos.writeUTF(name);
                dos.writeLong(timestamp);
                dos.writeUTF(containerName);
                dos.flush();
            } catch (IOException e) {
                throw new RuntimeException("Unable to write rowkey: " + this, e);
            }

            return os.toByteArray();
        }

        public static byte[] getTimestampScanKey(String name, long timestamp) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            try {
                dos.writeUTF(name);
                dos.writeLong(timestamp);
            } catch (IOException e) {
                throw new RuntimeException("Unable to create scan key " , e);
            }

            return os.toByteArray();
        }

        public static AccessTrackerKey fromRowKey(byte[] rowKey) {
            ByteArrayInputStream is = new ByteArrayInputStream(rowKey);
            DataInputStream dis = new DataInputStream(is);
            try {
                String name = dis.readUTF();
                long timestamp = dis.readLong();
                String containerName = dis.readUTF();
                return new AccessTrackerKey(name, containerName, timestamp);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read rowkey: ", e);
            }
        }
    }

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
    HTableInterface accessTrackerTable;
    String accessTrackerColumnFamily;
    AccessTracker underlyingTracker;
    long timestamp = System.currentTimeMillis();
    String name;
    String containerName;
    private Timer timer;
    long maxMillisecondsBetweenPersists;

    public PersistentAccessTracker( String name
                                  , String containerName
                                  , HTableInterface accessTrackerTable
                                  , String columnFamily
                                  , AccessTracker underlyingTracker
                                  , long maxMillisecondsBetweenPersists
                                  )
    {
        this.containerName = containerName;
        this.accessTrackerTable = accessTrackerTable;
        this.name = name;
        this.accessTrackerColumnFamily = columnFamily;
        this.underlyingTracker = underlyingTracker;
        this.maxMillisecondsBetweenPersists = maxMillisecondsBetweenPersists;
        timer = new Timer();
        if(maxMillisecondsBetweenPersists > 0) {
            timer.scheduleAtFixedRate(new Persister(this), maxMillisecondsBetweenPersists, maxMillisecondsBetweenPersists);
        }
    }

    public void persist(boolean force) {
        synchronized(sync) {
            if(force || (System.currentTimeMillis() - timestamp) >= maxMillisecondsBetweenPersists) {
                //persist
                try {
                    AccessTrackerUtil.INSTANCE.persistTracker(accessTrackerTable, accessTrackerColumnFamily, new AccessTrackerKey(name, containerName, timestamp), underlyingTracker);
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

    @Override
    public void cleanup() throws IOException {
        synchronized(sync) {
            try {
                persist(true);
            }
            catch(Throwable t) {
                LOG.error("Unable to persist underlying tracker", t);
            }
            underlyingTracker.cleanup();
            accessTrackerTable.close();
        }
    }
}
