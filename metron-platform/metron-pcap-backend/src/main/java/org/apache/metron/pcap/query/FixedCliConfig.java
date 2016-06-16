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
package org.apache.metron.pcap.query;

import org.apache.metron.common.Constants;

import java.util.EnumMap;

public class FixedCliConfig extends CliConfig {

  private EnumMap<Constants.Fields, String> fixedFields;

  public FixedCliConfig() {
    this.fixedFields = new EnumMap<>(Constants.Fields.class);
  }

  public EnumMap<Constants.Fields, String> getFixedFields() {
    return fixedFields;
  }

  public void setFixedFields(EnumMap<Constants.Fields, String> fixedFields) {
    this.fixedFields = fixedFields;
  }

  public void putFixedField(Constants.Fields key, String value) {
    String trimmedVal = value != null ? value.trim() : null;
    if (!isNullOrEmpty(trimmedVal)) {
      this.fixedFields.put(key, value);
    }
  }

}
