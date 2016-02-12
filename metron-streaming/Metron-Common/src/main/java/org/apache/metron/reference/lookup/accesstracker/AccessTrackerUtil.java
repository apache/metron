package org.apache.metron.reference.lookup.accesstracker;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import javax.annotation.Nullable;
import java.io.*;

/**
 * Created by cstella on 2/5/16.
 */
public enum AccessTrackerUtil {
    INSTANCE;

    public static byte[] COLUMN = Bytes.toBytes("v");

    public AccessTracker deserializeTracker(byte[] bytes) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        return (AccessTracker) ois.readObject();
    }
    public byte[] serializeTracker(AccessTracker tracker) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(tracker);
        oos.flush();
        oos.close();
        return bos.toByteArray();
    }


    public void persistTracker(HTableInterface accessTrackerTable, String columnFamily, PersistentAccessTracker.AccessTrackerKey key, AccessTracker underlyingTracker) throws IOException {
        Put put = new Put(key.toRowKey());
        put.add(Bytes.toBytes(columnFamily), COLUMN, serializeTracker(underlyingTracker));
        accessTrackerTable.put(put);
    }

    public Iterable<AccessTracker> loadAll(HTableInterface accessTrackerTable, final String columnFamily, final String name, final long earliest) throws IOException {
        Scan scan = new Scan(PersistentAccessTracker.AccessTrackerKey.getTimestampScanKey(name, earliest));
        ResultScanner scanner = accessTrackerTable.getScanner(scan);
        return Iterables.transform(scanner, new Function<Result, AccessTracker>() {

            @Nullable
            @Override
            public AccessTracker apply(@Nullable Result result) {
                try {
                    return deserializeTracker(result.getValue(Bytes.toBytes(columnFamily), COLUMN));
                } catch (Exception e) {
                    throw new RuntimeException("Unable to deserialize " + name + " @ " + earliest);
                }
            }
        });
    }


    public AccessTracker loadAll(Iterable<AccessTracker> trackers) throws IOException, ClassNotFoundException {
        AccessTracker tracker = null;
        for(AccessTracker t : trackers) {
            if(tracker == null) {
                tracker = t;
            }
            else {
                tracker = tracker.union(t);
            }
        }
        return tracker;
    }
}
