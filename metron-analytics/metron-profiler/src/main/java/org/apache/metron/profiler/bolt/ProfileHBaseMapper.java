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

package org.apache.metron.profiler.bolt;

import backtype.storm.tuple.Tuple;
import org.apache.commons.beanutils.BeanMap;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.stellar.StellarExecutor;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hbase.common.ColumnList;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

/**
 * An HbaseMapper that defines how a ProfileMeasurement is persisted within an HBase table.
 */
public class ProfileHBaseMapper implements HBaseMapper {

  /**
   * Executes Stellar code and maintains state across multiple invocations.
   */
  private StellarExecutor executor;

  /**
   * Generates the row keys necessary to store profile data in HBase.
   */
  private RowKeyBuilder rowKeyBuilder;

  /**
   * Generates the ColumnList necesary to store profile data in HBase.
   */
  private ColumnBuilder columnBuilder;

  public ProfileHBaseMapper() {
    setRowKeyBuilder(new SaltyRowKeyBuilder());
    setColumnBuilder(new ValueOnlyColumnBuilder());
  }

  public ProfileHBaseMapper(RowKeyBuilder rowKeyBuilder, ColumnBuilder columnBuilder) {
    setRowKeyBuilder(rowKeyBuilder);
    setColumnBuilder(columnBuilder);
  }

  /**
   * Define the row key for a ProfileMeasurement.
   * @param tuple The tuple containing a ProfileMeasurement.
   * @return The Hbase row key.
   */
  @Override
  public byte[] rowKey(Tuple tuple) {
    ProfileMeasurement m = (ProfileMeasurement) tuple.getValueByField("measurement");
    List<Object> groups = executeGroupBy(m);
    return rowKeyBuilder.rowKey(m, groups);
  }

  /**
   * Defines how the fields within a ProfileMeasurement are mapped to HBase.
   * @param tuple The tuple containing the ProfileMeasurement.
   */
  @Override
  public ColumnList columns(Tuple tuple) {
    ProfileMeasurement measurement = (ProfileMeasurement) tuple.getValueByField("measurement");
    return columnBuilder.columns(measurement);
  }

  /**
   * Executes each of the 'groupBy' expressions.  The result of each
   * expression are the groups used to sort the data as part of the
   * row key.
   * @param m The profile measurement.
   * @return The result of executing the 'groupBy' expressions.
   */
  private List<Object> executeGroupBy(ProfileMeasurement m) {
    List<Object> groups = new ArrayList<>();

    if(!isEmpty(m.getGroupBy())) {
      try {
        // allows each 'groupBy' expression to refer to the fields of the ProfileMeasurement
        BeanMap measureAsMap = new BeanMap(m);

        for (String expr : m.getGroupBy()) {
          Object result = executor.execute(expr, measureAsMap, Object.class);
          groups.add(result);
        }

      } catch(Throwable e) {
        String msg = format("Bad 'groupBy' expression: %s, profile=%s, entity=%s",
                e.getMessage(), m.getProfileName(), m.getEntity());
        throw new ParseException(msg, e);
      }
    }

    return groups;
  }

  public void setExecutor(StellarExecutor executor) {
    this.executor = executor;
  }

  public void setRowKeyBuilder(RowKeyBuilder rowKeyBuilder) {
    this.rowKeyBuilder = rowKeyBuilder;
  }

  public void setColumnBuilder(ColumnBuilder columnBuilder) {
    this.columnBuilder = columnBuilder;
  }
}
