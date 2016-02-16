package org.apache.metron.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;

import java.io.IOException;

/**
 * Created by cstella on 2/11/16.
 */
public class HTableProvider implements TableProvider {
    @Override
    public HTableInterface getTable(Configuration config, String tableName) throws IOException {
        return new HTable(config, tableName);
    }
}
