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

import org.apache.hadoop.hbase.Cell;

import java.util.Arrays;

public class HBaseUtils {

  /**
   * Retrieves the column qualifier from a cell.
   * @param cell An HBase cell.
   * @return The qualifier contained within the cell.
   */
  public static byte[] getQualifier(Cell cell) {
    int length = cell.getQualifierLength();
    int offset = cell.getQualifierOffset();
    byte[] bytes = Arrays.copyOfRange(cell.getRowArray(), offset, offset + length);
    return bytes;
  }

  /**
   * Retrieves the value from an HBase cell.
   * @param cell An HBase cell.
   * @return The value contained within the cell.
   */
  public static byte[] getValue(Cell cell) {
    int length = cell.getValueLength();
    int offset = cell.getValueOffset();
    byte[] bytes = Arrays.copyOfRange(cell.getRowArray(), offset, offset + length);
    return bytes;
  }
}
