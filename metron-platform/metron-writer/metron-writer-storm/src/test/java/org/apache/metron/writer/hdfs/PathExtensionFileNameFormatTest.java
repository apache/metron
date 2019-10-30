/*
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

package org.apache.metron.writer.hdfs;

import org.apache.storm.hdfs.bolt.format.DefaultFileNameFormat;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PathExtensionFileNameFormatTest {

  private static final String PATH = "/apps/metron";
  private static final String EXTENSION = ".json";
  private static final String PATH_EXTENSION = "field_result";

  @Test
  public void testGetPath() {
    FileNameFormat delegate = new DefaultFileNameFormat().withExtension(EXTENSION).withPath(PATH);
    FileNameFormat sourceFormat = new PathExtensionFileNameFormat(PATH_EXTENSION, delegate);
    String actual = sourceFormat.getPath();
    String expected = PATH + "/" + PATH_EXTENSION;
    assertEquals(expected, actual);
  }

  @Test
  public void testGetPathEmptyPathExtension() {
    FileNameFormat delegate = new DefaultFileNameFormat().withExtension(EXTENSION).withPath(PATH);
    FileNameFormat sourceFormat = new PathExtensionFileNameFormat("", delegate);
    String actual = sourceFormat.getPath();
    assertEquals(PATH + "/", actual);
  }
}
