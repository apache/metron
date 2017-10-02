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

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.metron.rest.service.HdfsService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class HdfsServiceImplTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private Configuration configuration;
    private HdfsService hdfsService;
    private String testDir = "./target/hdfsUnitTest";

    @Before
    public void setup() throws IOException {
        configuration = new Configuration();
        hdfsService = new HdfsServiceImpl(configuration);
        File file = new File(testDir);
        if (!file.exists()) {
          file.mkdirs();
        }
        FileUtils.cleanDirectory(file);
    }

    @After
    public void teardown() throws IOException {
        File file = new File(testDir);
        FileUtils.cleanDirectory(file);
    }

    @Test
    public void listShouldListFiles() throws Exception {
        FileUtils.writeStringToFile(new File(testDir, "file1.txt"), "value1");
        FileUtils.writeStringToFile(new File(testDir, "file2.txt"), "value2");

        List<String> paths = hdfsService.list(new Path(testDir));
        Collections.sort(paths);
        assertEquals(2, paths.size());
        assertEquals("file1.txt", paths.get(0));
        assertEquals("file2.txt", paths.get(1));
    }


    @Test
    public void readShouldProperlyReadContents() throws Exception {
        String contents = "contents";
        FileUtils.writeStringToFile(new File(testDir, "readTest.txt"), contents);

        assertEquals("contents", hdfsService.read(new Path(testDir, "readTest.txt")));
    }

    @Test
    public void writeShouldProperlyWriteContents() throws Exception {
        String contents = "contents";
        hdfsService.write(new Path(testDir, "writeTest.txt"), contents.getBytes(UTF_8),null,null,null);

        assertEquals("contents", FileUtils.readFileToString(new File(testDir, "writeTest.txt")));
    }

    @Test
    public void writeShouldProperlyWriteContentsWithPermissions() throws Exception {
        String contents = "contents";
        Path thePath = new Path(testDir,"writeTest2.txt");
        hdfsService.write(thePath, contents.getBytes(UTF_8),"rw-","r-x","r--");

        assertEquals("contents", FileUtils.readFileToString(new File(testDir, "writeTest2.txt")));
        assertEquals(FileSystem.get(configuration).listStatus(thePath)[0].getPermission().toShort(),
        new FsPermission(FsAction.READ_WRITE,FsAction.READ_EXECUTE,FsAction.READ).toShort());
    }

    @Test
    public void deleteShouldProperlyDeleteFile() throws Exception {
        String contents = "contents";
        FileUtils.writeStringToFile(new File(testDir, "deleteTest.txt"), contents);

        List<String> paths = hdfsService.list(new Path(testDir));
        assertEquals(1, paths.size());
        assertEquals("deleteTest.txt", paths.get(0));

        hdfsService.delete(new Path(testDir, "deleteTest.txt"), false);

        paths = hdfsService.list(new Path(testDir));
        assertEquals(0, paths.size());
    }
}
