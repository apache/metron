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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.metron.pcap.PcapMerger;



/**
 * Holds pcaps data, status and the partial response key.
 * 
 * @author Sayi
 */
public class PcapsResponse {

  /**
   * The Enum Status.
   */
  public enum Status {
    
    /** The partial. */
    PARTIAL, 
 /** The complete. */
 COMPLETE
  };

  /** response of the processed keys. */
  private List<byte[]> pcaps = new ArrayList<byte[]>();;

  /** partial response key. */
  private String lastRowKey;

  /** The status. */
  private Status status = Status.COMPLETE;

  /**
   * Sets the pcaps.
   * 
   * @param pcaps
   *          the new pcaps
   */
  public void setPcaps(List<byte[]> pcaps) {
    this.pcaps = pcaps;
  }

  /**
   * Adds the pcaps.
   * 
   * @param pcaps
   *          the pcaps
   */
  public void addPcaps(byte[] pcaps) {
    this.pcaps.add(pcaps);
  }

  /**
   * Gets the partial response key.
   * 
   * @return the partial response key
   */
  public String getLastRowKey() {
    return lastRowKey;
  }

  /**
   * Sets the partial response key.
   * 
   * @param lastRowKey
   *          the last row key
   */
  public void setLastRowKey(String lastRowKey) {
    this.lastRowKey = lastRowKey;
  }

  /**
   * Gets the status.
   * 
   * @return the status
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Sets the status.
   * 
   * @param status
   *          the new status
   */
  public void setStatus(Status status) {
    this.status = status;
  }

  /**
   * Checks if is resonse size within limit.
   * 
   * @param maxResultSize
   *          the max result size
   * @return true, if is resonse size within limit
   */
  public boolean isResonseSizeWithinLimit(long maxResultSize) {
    // System.out.println("isResonseSizeWithinLimit() : getResponseSize() < (input|default result size - maximum packet size ) ="+
    // getResponseSize()+ " < " + ( maxResultSize
    // -ConfigurationUtil.getMaxRowSize()));
    return getResponseSize() < (maxResultSize - ConfigurationUtil
        .getMaxRowSize());
  }

  /**
   * Gets the response size.
   * 
   * @return the response size
   */
  public long getResponseSize() {
    long responseSize = 0;
    for (byte[] pcap : this.pcaps) {
      responseSize = responseSize + pcap.length;
    }
    return responseSize;
  }

  /**
   * Gets the pcaps.
   * 
   * @return the pcaps
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public byte[] getPcaps() throws IOException {
    if (pcaps.size() == 1) {
      return pcaps.get(0);
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PcapMerger.merge(baos, pcaps);
    return baos.toByteArray();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "PcapsResponse [lastRowKey=" + lastRowKey
        + ", status=" + status + ", pcapsSize="
        + String.valueOf(getResponseSize()) + "]";
  }
}
