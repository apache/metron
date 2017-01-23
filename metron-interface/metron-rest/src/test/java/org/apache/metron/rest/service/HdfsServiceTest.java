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
package org.apache.metron.rest.service;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.metron.rest.config.HadoopConfig;
import org.apache.metron.rest.service.impl.HdfsServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;

import static org.apache.metron.rest.MetronRestConstants.TEST_PROFILE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={HadoopConfig.class, HdfsServiceTest.HdfsServiceTestContextConfiguration.class})
@ActiveProfiles(TEST_PROFILE)
public class HdfsServiceTest {

    @Configuration
    @Profile("test")
    static class HdfsServiceTestContextConfiguration {

        @Bean
        public HdfsService hdfsService() {
            return new HdfsServiceImpl();
        }
    }

    @Autowired
    private HdfsService hdfsService;

    @Test
    public void test() throws IOException {
        String rootDir = "./src/test/tmp";
        File rootFile = new File(rootDir);
        Path rootPath = new Path(rootDir);
        if (rootFile.exists()) {
            FileUtils.cleanDirectory(rootFile);
            FileUtils.deleteDirectory(rootFile);
        }
        assertEquals(true, rootFile.mkdir());
        String fileName1 = "fileName1";
        String fileName2 = "fileName2";
        Path path1 = new Path(rootDir, fileName1);
        String value1 = "value1";
        String value2 = "value2";
        Path path2 = new Path(rootDir, fileName2);
        String invalidFile = "invalidFile";
        Path pathInvalidFile = new Path(rootDir, invalidFile);

        FileStatus[] fileStatuses = hdfsService.list(new Path(rootDir));
        assertEquals(0, fileStatuses.length);


        hdfsService.write(path1, value1.getBytes());
        assertEquals(value1, FileUtils.readFileToString(new File(rootDir, fileName1)));
        assertEquals(value1, new String(hdfsService.read(path1)));

        fileStatuses = hdfsService.list(rootPath);
        assertEquals(1, fileStatuses.length);
        assertEquals(fileName1, fileStatuses[0].getPath().getName());

        hdfsService.write(path2, value2.getBytes());
        assertEquals(value2, FileUtils.readFileToString(new File(rootDir, fileName2)));
        assertEquals(value2, new String(hdfsService.read(path2)));

        fileStatuses = hdfsService.list(rootPath);
        assertEquals(2, fileStatuses.length);
        assertEquals(fileName1, fileStatuses[0].getPath().getName());
        assertEquals(fileName1, fileStatuses[0].getPath().getName());

        assertEquals(true, hdfsService.delete(path1, false));
        fileStatuses = hdfsService.list(rootPath);
        assertEquals(1, fileStatuses.length);
        assertEquals(fileName2, fileStatuses[0].getPath().getName());
        assertEquals(true, hdfsService.delete(path2, false));
        fileStatuses = hdfsService.list(rootPath);
        assertEquals(0, fileStatuses.length);

        try {
            hdfsService.read(pathInvalidFile);
            fail("Exception should be thrown when reading invalid file name");
        } catch(IOException e) {
        }
        assertEquals(false, hdfsService.delete(pathInvalidFile, false));

        FileUtils.deleteDirectory(new File(rootDir));
    }
}
