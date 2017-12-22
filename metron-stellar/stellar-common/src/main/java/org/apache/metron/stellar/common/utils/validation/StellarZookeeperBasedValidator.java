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

import static org.apache.metron.stellar.common.shell.StellarShell.ERROR_PROMPT;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang.NullArgumentException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.stellar.common.StellarProcessor;
import org.atteo.classindex.ClassIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StellarZookeeperBasedValidator implements StellarValidator {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String FAILED_COMPILE = "Failed to compile";
  private CuratorFramework client;

  public StellarZookeeperBasedValidator(CuratorFramework client) throws NullArgumentException {
    if (client == null) {
      throw new NullArgumentException("client");
    }
    this.client = client;
  }


  @Override
  public Iterable<ValidationResult> validate(Optional<LineWriter> writer) {
    // discover all the StellarConfigurationProvider
    Set<StellarConfigurationProvider> providerSet = new HashSet<>();

    for (Class<?> c : ClassIndex.getSubclasses(StellarConfigurationProvider.class,
        Thread.currentThread().getContextClassLoader())) {
      boolean isAssignable = StellarConfigurationProvider.class.isAssignableFrom(c);
      if (isAssignable) {
        try {
          StellarConfigurationProvider reporter = StellarConfigurationProvider.class
              .cast(c.getConstructor().newInstance());
          providerSet.add(reporter);
        } catch (Exception e) {
          LOG.error("Provider: " + c.getCanonicalName() + " not valid, skipping", e);
        }
      }
    }

    ArrayList<ValidationResult> results = new ArrayList<>();
    providerSet.forEach((r) -> {
      try {
        List<ExpressionConfigurationHolder> holders = r
            .provideConfigurations(client, (pathName, exception) -> {
              results.add(new ValidationResult(pathName, null, exception.getMessage(), false));
            });

        holders.forEach((h) -> {
          try {
            h.discover();
            h.visit((path, statement) -> {
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
      } catch (Exception e) {
        LOG.error(e.getMessage(), e);
      }
    });
    return results;
  }
}
