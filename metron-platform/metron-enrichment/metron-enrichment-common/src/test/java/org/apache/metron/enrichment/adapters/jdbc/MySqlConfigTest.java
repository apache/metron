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
package org.apache.metron.enrichment.adapters.jdbc;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MySqlConfigTest {

  private String sampleURL = "jdbc:mysql://10.22.0.214:3306/GEO?user=root&password=hadoop123";
  private MySqlConfig conn;

  @Before
  public void setupJdbc() {
    conn = new MySqlConfig();
    conn.setHost("10.22.0.214");
    conn.setPort(3306);
    conn.setTable("GEO");
    conn.setUsername("root");
    conn.setPassword("hadoop123");
  }

  @Test
  public void testGetJdbcUrl() throws Exception {
    Assert.assertEquals(sampleURL, conn.getJdbcUrl());
  }

}
