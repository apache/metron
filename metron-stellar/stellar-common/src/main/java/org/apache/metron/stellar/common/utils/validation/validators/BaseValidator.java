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

package org.apache.metron.stellar.common.utils.validation.validators;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.common.utils.validation.StellarConfiguredStatementContainer;
import org.apache.metron.stellar.common.utils.validation.StellarValidator;
import org.apache.metron.stellar.common.utils.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseValidator implements StellarValidator {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String FAILED_COMPILE = "Failed to compile";

  public BaseValidator() {
  }

  @Override
  public abstract Iterable<ValidationResult> validate();

  protected List<ValidationResult> handleContainers(
      List<StellarConfiguredStatementContainer> containers) {
    ArrayList<ValidationResult> results = new ArrayList<>();
    containers.forEach((container) -> {
      try {
        container.discover();
        container.visit((path, statement) -> {
          try {
            if (StellarProcessor.compile(statement) == null) {
              results.add(new ValidationResult(path, statement, FAILED_COMPILE, false));
            } else {
              results.add(new ValidationResult(path, statement, null, true));
            }
          } catch (RuntimeException e) {
            results.add(new ValidationResult(path, statement, e.getMessage(), false));
          }
        }, (path, error) -> {
          results.add(new ValidationResult(path, null, error.getMessage(), false));
        });
      } catch (Exception e) {
        LOG.error(e.getMessage(), e);
      }
    });
    return results;
  }
}
