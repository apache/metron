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

package org.apache.metron.parsers;

import org.apache.commons.io.FileUtils;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.json.simple.JSONObject;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@EnableRuleMigrationSupport
public class BasicParserTest {

  private static final String KEY1 = "key1";

  private static class SomeParserWithCharset extends BasicParser {

    @Override
    public void init() {

    }

    @Override
    public void configure(Map<String, Object> config) {
      setReadCharset(config);
    }

    @Override
    public Optional<MessageParserResult<JSONObject>> parseOptionalResult(byte[] parseMessage) {
      String message = new String(parseMessage, getReadCharset());
      Map<String, Object> out = new HashMap<>();
      out.put(KEY1, message);
      MessageParserResult<JSONObject> result = new DefaultMessageParserResult<>(
              Collections.singletonList(new JSONObject(out)));
      return Optional.of(result);
    }
  }

  private static class SomeParserNoCharset extends SomeParserWithCharset {
    @Override
    public void configure(Map<String, Object> config) {
      // don't set the charset
    }
  }


  private static final String SAMPLE_DATA = "Here is some sample data";
  private SomeParserWithCharset parserWithCharset;
  private SomeParserNoCharset parserNoCharset;
  private Map<String, Object> parserConfig;
  private File fileUTF_16;
  private File fileUTF_8;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @BeforeEach
  public void setup() throws IOException {
    tempFolder.create();
    parserWithCharset = new SomeParserWithCharset();
    parserNoCharset = new SomeParserNoCharset();
    parserConfig = new HashMap<>();
    fileUTF_16 = new File(tempFolder.getRoot(), "fileUTF-16");
    fileUTF_8 = new File(tempFolder.getRoot(), "fileUTF-8");
    writeDataEncodedAs(fileUTF_16, SAMPLE_DATA, StandardCharsets.UTF_16);
    writeDataEncodedAs(fileUTF_8, SAMPLE_DATA, StandardCharsets.UTF_8);
  }

  private void writeDataEncodedAs(File file, String data, Charset charset) throws IOException {
    byte[] bytes = data.getBytes(charset);
    FileUtils.writeByteArrayToFile(file, bytes);
  }

  @Test
  public void verify_encoding_translation_assumptions() throws IOException {
    // read in file encoded as UTF_16 bytes to a String using UTF_8 and UTF_16 encoding
    // the UTF_8 translation here should be a garbled mess because UTF_16 needs to have a
    // translation step for it to be correct in UTF_8
    String utf16_8 = readDataEncodedAs(fileUTF_16, StandardCharsets.UTF_8);
    String utf16_16 = readDataEncodedAs(fileUTF_16, StandardCharsets.UTF_16);
    File utf16_16_8 = new File(tempFolder.getRoot(), "outUTF-8");
    writeDataEncodedAs(utf16_16_8, utf16_16, StandardCharsets.UTF_8);
    String utf8_8 = readDataEncodedAs(utf16_16_8, StandardCharsets.UTF_8);
    assertThat(utf8_8, equalTo(utf16_16));
    assertThat(utf8_8, not(equalTo(utf16_8)));

    assertThat(utf8_8, equalTo(utf16_16));
    assertThat(utf8_8, not(equalTo(utf16_8)));
  }

  private String readDataEncodedAs(File file, Charset charset) throws IOException {
    return FileUtils.readFileToString(file, charset);
  }

  @Test
  public void parses_with_specified_encoding() {
    parserConfig.put(MessageParser.READ_CHARSET, StandardCharsets.UTF_16.toString());
    parserWithCharset.configure(parserConfig);
    Optional<MessageParserResult<JSONObject>> result = parserWithCharset
        .parseOptionalResult(SAMPLE_DATA.getBytes(StandardCharsets.UTF_16));
    MessageParserResult<JSONObject> json = result.get();
    assertThat(json.getMessages().size(), equalTo(1));
    assertThat(json.getMessages().get(0).get(KEY1), equalTo(SAMPLE_DATA));
  }

  @Test
  public void values_will_not_match_when_specified_encoding_is_wrong() {
    parserConfig.put(MessageParser.READ_CHARSET, StandardCharsets.UTF_8.toString());
    parserWithCharset.configure(parserConfig);
    Optional<MessageParserResult<JSONObject>> result = parserWithCharset
        .parseOptionalResult(SAMPLE_DATA.getBytes(StandardCharsets.UTF_16));
    MessageParserResult<JSONObject> json = result.get();
    assertThat(json.getMessages().size(), equalTo(1));
    assertThat(json.getMessages().get(0).get(KEY1), not(equalTo(SAMPLE_DATA)));
  }

  @Test
  public void parses_with_default_encoding_when_not_configured() {
    parserWithCharset.configure(parserConfig);
    Optional<MessageParserResult<JSONObject>> result = parserWithCharset
        .parseOptionalResult(SAMPLE_DATA.getBytes(StandardCharsets.UTF_8));
    MessageParserResult<JSONObject> json = result.get();
    assertThat(json.getMessages().size(), equalTo(1));
    assertThat(json.getMessages().get(0).get(KEY1), equalTo(SAMPLE_DATA));
  }

  @Test
  public void parses_with_default_encoding_from_basic_parser() {
    parserNoCharset.configure(parserConfig);
    Optional<MessageParserResult<JSONObject>> result = parserNoCharset
        .parseOptionalResult(SAMPLE_DATA.getBytes(StandardCharsets.UTF_8));
    MessageParserResult<JSONObject> json = result.get();
    assertThat(json.getMessages().size(), equalTo(1));
    assertThat(json.getMessages().get(0).get(KEY1), equalTo(SAMPLE_DATA));
  }

}
