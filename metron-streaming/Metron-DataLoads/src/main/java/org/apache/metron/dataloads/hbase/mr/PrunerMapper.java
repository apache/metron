package org.apache.metron.dataloads.hbase.mr;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.metron.reference.lookup.LookupKey;
import org.apache.metron.reference.lookup.accesstracker.AccessTracker;
import org.apache.metron.reference.lookup.accesstracker.AccessTrackerUtil;

import java.io.IOException;

/**
 * Created by cstella on 2/5/16.
 */
public class PrunerMapper extends TableMapper<ImmutableBytesWritable, Delete> {
    public static final String ACCESS_TRACKER_TABLE_CONF = "access_tracker_table";
    public static final String ACCESS_TRACKER_CF_CONF = "access_tracker_cf";
    public static final String TIMESTAMP_CONF = "access_tracker_timestamp";
    public static final String ACCESS_TRACKER_NAME_CONF = "access_tracker_name";
    AccessTracker tracker;
    @Override
    public void setup(Context context) throws IOException
    {
        String atTable = context.getConfiguration().get(ACCESS_TRACKER_TABLE_CONF);
        String atCF = context.getConfiguration().get(ACCESS_TRACKER_CF_CONF);
        String atName = context.getConfiguration().get(ACCESS_TRACKER_NAME_CONF);
        HTable table = new HTable(context.getConfiguration(), atTable);
        long timestamp = context.getConfiguration().getLong(TIMESTAMP_CONF, -1);
        if(timestamp < 0) {
            throw new IllegalStateException("Must specify a timestamp that is positive.");
        }
        try {
            tracker = AccessTrackerUtil.INSTANCE.loadAll(AccessTrackerUtil.INSTANCE.loadAll(table, atCF, atName, timestamp));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load the accesstrackers from the directory", e);
        }
    }

    @Override
    public void map(ImmutableBytesWritable key, Result value, Context context) throws IOException, InterruptedException {
        if(tracker == null || key == null) {
            throw new RuntimeException("Tracker = " + tracker + " key = " + key);
        }
        if(!tracker.hasSeen(toLookupKey(key.get()))) {
            Delete d = new Delete(key.get());
            context.write(key, d);
        }
    }

    protected LookupKey toLookupKey(final byte[] bytes) {
        return new LookupKey() {
            @Override
            public byte[] toBytes() {
                return bytes;
            }
        };
    }

}
