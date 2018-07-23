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
package org.apache.metron.rest.model.pcap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class Pdml {

  @JacksonXmlProperty(isAttribute = true)
  private String version;
  @JacksonXmlProperty(isAttribute = true)
  private String creator;
  @JacksonXmlProperty(isAttribute = true)
  private String time;
  @JacksonXmlProperty(isAttribute = true, localName = "capture_file")
  private String captureFile;
  @JacksonXmlProperty(localName = "packet")
  @JacksonXmlElementWrapper(useWrapping = false)
  private List<Packet> packets;

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
  }

  public String getCaptureFile() {
    return captureFile;
  }

  public void setCaptureFile(String captureFile) {
    this.captureFile = captureFile;
  }

  public List<Packet> getPackets() {
    return packets;
  }

  public void setPackets(List<Packet> packets) {
    this.packets = packets;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Pdml pdml = (Pdml) o;

    return (getVersion() != null ? getVersion().equals(pdml.getVersion()) : pdml.getVersion() != null) &&
            (getCreator() != null ? getCreator().equals(pdml.getCreator()) : pdml.getCreator() == null) &&
            (getTime() != null ? getTime().equals(pdml.getTime()) : pdml.getTime() == null) &&
            (getCaptureFile() != null ? getCaptureFile().equals(pdml.getCaptureFile()) : pdml.getCaptureFile() == null) &&
            (getPackets() != null ? getPackets().equals(pdml.getPackets()) : pdml.getPackets() == null);
  }

  @Override
  public int hashCode() {
    int result = getVersion() != null ? getVersion().hashCode() : 0;
    result = 31 * result + (getCreator() != null ? getCreator().hashCode() : 0);
    result = 31 * result + (getTime() != null ? getTime().hashCode() : 0);
    result = 31 * result + (getCaptureFile() != null ? getCaptureFile().hashCode() : 0);
    result = 31 * result + (getPackets() != null ? getPackets().hashCode() : 0);
    return result;
  }
}
