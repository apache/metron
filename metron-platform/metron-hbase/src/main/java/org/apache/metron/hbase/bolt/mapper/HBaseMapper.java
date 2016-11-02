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

package org.apache.metron.hbase.bolt.mapper;

import org.apache.storm.tuple.Tuple;
import java.io.Serializable;
import java.util.Optional;

/**
 * Maps a <code>backtype.storm.tuple.Tuple</code> object
 * to a row in an HBase table.
 *
 * Original code based on the Apache Storm project. See
 * https://github.com/apache/storm/tree/master/external/storm-hbase.
 */
public interface HBaseMapper extends Serializable {

  /**
   * Defines the HBase row key that will be used when writing the data from a
   * tuple to HBase.
   *
   * @param tuple The tuple to map to HBase.
   */
  byte[] rowKey(Tuple tuple);

  /**
   * Defines the columnar structure that will be used when writing the data
   * from a tuple to HBase.
   *
   * @param tuple The tuple to map to HBase.
   */
  ColumnList columns(Tuple tuple);

  /**
   *
   * Defines the TTL (time-to-live) that will be used when writing the data
   * from a tuple to HBase.  After the TTL, the data will expire and will be
   * purged.
   *
   * @param tuple The tuple to map to HBase.
   * @return The TTL in milliseconds.
   */
  Optional<Long> getTTL(Tuple tuple);
}
