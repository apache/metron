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
package org.apache.metron.parsers.logstash;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class BasicLogstashParserTest {

  private static BasicLogstashParser logstashParser;

  @BeforeClass
  public static void setUpOnce() throws Exception {
      Map<String, Object> parserConfig = new HashMap<>();
      logstashParser = new BasicLogstashParser();
      logstashParser.configure(parserConfig);
      logstashParser.init();
  }

  @Test
  public void testConfigureDefault() {
      Map<String, Object> parserConfig = new HashMap<>();
      BasicLogstashParser testParser = new BasicLogstashParser();
      testParser.configure(parserConfig);
      testParser.init();
  }

}
