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
package org.apache.metron.enrichment.adapters.threatintel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThreatIntelConfigTest {

  private String cf = "cf";
  private String table = "threatintel";
  private String trackCf = "cf";
  private String trackTable = "Track";
  private double falsePositive = 0.03;
  private int expectedInsertion = 1;
  private long millionseconds = (long) 0.1;

  @Test
  public void test() {
    ThreatIntelConfig tic = new ThreatIntelConfig();
    tic.withHBaseCF(cf);
    tic.withHBaseTable(table);
    tic.withExpectedInsertions(expectedInsertion);
    tic.withFalsePositiveRate(falsePositive);
    tic.withMillisecondsBetweenPersists(millionseconds);
    tic.withTrackerHBaseCF(trackCf);
    tic.withTrackerHBaseTable(trackTable);

    assertEquals(cf, tic.getHBaseCF());
    assertEquals(table, tic.getHBaseTable());
    assertEquals(trackCf, tic.getTrackerHBaseCF());
    assertEquals(trackTable, tic.getTrackerHBaseTable());
    assertEquals(expectedInsertion, tic.getExpectedInsertions());
    assertEquals(millionseconds, tic.getMillisecondsBetweenPersists());

  }

}
