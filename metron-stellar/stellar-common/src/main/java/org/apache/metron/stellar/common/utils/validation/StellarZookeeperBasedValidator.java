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
import java.util.Set;
import org.apache.commons.lang.NullArgumentException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.stellar.common.StellarConfiguredStatementReporter;
import org.apache.metron.stellar.common.StellarProcessor;
import org.atteo.classindex.ClassIndex;

public class StellarZookeeperBasedValidator implements StellarValidator{

  private CuratorFramework client;

  public StellarZookeeperBasedValidator(CuratorFramework client) throws NullArgumentException {
    if(client == null) {
      throw new NullArgumentException("client");
    }
    this.client = client;
  }


  @Override
  public void validate(LineWriter writer) {
    // discover all the StellarConfiguredStatementReporters
    Set<StellarConfiguredStatementReporter> reporterSet = new HashSet<>();

    for (Class<?> c : ClassIndex.getSubclasses(StellarConfiguredStatementReporter.class,
        Thread.currentThread().getContextClassLoader())) {
      boolean isAssignable = StellarConfiguredStatementReporter.class.isAssignableFrom(c);
      if (isAssignable) {
        try {
          StellarConfiguredStatementReporter reporter = StellarConfiguredStatementReporter.class
              .cast(c.getConstructor().newInstance());
          reporterSet.add(reporter);
        } catch (Exception e) {
          writer.write(ERROR_PROMPT + " Reporter: " + c.getCanonicalName() + " not valid, skipping");
        }
      }
    }

    writer.write(String.format("Discovered %d reporters", reporterSet.size()));

    writer.write("Visiting all configurations.  ThreatTriage rules are checked when loading the "
        + "configuration, thus an invalid ThreatTriage rule will fail the entire Enrichement Configuration.");
    if (reporterSet.size() > 0) {
      reporterSet.forEach((r) ->  writer.write(r.getName() + " "));
      writer.write("");
    } else {
      return;
    }

    reporterSet.forEach((r) -> {
      writer.write("Visiting " + r.getName());
      try {
        r.vist(client, ((contextNames, statement) -> {
          // the names taken together are the identifier for this
          // statement
          String name = String.join("->", contextNames);

          writer.write("==================================================");
          writer.write("validating " + name);
          try {
            if (StellarProcessor.compile(statement) == null) {
              writer.write(
                  ERROR_PROMPT + String.format("Statement: %s is not valid, please review", name));
              writer.write(ERROR_PROMPT + String.format("Statement: %s ", statement));
            }
          } catch (RuntimeException e) {
            writer.write(ERROR_PROMPT + "Error Visiting " + name);
            writer.write(e.getMessage());
            writer.write("--");
            writer.write(ERROR_PROMPT + ": " + statement);
          }
          writer.write("==================================================");
        }), (contextNames, exception) -> {
          String name = String.join("->", contextNames);
          writer.write(
              ERROR_PROMPT + String.format("Configuration %s is not valid, please review", name));
        });
      } catch (Exception e) {
        writer.write(ERROR_PROMPT + "Error Visiting " + r.getName());
        writer.write(e.getMessage());
      }
    });
    writer.write("\nDone validation");
  }
}
