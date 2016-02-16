package org.apache.metron.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterface;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by cstella on 2/11/16.
 */
public interface TableProvider extends Serializable {
    HTableInterface getTable(Configuration config, String tableName) throws IOException;
}
