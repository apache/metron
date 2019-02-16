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
package org.apache.metron.parsers;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class AbstractParserConfigTest {

  protected static BasicParser parser = null;
  protected static String[] inputStrings;
  private String schemaJsonString = null;

  protected boolean validateJsonData(final String jsonSchema, final String jsonData)
      throws IOException, ProcessingException {

    final JsonNode d = JsonLoader.fromString(jsonData);
    final JsonNode s = JsonLoader.fromString(jsonSchema);

    final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
    JsonValidator v = factory.getValidator();

    ProcessingReport report = v.validate(s, d);

    return report.toString().contains("success");
  }

  protected String readSchemaFromFile(URL schema_url) throws Exception {
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(schema_url.getFile()),
        StandardCharsets.UTF_8));
    String line;
    StringBuilder sb = new StringBuilder();
    while ((line = br.readLine()) != null) {
      sb.append(line);
    }
    br.close();

    String schema_string = sb.toString().replaceAll("\n", "");
    schema_string = schema_string.replaceAll(" ", "");

    return schema_string;
  }

  protected String[] readTestDataFromFile(String test_data_url) throws Exception {
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(test_data_url), StandardCharsets.UTF_8));
    ArrayList<String> inputDataLines = new ArrayList<>();

    String line;
    while ((line = br.readLine()) != null) {
      inputDataLines.add(line.replaceAll("\n", ""));
    }
    br.close();
    String[] inputData = new String[inputDataLines.size()];
    inputData = inputDataLines.toArray(inputData);

    return inputData;
  }

  public void setSchemaJsonString(String schemaJsonString) {
    this.schemaJsonString = schemaJsonString;
  }


  public String getSchemaJsonString() {
    return this.schemaJsonString;
  }
}
