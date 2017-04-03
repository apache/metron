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
import org.junit.Assert;
import org.junit.Test;

public class SourceFileNameFormatTest {

  private static final String PATH = "/apps/metron/";
  private static final String EXTENSION = ".json";
  private static final String PATH_EXTENSION = "field_result";
  private static final String SOURCE_TYPE = "sourceType";

  @Test
  public void testGetPath() {
    FileNameFormat delegate = new DefaultFileNameFormat().withExtension(EXTENSION).withPath(PATH);
    FileNameFormat sourceFormat = new SourceFileNameFormat(SOURCE_TYPE, PATH_EXTENSION, delegate);
    String actual = sourceFormat.getPath();
    String expected = PATH + PATH_EXTENSION + "/" + SOURCE_TYPE;
    // Run a replace because extra "/" will be dropped anyway when writing.  Could do this in the implemention, but it's unnecessary.
    Assert.assertEquals(expected, actual.replace("//", "/"));
  }

  @Test
  public void testGetPathEmptyPathExtension() {
    FileNameFormat delegate = new DefaultFileNameFormat().withExtension(EXTENSION).withPath(PATH);
    FileNameFormat sourceFormat = new SourceFileNameFormat(SOURCE_TYPE, "", delegate);
    String actual = sourceFormat.getPath();
    String expected = PATH + "" + "/" + SOURCE_TYPE;
    // Run a replace because extra "/" will be dropped anyway when writing.  Could do this in the implemention, but it's unnecessary.
    Assert.assertEquals(expected, actual.replace("//", "/"));
  }
}
