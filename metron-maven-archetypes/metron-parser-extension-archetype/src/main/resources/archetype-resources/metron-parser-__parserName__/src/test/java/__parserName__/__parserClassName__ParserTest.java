#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
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
package ${package}.${parserName};

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ${parserClassName}ParserTest {

  private static ${parserClassName}Parser ${parserName}Parser;

  @BeforeClass
  public static void setUpOnce() throws Exception {
      Map<String, Object> parserConfig = new HashMap<>();
      ${parserName}Parser = new ${parserClassName}Parser();
      ${parserName}Parser.configure(parserConfig);
      ${parserName}Parser.init();
  }

  @Test
  public void testConfigureDefault() {
      Map<String, Object> parserConfig = new HashMap<>();
      ${parserClassName}Parser testParser = new ${parserClassName}Parser();
      testParser.configure(parserConfig);
      testParser.init();
  }

}
