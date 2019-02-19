package org.apache.metron.writer.hdfs;/*
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.apache.storm.hdfs.bolt.format.DefaultFileNameFormat;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy.Units;
import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy;
import org.apache.storm.hdfs.common.rotation.RotationAction;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.mockito.Mockito.*;

public class SourceHandlerTest {
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private static final String SENSOR_NAME = "sensor";
  private static final String WRITER_NAME = "writerName";

  private File folder;
  private FileNameFormat testFormat;

  RotationAction rotAction1 = mock(RotationAction.class);
  RotationAction rotAction2 = mock(RotationAction.class);
  List<RotationAction> rotActions;

  SourceHandlerCallback callback = mock(SourceHandlerCallback.class);

  @Before
  public void setup() throws IOException {
    // Ensure each test has a unique folder to work with.
    folder = tempFolder.newFolder();
    testFormat = new DefaultFileNameFormat()
        .withPath(folder.toString())
        .withExtension(".json")
        .withPrefix("prefix-");

    rotActions = new ArrayList<>();
    rotActions.add(rotAction1);
    rotActions.add(rotAction2);
  }

  @Test
  public void testRotateOutputFile() throws IOException {
    SourceHandler handler = new SourceHandler(
        rotActions,
        new FileSizeRotationPolicy(10000, Units.MB), // Don't actually care about the rotation
        new CountSyncPolicy(1),
        testFormat,
        callback
    );

    handler.rotateOutputFile();

    // Function should ensure rotation actions and callback are called.
    verify(rotAction1).execute(any(), any());
    verify(rotAction2).execute(any(), any());
    verify(callback).removeKey();
  }
}
