package org.apache.metron.dataloads.taxii;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.hadoop.hbase.client.HTableInterface;

public class TableInfo {
    private String tableName;
    private String columnFamily;
    public TableInfo(String s) {
        Iterable<String> i = Splitter.on(":").split(s);
        if(Iterables.size(i) != 2) {
            throw new IllegalStateException("Malformed table:cf => " + s);
        }
        tableName = Iterables.getFirst(i, null);
        columnFamily = Iterables.getLast(i);
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnFamily() {
        return columnFamily;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TableInfo tableInfo = (TableInfo) o;

        if (getTableName() != null ? !getTableName().equals(tableInfo.getTableName()) : tableInfo.getTableName() != null)
            return false;
        return getColumnFamily() != null ? getColumnFamily().equals(tableInfo.getColumnFamily()) : tableInfo.getColumnFamily() == null;

    }

    @Override
    public int hashCode() {
        int result = getTableName() != null ? getTableName().hashCode() : 0;
        result = 31 * result + (getColumnFamily() != null ? getColumnFamily().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TableInfo{" +
                "tableName='" + tableName + '\'' +
                ", columnFamily='" + columnFamily + '\'' +
                '}';
    }
}
