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
import java.util.Objects;

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
    return Objects.equals(name, field.name) &&
            Objects.equals(pos, field.pos) &&
            Objects.equals(showname, field.showname) &&
            Objects.equals(size, field.size) &&
            Objects.equals(value, field.value) &&
            Objects.equals(show, field.show) &&
            Objects.equals(unmaskedvalue, field.unmaskedvalue) &&
            Objects.equals(hide, field.hide) &&
            Objects.equals(fields, field.fields) &&
            Objects.equals(protos, field.protos);
  }

  @Override
  public int hashCode() {

    return Objects.hash(name, pos, showname, size, value, show, unmaskedvalue, hide, fields, protos);
  }
}
