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
package org.apache.metron.rest.service.impl;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.HdfsService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HdfsServiceImpl.class, FileSystem.class})
public class HdfsServiceImplExceptionTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private Configuration configuration;
    private HdfsService hdfsService;
    private String testDir = "./target/hdfsUnitTest";

    @Before
    public void setup() throws IOException {
        configuration = new Configuration();
        hdfsService = new HdfsServiceImpl(configuration);

        mockStatic(FileSystem.class);
    }

    @Test
    public void listShouldWrapExceptionInRestException() throws Exception {
      exception.expect(RestException.class);

      FileSystem fileSystem = mock(FileSystem.class);
      when(FileSystem.get(configuration)).thenReturn(fileSystem);
      when(fileSystem.listStatus(new Path(testDir))).thenThrow(new IOException());

      hdfsService.list(new Path(testDir));
    }

    @Test
    public void readShouldWrapExceptionInRestException() throws Exception {
        exception.expect(RestException.class);

        FileSystem fileSystem = mock(FileSystem.class);
        when(FileSystem.get(configuration)).thenReturn(fileSystem);
        when(fileSystem.open(new Path(testDir))).thenThrow(new IOException());

        hdfsService.read(new Path(testDir));
    }

    @Test
    public void writeShouldWrapExceptionInRestException() throws Exception {
        exception.expect(RestException.class);

        FileSystem fileSystem = mock(FileSystem.class);
        when(FileSystem.get(configuration)).thenReturn(fileSystem);
        when(fileSystem.create(new Path(testDir), true)).thenThrow(new IOException());

        hdfsService.write(new Path(testDir), "contents".getBytes(UTF_8),null, null,null);
    }

    @Test
    public void writeShouldThrowIfInvalidPermissions() throws Exception {
        exception.expect(RestException.class);

        FileSystem fileSystem = mock(FileSystem.class);
        when(FileSystem.get(configuration)).thenReturn(fileSystem);

        hdfsService.write(new Path(testDir,"test"),"oops".getBytes(UTF_8), "foo", "r-x","r--");
    }

    @Test
    public void deleteShouldWrapExceptionInRestException() throws Exception {
        exception.expect(RestException.class);

        FileSystem fileSystem = mock(FileSystem.class);
        when(FileSystem.get(configuration)).thenReturn(fileSystem);
        when(fileSystem.delete(new Path(testDir), false)).thenThrow(new IOException());

        hdfsService.delete(new Path(testDir), false);
    }
}
