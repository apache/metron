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
import java.util.List;

/**
 * interface to all 'keys' based pcaps fetching methods.
 * 
 * @author Sayi
 */
public interface IPcapGetter {

  /**
   * Gets the pcaps for the input list of keys and lastRowKey.
   * 
   * @param keys
   *          the list of keys for which pcaps are to be retrieved
   * @param lastRowKey
   *          last row key from the previous partial response
   * @param startTime
   *          the start time in system milliseconds to be used to filter the
   *          pcaps. The value is set to '0' if the caller sends negative value
   * @param endTime
   *          the end time in system milliseconds to be used to filter the
   *          pcaps. The value is set to Long.MAX_VALUE if the caller sends
   *          negative value. 'endTime' must be greater than the 'startTime'.
   * @param includeReverseTraffic
   *          indicates whether or not to include pcaps from the reverse traffic
   * @param includeDuplicateLastRow
   *          indicates whether or not to include the last row from the previous
   *          partial response
   * @param maxResultSize
   *          the max result size
   * @return PcapsResponse with all matching pcaps merged together
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public PcapsResponse getPcaps(List<String> keys, String lastRowKey,
      long startTime, long endTime, boolean includeReverseTraffic,
      boolean includeDuplicateLastRow, long maxResultSize) throws IOException;

  /**
   * Gets the pcaps for the input key.
   * 
   * @param key
   *          the key for which pcaps is to be retrieved.
   * @param startTime
   *          the start time in system milliseconds to be used to filter the
   *          pcaps. The value is set to '0' if the caller sends negative value
   * @param endTime
   *          the end time in system milliseconds to be used to filter the
   *          pcaps.The value is set to Long.MAX_VALUE if the caller sends
   *          negative value. 'endTime' must be greater than the 'startTime'.
   * @param includeReverseTraffic
   *          indicates whether or not to include pcaps from the reverse traffic
   * @return PcapsResponse with all matching pcaps merged together
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public PcapsResponse getPcaps(String key, long startTime, long endTime,
      boolean includeReverseTraffic) throws IOException;

  /**
   * Gets the pcaps for the input list of keys.
   * 
   * @param keys
   *          the list of keys for which pcaps are to be retrieved.
   * @return PcapsResponse with all matching pcaps merged together
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public PcapsResponse getPcaps(List<String> keys) throws IOException;

  /**
   * Gets the pcaps for the input key.
   * 
   * @param key
   *          the key for which pcaps is to be retrieved.
   * @return PcapsResponse with all matching pcaps merged together
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public PcapsResponse getPcaps(String key) throws IOException;

}
