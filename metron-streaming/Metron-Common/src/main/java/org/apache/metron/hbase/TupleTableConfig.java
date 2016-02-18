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
package org.apache.metron.hbase;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.base.Joiner;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import backtype.storm.tuple.Tuple;
import org.apache.log4j.Logger;

/**
 * Configuration for Storm {@link Tuple} to HBase serialization.
 */
@SuppressWarnings("serial")
public class TupleTableConfig extends TableConfig implements Serializable {
  private static final Logger LOG = Logger.getLogger(TupleTableConfig.class);
  static final long serialVersionUID = -1L;
  public static final long DEFAULT_INCREMENT = 1L;
  
  protected String tupleRowKeyField;
  protected String tupleTimestampField;
  protected Durability durability = Durability.USE_DEFAULT;
  private String fields;

  /**
   * Initialize configuration
   * 
   * @param table
   *          The HBase table name
   * @param rowKeyField
   *          The {@link Tuple} field used to set the rowKey
   */
  public TupleTableConfig(final String table, final String rowKeyField) {
    super(table);
    this.tupleRowKeyField = rowKeyField;
    this.tupleTimestampField = "";
    this.columnFamilies = new HashMap<String, Set<String>>();
  }
  
  /**
   * Initialize configuration
   * 
   * @param table
   *          The HBase table name
   * @param rowKeyField
   *          The {@link Tuple} field used to set the rowKey
   * @param timestampField
   *          The {@link Tuple} field used to set the timestamp
   */
  public TupleTableConfig(final String table, final String rowKeyField, final String timestampField) {
    super(table);
    this.tupleRowKeyField = rowKeyField;
    this.tupleTimestampField = timestampField;
    this.columnFamilies = new HashMap<String, Set<String>>();
  }

  public TupleTableConfig() {
    super(null);
    this.columnFamilies = new HashMap<String, Set<String>>();
  }



  public TupleTableConfig withRowKeyField(String rowKeyField) {
    this.tupleRowKeyField = rowKeyField;
    return this;
  }

  public TupleTableConfig withTimestampField(String timestampField) {
    this.tupleTimestampField = timestampField;
    return this;
  }

  public TupleTableConfig withFields(String fields) {
    this.fields = fields;
    return this;
  }



  public String getFields() {
    return fields;
  }



  /**
   * Add column family and column qualifier to be extracted from tuple
   * 
   * @param columnFamily
   *          The column family name
   * @param columnQualifier
   *          The column qualifier name
   */
  public void addColumn(final String columnFamily, final String columnQualifier) {
    Set<String> columns = this.columnFamilies.get(columnFamily);
    
    if (columns == null) {
      columns = new HashSet<String>();
    }
    columns.add(columnQualifier);
    
    this.columnFamilies.put(columnFamily, columns);
  }
  
  /**
   * Creates a HBase {@link Put} from a Storm {@link Tuple}
   * 
   * @param tuple
   *          The {@link Tuple}
   * @return {@link Put}
   */
  public Put getPutFromTuple(final Tuple tuple) throws IOException{
    byte[] rowKey = null;
    try {
      rowKey = Bytes.toBytes(tuple.getStringByField(tupleRowKeyField));
    }
    catch(IllegalArgumentException iae) {
      throw new IOException("Unable to retrieve " + tupleRowKeyField + " from " + tuple + " [ " + Joiner.on(',').join(tuple.getFields()) + " ]", iae);
    }
    
    long ts = 0;
    if (!tupleTimestampField.equals("")) {
      ts = tuple.getLongByField(tupleTimestampField);
    }
    
    Put p = new Put(rowKey);
    
    p.setDurability(durability);
    
    if (columnFamilies.size() > 0) {
      for (String cf : columnFamilies.keySet()) {
        byte[] cfBytes = Bytes.toBytes(cf);
        for (String cq : columnFamilies.get(cf)) {
          byte[] cqBytes = Bytes.toBytes(cq);
          byte[] val = tuple.getBinaryByField(cq);
          
          if (ts > 0) {
            p.add(cfBytes, cqBytes, ts, val);
          } else {
            p.add(cfBytes, cqBytes, val);
          }
        }
      }
    }
    
    return p;
  }
  
  /**
   * Creates a HBase {@link Increment} from a Storm {@link Tuple}
   * 
   * @param tuple
   *          The {@link Tuple}
   * @param increment
   *          The amount to increment the counter by
   * @return {@link Increment}
   */
  public Increment getIncrementFromTuple(final Tuple tuple, final long increment) {
    byte[] rowKey = Bytes.toBytes(tuple.getStringByField(tupleRowKeyField));
    
    Increment inc = new Increment(rowKey);
    inc.setDurability(durability);
    
    if (columnFamilies.size() > 0) {
      for (String cf : columnFamilies.keySet()) {
        byte[] cfBytes = Bytes.toBytes(cf);
        for (String cq : columnFamilies.get(cf)) {
          byte[] val;
          try {
            val = Bytes.toBytes(tuple.getStringByField(cq));
          } catch (IllegalArgumentException ex) {
            // if cq isn't a tuple field, use cq for counter instead of tuple
            // value
            val = Bytes.toBytes(cq);
          }
          inc.addColumn(cfBytes, val, increment);
        }
      }
    }
    
    return inc;
  }
  
  /**
   * Increment the counter for the given family and column by the specified
   * amount
   * <p>
   * If the family and column already exist in the Increment the counter value
   * is incremented by the specified amount rather than overridden, as it is in
   * HBase's {@link Increment#addColumn(byte[], byte[], long)} method
   * 
   * @param inc
   *          The {@link Increment} to update
   * @param family
   *          The column family
   * @param qualifier
   *          The column qualifier
   * @param amount
   *          The amount to increment the counter by
   */
  public static void addIncrement(Increment inc, final byte[] family, final byte[] qualifier, final Long amount) {
    
    NavigableMap<byte[], Long> set = inc.getFamilyMapOfLongs().get(family);
    if (set == null) {
      set = new TreeMap<byte[], Long>(Bytes.BYTES_COMPARATOR);
    }
    
    // If qualifier exists, increment amount
    Long counter = set.get(qualifier);
    if (counter == null) {
      counter = 0L;
    }
    set.put(qualifier, amount + counter);
    
    inc.getFamilyMapOfLongs().put(family, set);
  }
  


  /**
   * @param durability
   *          Sets whether to write to HBase's edit log.
   *          <p>
   *          Setting to false will mean fewer operations to perform when
   *          writing to HBase and hence better performance, but changes that
   *          haven't been flushed to a store file will be lost in the event of
   *          HBase failure
   *          <p>
   *          Enabled by default
   */
  public void setDurability(Durability durability) {
    this.durability = durability;
  }
  
  
  public Durability getDurability() {
    return  durability;
  }
  


  /**
   * @return the tupleRowKeyField
   */
  public String getTupleRowKeyField() {
    return tupleRowKeyField;
  }
}
