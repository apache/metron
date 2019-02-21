package org.apache.metron.hbase.coprocessor.config;

import org.apache.metron.common.configuration.ConfigOption;

public enum CoprocessorOptions implements ConfigOption {
  TABLE_NAME("metron.coprocessor.tableName"),
  COLUMN_FAMILY("metron.coprocessor.columnFamily"),
  COLUMN_QUALIFIER("metron.coprocessor.columnQualifier");

  private String key;

  CoprocessorOptions(String key) {
    this.key = key;
  }

  @Override
  public String getKey() {
    return key;
  }

}
