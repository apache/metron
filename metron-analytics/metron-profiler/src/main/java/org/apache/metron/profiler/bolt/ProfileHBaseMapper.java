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
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hbase.common.ColumnList;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

/**
 * An HbaseMapper that defines how a ProfileMeasurement is persisted within an HBase table.
 */
public class ProfileHBaseMapper implements HBaseMapper {

  /**
   * A salt is prepended to the row key to help prevent hotspotting.  This constant is used
   * to generate the salt.  Ideally, this constant should be roughly equal to the number of
   * nodes in the Hbase cluster.
   */
  private int saltDivisor;

  /**
   * The name of the column family.
   */
  private String columnFamily;

  public ProfileHBaseMapper() {
    setColumnFamily("P");
    setSaltDivisor(1000);
  }

  /**
   * Define the row key for a ProfileMeasurement.
   * @param tuple The tuple containing a ProfileMeasurement.
   * @return The Hbase row key.
   */
  @Override
  public byte[] rowKey(Tuple tuple) {
    ProfileMeasurement m = (ProfileMeasurement) tuple.getValueByField("measurement");

    // create a calendar to determine day-of-week, etc
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(m.getStart());

    return Bytes.toBytes(getSalt(m.getStart()) +
                    m.getProfileName() +
                    calendar.get(Calendar.DAY_OF_WEEK) +
                    calendar.get(Calendar.WEEK_OF_MONTH) +
                    calendar.get(Calendar.MONTH) +
                    calendar.get(Calendar.YEAR) +
                    m.getEntity() +
                    m.getStart());
  }

  /**
   * Defines how the fields within a ProfileMeasurement are mapped to HBase.
   * @param tuple The tuple containing the ProfileMeasurement.
   */
  @Override
  public ColumnList columns(Tuple tuple) {
    ProfileMeasurement measurement = (ProfileMeasurement) tuple.getValueByField("measurement");

    byte[] cfBytes = Bytes.toBytes(columnFamily);
    ColumnList cols = new ColumnList();
    cols.addColumn(cfBytes, QPROFILE, Bytes.toBytes(measurement.getProfileName()));
    cols.addColumn(cfBytes, QENTITY, Bytes.toBytes(measurement.getEntity()));
    cols.addColumn(cfBytes, QSTART, Bytes.toBytes(measurement.getStart()));
    cols.addColumn(cfBytes, QEND, Bytes.toBytes(measurement.getEnd()));
    cols.addColumn(cfBytes, QVALUE, toBytes(measurement.getValue()));

    return cols;
  }

  /**
   * Serialize a profile measurement's value.
   *
   * The value produced by a Profile definition can be any numeric data type.  The data
   * type depends on how the profile is defined by the user.  The user should be able to
   * choose the data type that is most suitable for their use case.
   *
   * @param value The value to serialize.
   */
  private byte[] toBytes(Object value) {
    byte[] result;

    if(value instanceof Integer) {
      result = Bytes.toBytes((Integer) value);
    } else if(value instanceof Double) {
      result = Bytes.toBytes((Double) value);
    } else if(value instanceof Short) {
      result = Bytes.toBytes((Short) value);
    } else if(value instanceof Long) {
      result = Bytes.toBytes((Long) value);
    } else if(value instanceof Float) {
      result = Bytes.toBytes((Float) value);
    } else {
      throw new RuntimeException("Expected 'Number': actual=" + value);
    }

    return result;
  }

  /**
   * Calculates a salt value that is used as part of the row key.
   *
   * The salt is calculated as 'md5(timestamp) % N' where N is a configurable value that ideally
   * is close to the number of nodes in the Hbase cluster.
   *
   * @param epoch The timestamp in epoch millis to use in generating the salt.
   */
  private int getSalt(long epoch) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      byte[] hash = digest.digest(Bytes.toBytes(epoch));
      return Bytes.toInt(hash) % saltDivisor;

    } catch(NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public String getColumnFamily() {
    return columnFamily;
  }

  public void setColumnFamily(String columnFamily) {
    this.columnFamily = columnFamily;
  }

  public int getSaltDivisor() {
    return saltDivisor;
  }

  public void setSaltDivisor(int saltDivisor) {
    this.saltDivisor = saltDivisor;
  }

  public static final byte[] QPROFILE = Bytes.toBytes("profile");
  public static final byte[] QENTITY = Bytes.toBytes("entity");
  public static final byte[] QSTART = Bytes.toBytes("start");
  public static final byte[] QEND = Bytes.toBytes("end");
  public static final byte[] QVALUE = Bytes.toBytes("value");
}
