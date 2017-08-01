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
package org.apache.metron.rest.service.impl.blockly;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Block {

  @XmlAttribute
  private String type;

  @XmlElement(name="mutation")
  private Mutation mutation;

  @XmlElement(name="field")
  private List<Field> fields;

  @XmlElement(name="value")
  private List<Value> values;

  @XmlAttribute
  private String x;

  @XmlAttribute
  private String y;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Block withType(String type) {
    this.type = type;
    return this;
  }

  public Mutation getMutation() {
    return this.mutation;
  }

  public void setMutation(Mutation mutation) {
    this.mutation = mutation;
  }

  public Block withMutation(Mutation mutation) {
    this.mutation = mutation;
    return this;
  }

  public List<Field> getFields() {
    return fields;
  }

  public void setFields(List<Field> fields) {
    this.fields = fields;
  }

  public Block addField(Field field) {
    if (fields == null) {
      fields = new ArrayList<>();
    }
    fields.add(field);
    return this;
  }

  public List<Value> getValues() {
    return values;
  }

  public void setValues(List<Value> values) {
    this.values = values;
  }

  public Block addValue(Value value) {
    if (values == null) {
      values = new ArrayList<>();
    }
    values.add(value);
    return this;
  }

  public String getX() {
    return x;
  }

  public void setX(String x) {
    this.x = x;
  }

  public Block withX(String x) {
    this.x = x;
    return this;
  }

  public String getY() {
    return y;
  }

  public void setY(String y) {
    this.y = y;
  }

  public Block withY(String y) {
    this.y = y;
    return this;
  }
}
