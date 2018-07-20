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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class Proto {

  @JacksonXmlProperty(isAttribute = true)
  private String name;
  @JacksonXmlProperty(isAttribute = true)
  private String pos;
  @JacksonXmlProperty(isAttribute = true)
  private String showname;
  @JacksonXmlProperty(isAttribute = true)
  private String size;
  @JacksonXmlProperty(isAttribute = true)
  private String hide;
  @JacksonXmlProperty(localName = "field")
  @JacksonXmlElementWrapper(useWrapping = false)
  private List<Field> fields;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPos() {
    return pos;
  }

  public void setPos(String pos) {
    this.pos = pos;
  }

  public String getShowname() {
    return showname;
  }

  public void setShowname(String showname) {
    this.showname = showname;
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public String getHide() {
    return hide;
  }

  public void setHide(String hide) {
    this.hide = hide;
  }

  public List<Field> getFields() {
    return fields;
  }

  public void setFields(List<Field> fields) {
    this.fields = fields;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Proto proto = (Proto) o;

    return (getName() != null ? getName().equals(proto.getName()) : proto.getName() != null) &&
            (getPos() != null ? getPos().equals(proto.getPos()) : proto.getPos() == null) &&
            (getShowname() != null ? getShowname().equals(proto.getShowname()) : proto.getShowname() == null) &&
            (getSize() != null ? getSize().equals(proto.getSize()) : proto.getSize() == null) &&
            (getHide() != null ? getHide().equals(proto.getHide()) : proto.getHide() == null) &&
            (getFields() != null ? getFields().equals(proto.getFields()) : proto.getFields() == null);
  }

  @Override
  public int hashCode() {
    int result = getName() != null ? getName().hashCode() : 0;
    result = 31 * result + (getPos() != null ? getPos().hashCode() : 0);
    result = 31 * result + (getShowname() != null ? getShowname().hashCode() : 0);
    result = 31 * result + (getSize() != null ? getSize().hashCode() : 0);
    result = 31 * result + (getHide() != null ? getHide().hashCode() : 0);
    result = 31 * result + (getFields() != null ? getFields().hashCode() : 0);
    return result;
  }
}
