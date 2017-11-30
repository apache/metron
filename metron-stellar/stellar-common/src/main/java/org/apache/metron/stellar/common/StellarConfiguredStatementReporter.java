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

package org.apache.metron.stellar.common;

import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.atteo.classindex.IndexSubclasses;

/**
 * StellarConfiguredStatementProviders are used provide stellar statements
 * and the context around those statements to the caller
 */
@IndexSubclasses
public interface StellarConfiguredStatementReporter {

  /**
   * The Name of this reporter
   * @return String
   */
  String getName();

  public interface StatementReportVisitor{
    void visit(List<String> contextNames, String statement);
  }

  public interface ConfigReportErrorConsumer {
    void consume(List<String> contextNames, Exception e);
  }

  void vist(CuratorFramework client, StatementReportVisitor visitor, ConfigReportErrorConsumer errorConsumer) throws Exception;

}
