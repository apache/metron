/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.hbase;

import org.apache.hadoop.hbase.util.Bytes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents a list of HBase columns.
 *
 * There are two types of columns, <i>standard</i> and <i>counter</i>.
 *
 * Standard columns have <i>column family</i> (required), <i>qualifier</i> (optional),
 * <i>timestamp</i> (optional), and a <i>value</i> (optional) values.
 *
 * Counter columns have <i>column family</i> (required), <i>qualifier</i> (optional),
 * and an <i>increment</i> (optional, but recommended) values.
 *
 * Inserts/Updates can be added via the <code>addColumn()</code> and <code>addCounter()</code>
 * methods.
 *
 * Original code based on the Apache Storm project. See
 * https://github.com/apache/storm/tree/master/external/storm-hbase.
 */
public class ColumnList {

  public static abstract class AbstractColumn {
    byte[] family, qualifier;

    AbstractColumn(byte[] family, byte[] qualifier){
      this.family = family;
      this.qualifier = qualifier;
    }

    public byte[] getFamily() {
      return family;
    }

    public byte[] getQualifier() {
      return qualifier;
    }
  }

  public static class Column extends AbstractColumn {
    byte[] value;
    long ts = -1;

    Column(byte[] family, byte[] qualifier, long ts, byte[] value){
      super(family, qualifier);
      this.value = value;
      this.ts = ts;
    }

    public byte[] getValue() {
      return value;
    }

    public long getTs() {
      return ts;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Column)) return false;
      Column column = (Column) o;
      return ts == column.ts &&
              Arrays.equals(value, column.value);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(ts);
      result = 31 * result + Arrays.hashCode(value);
      return result;
    }

    @Override
    public String toString() {
      return "Column{" +
              "value=" + Arrays.toString(value) +
              ", ts=" + ts +
              '}';
    }
  }

  public static class Counter extends AbstractColumn {
    long incr = 0;
    Counter(byte[] family, byte[] qualifier, long incr){
      super(family, qualifier);
      this.incr = incr;
    }

    public long getIncrement() {
      return incr;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Counter)) return false;
      Counter counter = (Counter) o;
      return incr == counter.incr;
    }

    @Override
    public int hashCode() {
      return Objects.hash(incr);
    }

    @Override
    public String toString() {
      return "Counter{" +
              "incr=" + incr +
              '}';
    }
  }

  private ArrayList<ColumnList.Column> columns;
  private ArrayList<ColumnList.Counter> counters;

  private ArrayList<Column> columns(){
    if(this.columns == null){
      this.columns = new ArrayList<>();
    }
    return this.columns;
  }

  private ArrayList<Counter> counters(){
    if(this.counters == null){
      this.counters = new ArrayList<>();
    }
    return this.counters;
  }

  /**
   * Add a standard HBase column.
   */
  public ColumnList addColumn(byte[] family, byte[] qualifier, long ts, byte[] value){
    columns().add(new Column(family, qualifier, ts, value));
    return this;
  }

  /**
   * Add a standard HBase column
   */
  public ColumnList addColumn(byte[] family, byte[] qualifier, byte[] value){
    columns().add(new Column(family, qualifier, -1, value));
    return this;
  }

  public ColumnList addColumn(byte[] family, byte[] qualifier){
    addColumn(new Column(family, qualifier, -1, null));
    return this;
  }

  public ColumnList addColumn(String family, String qualifier){
    addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier));
    return this;
  }

  public ColumnList addColumn(String family, String qualifier, byte[] value){
    return addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier), value);
  }

  public ColumnList addColumn(String family, String qualifier, String value){
    return addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier), Bytes.toBytes(value));
  }

  /**
   * Add a standard HBase column given an instance of a class that implements
   * the <code>IColumn</code> interface.
   */
  public ColumnList addColumn(IColumn column){
    return this.addColumn(column.family(), column.qualifier(), column.timestamp(), column.value());
  }

  public ColumnList addColumn(Column column){
    columns().add(column);
    return this;
  }

  /**
   * Add an HBase counter column.
   */
  public ColumnList addCounter(byte[] family, byte[] qualifier, long incr){
    counters().add(new Counter(family, qualifier, incr));
    return this;
  }

  /**
   * Add an HBase counter column given an instance of a class that implements the
   * <code>ICounter</code> interface.
   */
  public ColumnList addCounter(ICounter counter){
    return this.addCounter(counter.family(), counter.qualifier(), counter.increment());
  }


  /**
   * Query to determine if we have column definitions.
   */
  public boolean hasColumns(){
    return columns != null && columns.size() > 0;
  }

  /**
   * Query to determine if we have counter definitions.
   */
  public boolean hasCounters(){
    return this.counters != null && counters.size() > 0;
  }

  /**
   * Get the list of column definitions.
   */
  public List<Column> getColumns(){
    return this.columns;
  }

  /**
   * Get the list of counter definitions.
   */
  public List<Counter> getCounters(){
    return this.counters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ColumnList)) return false;
    ColumnList that = (ColumnList) o;
    return Objects.equals(columns, that.columns) &&
            Objects.equals(counters, that.counters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(columns, counters);
  }

  @Override
  public String toString() {
    return "ColumnList{" +
            "columns=" + columns +
            ", counters=" + counters +
            '}';
  }
}