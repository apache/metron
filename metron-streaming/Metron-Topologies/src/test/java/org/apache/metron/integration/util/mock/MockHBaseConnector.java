package org.apache.metron.integration.util.mock;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException;
import org.apache.metron.hbase.Connector;
import org.apache.metron.hbase.TupleTableConfig;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by cstella on 1/29/16.
 */
public class MockHBaseConnector extends Connector {
    static List<Put> puts = Collections.synchronizedList(new ArrayList<Put>());
    public MockHBaseConnector(TupleTableConfig conf, String _quorum, String _port) throws IOException {
        super(conf, _quorum, _port);
    }

    @Override
    public void put(Put put) throws InterruptedIOException, RetriesExhaustedWithDetailsException {
        puts.add(put);
    }

    @Override
    public void close() {

    }
    public static void clear() {
        puts.clear();
    }
    public static List<Put> getPuts() {
        return puts;
    }
}
