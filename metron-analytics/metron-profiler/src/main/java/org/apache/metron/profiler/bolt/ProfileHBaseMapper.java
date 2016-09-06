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
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.stellar.StellarExecutor;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hbase.common.ColumnList;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.lang.String.format;

/**
 * An HbaseMapper that defines how a ProfileMeasurement is persisted within an HBase table.
 */
public class ProfileHBaseMapper implements HBaseMapper {

  /**
   * Executes Stellar code and maintains state across multiple invocations.
   */
  private StellarExecutor executor;

  /**
   * A salt can be prepended to the row key to help prevent hot-spotting.  The salt
   * divisor is used to generate the salt.
   *
   * If the salt divisor is 0, a salt will not be used.  By default, the salt is set
   * to 0 and is not used in the row key.
   *
   * If the salt divisor is not 0, the salt will be prepended to the row key to help
   * prevent hot-spotting.  When used this constant should be roughly equal to the
   * number of nodes in the Hbase cluster.
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

    // execute the 'groupBy' expressions to determine the 'groups' used in the row key
    String groups = executeGroupBy(m);

    // row key = profile + entity + [group1, ...] + timestamp
    int length = m.getProfileName().length() + m.getEntity().length() + groups.length() + Long.BYTES;

    ByteBuffer buffer;
    if(saltDivisor > 0) {
      // the row key needs to be prepended with a salt
      byte[] salt = getSalt(m.getStart(), saltDivisor);
      buffer = ByteBuffer
              .allocate(length + salt.length)
              .put(salt);

    } else {
      // no salt is needed
      buffer = ByteBuffer
              .allocate(length);
    }

    // append the remainder of the fields
    buffer.put(m.getProfileName().getBytes())
            .put(m.getEntity().getBytes())
            .put(groups.getBytes())
            .putLong(m.getStart());

    buffer.flip();
    return buffer.array();
  }

  /**
   * Executes each of the 'groupBy' expressions.  The results of each
   * are then appended to one another and returned as a String.
   * @param m
   * @return
   */
  private String executeGroupBy(ProfileMeasurement m) {

    if(m.getGroupBy() == null || m.getGroupBy().size() == 0) {
      // no groupBy expressions define
      return "";
    }

    // allows each 'groupBy' expression to refer to the fields of the ProfileMeasurement
    BeanMap measureAsMap = new BeanMap(m);
    StringBuilder builder = new StringBuilder();

    try {
      // execute each of the 'groupBy' - build a String out of the results
      for (String expr : m.getGroupBy()) {
        Object result = executor.execute(expr, measureAsMap, Object.class);
        builder.append(result);
      }

    } catch(Throwable e) {
      String msg = format("Bad 'groupBy' expression: %s, profile=%s, entity=%s, start=%d",
              e.getMessage(), m.getProfileName(), m.getEntity(), m.getStart());
      throw new ParseException(msg, e);
    }

    return builder.toString();
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
  public static byte[] getSalt(long epoch, int saltDivisor) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      byte[] hash = digest.digest(Bytes.toBytes(epoch));
      int salt = Bytes.toInt(hash) % saltDivisor;
      return Bytes.toBytes(salt);

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

  public StellarExecutor getExecutor() {
    return executor;
  }

  public void setExecutor(StellarExecutor executor) {
    this.executor = executor;
  }

  public static final byte[] QPROFILE = Bytes.toBytes("profile");
  public static final byte[] QENTITY = Bytes.toBytes("entity");
  public static final byte[] QSTART = Bytes.toBytes("start");
  public static final byte[] QEND = Bytes.toBytes("end");
  public static final byte[] QVALUE = Bytes.toBytes("value");
}
