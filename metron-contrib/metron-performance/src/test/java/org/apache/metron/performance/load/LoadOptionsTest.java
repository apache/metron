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
package org.apache.metron.performance.load;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class LoadOptionsTest {
  @Test
  public void testHappyPath() {
    CommandLine cli = LoadOptions.parse(new PosixParser(), new String[] { "-eps", "1000", "-ot","foo"});
    EnumMap<LoadOptions, Optional<Object>> results = LoadOptions.createConfig(cli);
    assertEquals(1000L, results.get(LoadOptions.EPS).get());
    assertEquals("foo", results.get(LoadOptions.OUTPUT_TOPIC).get());
    assertEquals(LoadGenerator.CONSUMER_GROUP, results.get(LoadOptions.CONSUMER_GROUP).get());
    assertEquals(Runtime.getRuntime().availableProcessors(), results.get(LoadOptions.NUM_THREADS).get());
    assertFalse(results.get(LoadOptions.BIASED_SAMPLE).isPresent());
    assertFalse(results.get(LoadOptions.CSV).isPresent());
  }

  @Test
  public void testCsvPresent() {
      CommandLine cli = LoadOptions.parse(new PosixParser(), new String[]{"-c", "/tmp/blah"});
      EnumMap<LoadOptions, Optional<Object>> results = LoadOptions.createConfig(cli);
      assertEquals(new File("/tmp/blah"), results.get(LoadOptions.CSV).get());
  }

  @Test
  public void testCsvMissing() {
      CommandLine cli = LoadOptions.parse(new PosixParser(), new String[]{});
      EnumMap<LoadOptions, Optional<Object>> results = LoadOptions.createConfig(cli);
      assertFalse(results.get(LoadOptions.CSV).isPresent());
  }

  @Test
  public void testThreadsByCores() {
      CommandLine cli = LoadOptions.parse(new PosixParser(), new String[]{"-p", "2C"});
      EnumMap<LoadOptions, Optional<Object>> results = LoadOptions.createConfig(cli);
      assertEquals(2 * Runtime.getRuntime().availableProcessors(), results.get(LoadOptions.NUM_THREADS).get());
  }

  @Test
  public void testThreadsByNum() {
      CommandLine cli = LoadOptions.parse(new PosixParser(), new String[]{"-p", "5"});
      EnumMap<LoadOptions, Optional<Object>> results = LoadOptions.createConfig(cli);
      assertEquals(5, results.get(LoadOptions.NUM_THREADS).get());
  }

  @Test
  public void testTemplatePresent() throws Exception {
    File templateFile= new File("target/template");
    String template = "test template1";
    try(BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(templateFile), StandardCharsets.UTF_8))) {
      IOUtils.write(template, w );
    }
    templateFile.deleteOnExit();
    CommandLine cli = LoadOptions.parse(new PosixParser(), new String[]{"-t", templateFile.getPath()});
    EnumMap<LoadOptions, Optional<Object>> results = LoadOptions.createConfig(cli);
    List<String> templates = (List<String>) results.get(LoadOptions.TEMPLATE).get();
    assertEquals(1, templates.size());
    assertEquals(template, templates.get(0));
  }

  @Test
  public void testTemplateMissing() {
    assertThrows(IllegalStateException.class, () -> LoadOptions.createConfig(LoadOptions.parse(new PosixParser(), new String[]{"-t", "target/template2"})));
  }
}
