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

package org.apache.metron.profiler.hbase;

import org.apache.metron.hbase.ColumnList;
import org.apache.metron.profiler.ProfileMeasurement;

import java.io.Serializable;

/**
 * Defines how fields in a ProfileMeasurement will be mapped to columns in HBase.
 */
public interface ColumnBuilder extends Serializable {

  /**
   * Generate the columns used to store a ProfileMeasurement.
   * @param measurement The profile measurement.
   */
  ColumnList columns(ProfileMeasurement measurement);

  /**
   * Returns the column family used to store the ProfileMeasurement values.
   * @return String returns the column family used to store the ProfileMeasurement values
   */
  String getColumnFamily();

  /**
   * Returns the column qualifiers for the given field of a ProfileMeasurement.
   * @return The column qualifier used to store a ProfileMeasurement's field in HBase.
   */
  byte[] getColumnQualifier(String fieldName);
}
