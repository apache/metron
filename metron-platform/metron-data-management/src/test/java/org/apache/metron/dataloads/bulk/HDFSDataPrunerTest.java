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
package org.apache.metron.dataloads.bulk;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Level;
import org.apache.metron.test.utils.UnitTestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class HDFSDataPrunerTest {


    private static File dataPath = new File("src/test/resources/HDFSDataPrunerTest");

    private Date todaysDate;
    private Date yesterday = new Date();


    @BeforeAll
    public static void beforeClass() {

        if (dataPath.isDirectory()) {
            dataPath.delete();
        }

        if (!dataPath.mkdirs()) {
            throw new RuntimeException("Couldn't create dataPath at: " + dataPath.getAbsolutePath());
        }

        dataPath.deleteOnExit();

    }


    @BeforeEach
    public void setUp() {

        Calendar today = Calendar.getInstance();
        today.clear(Calendar.HOUR);
        today.clear(Calendar.MINUTE);
        today.clear(Calendar.SECOND);
        todaysDate = today.getTime();
        yesterday.setTime(todaysDate.getTime() - TimeUnit.DAYS.toMillis(1));

    }

    @Test
    public void testFailsOnTodaysDate() {
        assertThrows(
            StartDateException.class,
            () -> new HDFSDataPruner(todaysDate, 30, "file:///", dataPath.getAbsolutePath() + "/file-*"));
    }

    @Test
    public void testDeletesCorrectFiles() throws Exception {

        createTestFiles();

        HDFSDataPruner pruner = new HDFSDataPruner(yesterday, 30, "file:///", dataPath.getAbsolutePath() + "/file-*");

        long prunedCount = pruner.prune();
        assertEquals(45, prunedCount, "Should have pruned 45 files- pruned: " + prunedCount);

        File[] filesLeft = dataPath.listFiles();
        File[] filesList = new File[filesLeft.length];
        for (int i = 0; i < 5; i++) {
            filesList[i] = new File(dataPath.getPath() + "//file-" + String.format("%02d", i));
        }

        Arrays.sort(filesLeft);
        assertArrayEquals(filesLeft, filesList, "First four files should have been left behind");


    }

    @Test
    public void testThrowsIsDirectory() throws Exception {

        FileSystem testFS = mock(FileSystem.class);
        when(testFS.isDirectory( any())).thenThrow(new IOException("Test Exception"));

        HDFSDataPruner pruner = new HDFSDataPruner(yesterday, 30, "file:///", dataPath.getAbsolutePath() + "/file-*");
        pruner.fileSystem = testFS;
        HDFSDataPruner.DateFileFilter filter = new HDFSDataPruner.DateFileFilter(pruner, true);
        UnitTestHelper.setLog4jLevel(HDFSDataPruner.class, Level.FATAL);
        assertThrows(RuntimeException.class, () -> filter.accept(new Path("foo")));
        UnitTestHelper.setLog4jLevel(HDFSDataPruner.class, Level.ERROR);
    }

    @Test
    public void testIgnoresDirectories() throws Exception {

        FileSystem testFS = mock(FileSystem.class);
        when(testFS.isDirectory( any())).thenReturn(true);

        HDFSDataPruner pruner = new HDFSDataPruner(yesterday, 30, "file:///", dataPath.getAbsolutePath() + "/file-*");
        pruner.fileSystem = testFS;
        HDFSDataPruner.DateFileFilter filter = new HDFSDataPruner.DateFileFilter(pruner, false);
        assertFalse(filter.accept(new Path("/tmp")), "Should ignore directories");

    }

    @Test
    public void testThrowBadFile() throws Exception {

        FileSystem testFS = mock(FileSystem.class);
        when(testFS.isDirectory( any())).thenReturn(false);
        when(testFS.getFileStatus( any())).thenThrow(new IOException("Test Exception"));

        HDFSDataPruner pruner = new HDFSDataPruner(yesterday, 30, "file:///", dataPath.getAbsolutePath() + "/file-*");

        pruner.fileSystem = testFS;
        HDFSDataPruner.DateFileFilter filter = new HDFSDataPruner.DateFileFilter(pruner, true);
        UnitTestHelper.setLog4jLevel(HDFSDataPruner.class, Level.FATAL);
        assertThrows(RuntimeException.class, () -> filter.accept(new Path("foo")));
        UnitTestHelper.setLog4jLevel(HDFSDataPruner.class, Level.ERROR);
    }

    private void createTestFiles() throws IOException {

        //create files
        for (int i = 0; i < 50; i++) {
            File file = new File(dataPath.getAbsolutePath() + "//file-" + String.format("%02d", i));
            file.createNewFile();
            file.deleteOnExit();
        }

        //Set modification date today - 1 day
        for (int i = 5; i < 25; i++) {
            File file = new File(dataPath.getAbsolutePath() + "//file-" + String.format("%02d", i));
            file.setLastModified(todaysDate.getTime() - TimeUnit.DAYS.toMillis(1));
            file.deleteOnExit();
        }

        //Set modification date today - 10 days
        for (int i = 25; i < 40; i++) {
            File file = new File(dataPath.getAbsolutePath() + "//file-" + String.format("%02d", i));
            file.setLastModified(todaysDate.getTime() - TimeUnit.DAYS.toMillis(10));
            file.deleteOnExit();
        }

        //Set modification date today - 20 days
        for (int i = 40; i < 50; i++) {
            File file = new File(dataPath.getAbsolutePath() + "//file-" + String.format("%02d", i));
            file.setLastModified(todaysDate.getTime() - TimeUnit.DAYS.toMillis(20));
            file.deleteOnExit();
        }

    }
}

