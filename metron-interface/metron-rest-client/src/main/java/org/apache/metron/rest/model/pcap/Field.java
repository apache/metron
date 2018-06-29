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

import java.util.ArrayList;
import java.util.List;

public class Field {

  @JacksonXmlProperty(isAttribute = true)
  private String name;
  @JacksonXmlProperty(isAttribute = true)
  private String pos;
  @JacksonXmlProperty(isAttribute = true)
  private String showname;
  @JacksonXmlProperty(isAttribute = true)
  private String size;
  @JacksonXmlProperty(isAttribute = true)
  private String value;
  @JacksonXmlProperty(isAttribute = true)
  private String show;
  @JacksonXmlProperty(isAttribute = true)
  private String unmaskedvalue;
  @JacksonXmlProperty(isAttribute = true)
  private String hide;
  @JacksonXmlProperty(localName = "field")
  @JacksonXmlElementWrapper(useWrapping = false)
  private List<Field> fields;
  @JacksonXmlProperty(localName = "proto")
  @JacksonXmlElementWrapper(useWrapping = false)
  private List<Proto> protos;

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

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getShow() {
    return show;
  }

  public void setShow(String show) {
    this.show = show;
  }

  public String getUnmaskedvalue() {
    return unmaskedvalue;
  }

  public void setUnmaskedvalue(String unmaskedvalue) {
    this.unmaskedvalue = unmaskedvalue;
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

  public List<Proto> getProtos() {
    return protos;
  }

  public void setProtos(List<Proto> protos) {
    this.protos = protos;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Field field = (Field) o;

    return (getName() != null ? getName().equals(field.getName()) : field.getName() != null) &&
            (getPos() != null ? getPos().equals(field.getPos()) : field.getPos() == null) &&
            (getShowname() != null ? getShowname().equals(field.getShowname()) : field.getShowname() == null) &&
            (getSize() != null ? getSize().equals(field.getSize()) : field.getSize() == null) &&
            (getValue() != null ? getValue().equals(field.getValue()) : field.getValue() == null) &&
            (getShow() != null ? getShow().equals(field.getShow()) : field.getShow() == null) &&
            (getUnmaskedvalue() != null ? getUnmaskedvalue().equals(field.getUnmaskedvalue()) : field.getUnmaskedvalue() == null) &&
            (getHide() != null ? getHide().equals(field.getHide()) : field.getHide() == null) &&
            (getFields() != null ? getFields().equals(field.getFields()) : field.getFields() == null) &&
            (getProtos() != null ? getProtos().equals(field.getProtos()) : field.getProtos() == null);
  }

  @Override
  public int hashCode() {
    int result = getName() != null ? getName().hashCode() : 0;
    result = 31 * result + (getPos() != null ? getPos().hashCode() : 0);
    result = 31 * result + (getShowname() != null ? getShowname().hashCode() : 0);
    result = 31 * result + (getSize() != null ? getSize().hashCode() : 0);
    result = 31 * result + (getValue() != null ? getValue().hashCode() : 0);
    result = 31 * result + (getShow() != null ? getShow().hashCode() : 0);
    result = 31 * result + (getUnmaskedvalue() != null ? getUnmaskedvalue().hashCode() : 0);
    result = 31 * result + (getHide() != null ? getHide().hashCode() : 0);
    result = 31 * result + (getFields() != null ? getFields().hashCode() : 0);
    result = 31 * result + (getProtos() != null ? getProtos().hashCode() : 0);
    return result;
  }
}
