/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.stellar.common.utils.validation;

/**
 * StellarConfiguredStatementProviders are used provide stellar statements
 * and the context around those statements to the caller.
 *
 * The usage pattern is:
 *
 * stellarConfiguredStatementContainer.discover();
 * stellarConfiguredStatementContiner.visit((p,s)->{}, (p,e) -> {} );
 *
 */
public interface StellarConfiguredStatementContainer {

  interface StatementVisitor {
    void visit(String path, String statement);
  }

  interface ErrorConsumer {
    void consume(String path, Exception e);
  }

  /**
   * Returns a name for this container.
   * @return String
   */
  String getName();

  /**
   * Returns a full name for this container.
   * This would include pathing information if applicable.
   * @return String
   */
  String getFullName();

  /**
   * Discover is called to allow the Container to discover it's configurations.
   * @throws Exception if there is an error
   */
  void discover() throws Exception;

  /**
   * Visit each configuration within this container.
   * @param visitor StatementVisitor callback
   * @param errorConsumer ErrorConsumer callback
   * @throws Exception if there is an error
   */
  void visit(StatementVisitor visitor, ErrorConsumer errorConsumer) throws Exception;


}
