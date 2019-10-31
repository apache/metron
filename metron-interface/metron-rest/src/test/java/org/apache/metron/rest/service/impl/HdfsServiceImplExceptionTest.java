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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//@RunWith(PowerMockRunner.class)
//@PrepareForTest({HdfsServiceImpl.class, FileSystem.class})
public class HdfsServiceImplExceptionTest {
    private Configuration configuration;
    private HdfsService hdfsService;
    private String testDir = "./target/hdfsUnitTest";

    @BeforeEach
    public void setup() {
        configuration = new Configuration();
        hdfsService = new HdfsServiceImpl(configuration);

//        mockStatic(FileSystem.class);
    }

    @Test
    public void listShouldWrapExceptionInRestException() throws Exception {
      FileSystem fileSystem = mock(FileSystem.class);
      when(FileSystem.get(configuration)).thenReturn(fileSystem);
      when(fileSystem.listStatus(new Path(testDir))).thenThrow(new IOException());

      assertThrows(RestException.class, () -> hdfsService.list(new Path(testDir)));
    }

    @Test
    public void readShouldWrapExceptionInRestException() throws Exception {
        FileSystem fileSystem = mock(FileSystem.class);
        when(FileSystem.get(configuration)).thenReturn(fileSystem);
        when(fileSystem.open(new Path(testDir))).thenThrow(new IOException());

        assertThrows(RestException.class, () -> hdfsService.read(new Path(testDir)));
    }

    @Test
    public void writeShouldWrapExceptionInRestException() throws Exception {
        FileSystem fileSystem = mock(FileSystem.class);
        when(FileSystem.get(configuration)).thenReturn(fileSystem);
        when(fileSystem.create(new Path(testDir), true)).thenThrow(new IOException());

        assertThrows(RestException.class,
                () -> hdfsService.write(new Path(testDir), "contents".getBytes(UTF_8),null, null,null));
    }

    @Test
    public void writeShouldThrowIfInvalidPermissions() throws Exception {
        FileSystem fileSystem = mock(FileSystem.class);
        when(FileSystem.get(configuration)).thenReturn(fileSystem);

        assertThrows(RestException.class,
                () -> hdfsService.write(new Path(testDir,"test"),"oops".getBytes(UTF_8), "foo", "r-x","r--"));
    }

    @Test
    public void deleteShouldWrapExceptionInRestException() throws Exception {
        FileSystem fileSystem = mock(FileSystem.class);
        when(FileSystem.get(configuration)).thenReturn(fileSystem);
        when(fileSystem.delete(new Path(testDir), false)).thenThrow(new IOException());

        assertThrows(RestException.class, () -> hdfsService.delete(new Path(testDir), false));
    }
}
