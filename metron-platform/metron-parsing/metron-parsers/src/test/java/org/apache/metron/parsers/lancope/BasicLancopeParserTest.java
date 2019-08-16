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
package org.apache.metron.parsers.lancope;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.metron.parsers.AbstractParserConfigTest;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BasicLancopeParserTest extends AbstractParserConfigTest {

  @Before
  public void setUp() throws Exception {
    inputStrings = super.readTestDataFromFile("src/test/resources/logData/LancopeParserTest.txt");
    parser = new BasicLancopeParser();

    URL schema_url = getClass().getClassLoader().getResource(
        "TestSchemas/LancopeSchema.json");
    super.setSchemaJsonString(super.readSchemaFromFile(schema_url));
  }

  @Test
  public void testParse() throws ParseException, IOException, ProcessingException {
    for (String inputString : inputStrings) {
      JSONObject parsed = parser.parse(inputString.getBytes(StandardCharsets.UTF_8)).get(0);
      Assert.assertNotNull(parsed);

      JSONParser parser = new JSONParser();

      Map<?, ?> json = (Map<?, ?>) parser.parse(parsed.toJSONString());
      Assert.assertTrue(validateJsonData(getSchemaJsonString(), json.toString()));
    }
  }

  @Test
  public void getsReadCharsetFromConfig() {
    Map<String, Object> config = new HashMap<>();
    config.put(MessageParser.READ_CHARSET, StandardCharsets.UTF_16.toString());
    parser.configure(config);
    assertThat(parser.getReadCharset(), equalTo(StandardCharsets.UTF_16));
  }

  @Test
  public void getsReadCharsetFromDefault() {
    Map<String, Object> config = new HashMap<>();
    parser.configure(config);
    assertThat(parser.getReadCharset(), equalTo(StandardCharsets.UTF_8));
  }
}

