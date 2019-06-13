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
import { QueryBuilder } from "./query-builder";

describe('query-builder', () => {

  it('should be able to handle multiple filters', () => {
    const queryBuilder = new QueryBuilder();

    queryBuilder.setSearch('alert_status:RESOLVE AND ip_src_addr:0.0.0.0');

    expect(queryBuilder.searchRequest.query).toBe(
      '(alert_status:RESOLVE OR metron_alert.alert_status:RESOLVE) AND (ip_src_addr:0.0.0.0 OR metron_alert.ip_src_addr:0.0.0.0)'
      );
  });

  it('should be able to handle multiple EXCLUDING filters for the same field', () => {
    const queryBuilder = new QueryBuilder();

    queryBuilder.setSearch('-alert_status:RESOLVE AND -alert_status:DISMISS');

    expect(queryBuilder.searchRequest.query).toBe(
      '(-alert_status:RESOLVE OR -metron_alert.alert_status:RESOLVE) AND (-alert_status:DISMISS OR -metron_alert.alert_status:DISMISS)'
      );
  });

  it('should be able to handle group multiple clauses to a single field, aka. field grouping', () => {
    const queryBuilder = new QueryBuilder();

    queryBuilder.setSearch('alert_status:(RESOLVE OR DISMISS)');

    expect(queryBuilder.searchRequest.query).toBe(
      '(alert_status:\\(RESOLVE\\ OR\\ DISMISS\\) OR metron_alert.alert_status:\\(RESOLVE\\ OR\\ DISMISS\\))'
      );
  });

});
