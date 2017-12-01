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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.NullArgumentException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.stellar.common.StellarProcessor;
import org.atteo.classindex.ClassIndex;

public class StellarZookeeperBasedValidator implements StellarValidator {

  private CuratorFramework client;

  public StellarZookeeperBasedValidator(CuratorFramework client) throws NullArgumentException {
    if (client == null) {
      throw new NullArgumentException("client");
    }
    this.client = client;
  }


  @Override
  public void validate(LineWriter writer) {
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
          writer
              .write(ERROR_PROMPT + " Provider: " + c.getCanonicalName() + " not valid, skipping");
        }
      }
    }

    writer.write(String.format("Discovered %d providers", providerSet.size()));

    writer.write("Visiting all configurations.  ThreatTriage rules are checked when loading the "
        + "configuration, thus an invalid ThreatTriage rule will fail the entire Enrichment Configuration.");
    if (providerSet.size() > 0) {
      providerSet.forEach((r) -> writer.write(r.getName() + " "));
      writer.write("");
    } else {
      return;
    }

    providerSet.forEach((r) -> {
      writer.write("Requesting configurations from " + r.getName());
      try {
        List<ExpressionConfigurationHolder> holders = r
            .provideConfigurations(client, (pathName, exception) -> {
              writer.write(ERROR_PROMPT + String
                  .format("Configuration %s is not valid, please review", pathName));
            });

        writer.write(String.format("%s provided %d configurations", r.getName(), holders.size()));
        holders.forEach((h) -> {
          try {
            h.discover();
            h.visit((path, statement) -> {
              writer.write("==================================================");
              writer.write(String.format("validating %s", path));
              try {
                if (StellarProcessor.compile(statement) == null) {
                  writer.write(ERROR_PROMPT + String
                      .format("Statement: %s is not valid, please review", path));
                  writer.write(ERROR_PROMPT + String.format("Statement: %s ", statement));
                }
              } catch (RuntimeException e) {
                writer.write(ERROR_PROMPT + "Error Visiting " + path);
                writer.write(e.getMessage());
                writer.write("--");
                writer.write(ERROR_PROMPT + ": " + statement);
              }
              writer.write("==================================================");
            }, (path, error) -> {
              writer.write(ERROR_PROMPT + String
                  .format("Configuration %s is not valid, please review", path));
            });
          } catch (Exception e) {
            writer.write(ERROR_PROMPT + " " + e.getMessage());
          }
        });
      } catch (Exception e) {
        writer.write(ERROR_PROMPT + "Error Visiting " + r.getName());
        writer.write(e.getMessage());
      }
    });
    writer.write("\nDone validation");
  }
}
