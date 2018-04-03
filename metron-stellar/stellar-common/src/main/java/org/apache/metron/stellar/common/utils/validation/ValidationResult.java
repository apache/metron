/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.stellar.common.utils.validation;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Result of a validation of a Stellar Rule
 */
public class ValidationResult {

  private String rulePath;
  private String rule;
  private String error;
  private boolean valid;

  public ValidationResult(String rulePath, String rule, String error, boolean valid) {
    this.rulePath = rulePath;
    this.rule = rule;
    this.error = error;
    this.valid = valid;
  }

  public String getRulePath() {
    return rulePath;
  }

  public String getRule() {
    return rule;
  }

  public String getError() {
    return error;
  }

  public boolean isValid() {
    return valid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ValidationResult that = (ValidationResult) o;

    return new EqualsBuilder().append(isValid(), that.isValid()).append(getRule(), that.getRule())
        .append(getError(), that.getError()).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(getRule()).append(getError()).append(isValid())
        .toHashCode();
  }
}
