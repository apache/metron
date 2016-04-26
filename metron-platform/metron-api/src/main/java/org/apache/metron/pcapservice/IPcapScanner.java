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
package org.apache.metron.pcapservice;

import java.io.IOException;

/**
 * The Interface for all pcaps fetching methods based on key range.
 */
public interface IPcapScanner {

  /**
   * Gets the pcaps for between startKey (inclusive) and endKey (exclusive).
   * 
   * @param startKey
   *          the start key of a key range for which pcaps is to be retrieved.
   * @param endKey
   *          the end key of a key range for which pcaps is to be retrieved.
   * @param maxResponseSize
   *          indicates the maximum response size in MegaBytes(MB). User needs
   *          to pass positive value and must be less than 60 (MB)
   * @param startTime
   *          the start time in system milliseconds to be used to filter the
   *          pcaps. The value is set to '0' if the caller sends negative value
   * @param endTime
   *          the end time in system milliseconds to be used to filter the
   *          pcaps. The value is set Long.MAX_VALUE if the caller sends
   *          negative value
   * @return byte array with all matching pcaps merged together
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public byte[] getPcaps(String startKey, String endKey, long maxResponseSize,
      long startTime, long endTime) throws IOException;

  /**
   * Gets the pcaps for between startKey (inclusive) and endKey (exclusive).
   * 
   * @param startKey
   *          the start key (inclusive) of a key range for which pcaps is to be
   *          retrieved.
   * @param endKey
   *          the end key (exclusive) of a key range for which pcaps is to be
   *          retrieved.
   * @return byte array with all matching pcaps merged together
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public byte[] getPcaps(String startKey, String endKey) throws IOException;

}
