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
package org.apache.metron.dataloads.hbase.mr;

import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.metron.enrichment.lookup.LookupKey;
import org.apache.metron.enrichment.lookup.accesstracker.AccessTracker;
import org.apache.metron.enrichment.lookup.accesstracker.AccessTrackerUtil;

import java.io.IOException;

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
        } catch (Throwable e) {
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

            @Override
            public void fromBytes(byte[] in) {

            }
        };
    }

}
