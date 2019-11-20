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
package org.apache.metron.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.metron.stellar.dsl.Context;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class FileSystemFunctionsTest {
  private FileSystemFunctions.FileSystemGetter fsGetter = null;
  private static File hdfsBaseDir;
  private static File localBaseDir;
  private static MiniDFSCluster hdfsCluster;
  private static String hdfsPrefix;
  private static String localPrefix;
  private String prefix;
  private Context context = null;
  private FileSystemFunctions.FileSystemGet get;
  private FileSystemFunctions.FileSystemGetList getList;
  private FileSystemFunctions.FileSystemLs ls;
  private FileSystemFunctions.FileSystemPut put;
  private FileSystemFunctions.FileSystemRm rm;

  public static Collection<Object[]> types() {
    return Arrays.asList(new Object[][]{
      {FileSystemFunctions.FS_TYPE.HDFS}
    , {FileSystemFunctions.FS_TYPE.LOCAL}
    });
  }

  @BeforeAll
  public static void setupFS() throws IOException {
    {
      hdfsBaseDir = Files.createTempDirectory("test_hdfs").toFile().getAbsoluteFile();
      Configuration conf = new Configuration();
      conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, hdfsBaseDir.getAbsolutePath());
      MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
      hdfsCluster = builder.build();
      hdfsPrefix = "/";
    }
    {
      localPrefix = "target/fsTest/";
      if (new File(localPrefix).exists()) {
        new File(localPrefix).delete();
      }
      new File(localPrefix).mkdirs();
    }
  }

  @AfterAll
  public static void teardown() {
    {
      hdfsCluster.shutdown();
      FileUtil.fullyDelete(hdfsBaseDir);
    }
    {
      new File(localPrefix).delete();
    }
  }

  private void setupFsTypeAndFunctions(FileSystemFunctions.FS_TYPE type) {
    if(type == FileSystemFunctions.FS_TYPE.HDFS) {
      prefix=hdfsPrefix;
      fsGetter = () -> hdfsCluster.getFileSystem();
    }
    else {
      prefix=localPrefix;
      fsGetter = FileSystemFunctions.FS_TYPE.LOCAL;
    }
    get = new FileSystemFunctions.FileSystemGet(fsGetter);
    get.initialize(null);
    getList = new FileSystemFunctions.FileSystemGetList(fsGetter);
    getList.initialize(null);
    ls = new FileSystemFunctions.FileSystemLs(fsGetter);
    ls.initialize(null);
    put = new FileSystemFunctions.FileSystemPut(fsGetter);
    put.initialize(null);
    rm  = new FileSystemFunctions.FileSystemRm(fsGetter);
    rm.initialize(null);
  }

  @ParameterizedTest
  @MethodSource("types")
  public void testHappyPath(FileSystemFunctions.FS_TYPE type) {
    setupFsTypeAndFunctions(type);
    Object putOut = put.apply(Arrays.asList("foo", prefix + "testPut.dat"), null);
    assertTrue((Boolean) putOut);
    String getOut = (String)get.apply(Arrays.asList(prefix + "testPut.dat"), null);
    assertEquals("foo", getOut);
    String lsOut = (String) ls.apply(Arrays.asList(prefix), null);
    assertFalse(lsOut.contains("(empty)"));
    Boolean rmRet = (Boolean)rm.apply(Arrays.asList(prefix + "testPut.dat"), null);
    assertTrue(rmRet);
    lsOut = (String) ls.apply(Arrays.asList(prefix), null);
    assertTrue(lsOut.contains("(empty)"));
  }

  @ParameterizedTest
  @MethodSource("types")
  public void testGetList(FileSystemFunctions.FS_TYPE type) {
    setupFsTypeAndFunctions(type);
    Object putOut = put.apply(Arrays.asList("foo\nbar", prefix + "testPut.dat"), null);
    assertTrue((Boolean) putOut);
    String getOut = (String)get.apply(Arrays.asList(prefix + "testPut.dat"), null);
    assertEquals("foo\nbar", getOut);
    List<String> list = (List<String>) getList.apply(Arrays.asList(prefix + "testPut.dat"), null);
    assertEquals(2,list.size());
    assertEquals("foo",list.get(0));
    assertEquals("bar",list.get(1));
  }

  @ParameterizedTest
  @MethodSource("types")
  public void testPutMissingFile(FileSystemFunctions.FS_TYPE type) {
    setupFsTypeAndFunctions(type);
    Object o = put.apply(Arrays.asList("foo", null), null);
    assertFalse((Boolean) o);
    String lsOut = (String) ls.apply(Arrays.asList(prefix), null);
    assertTrue(lsOut.contains("(empty)"));
  }

  @ParameterizedTest
  @MethodSource("types")
  public void testRmTwice(FileSystemFunctions.FS_TYPE type) {
    setupFsTypeAndFunctions(type);
    Object putOut = put.apply(Arrays.asList("foo", prefix + "testPut.dat"), null);
    assertTrue((Boolean) putOut);
    Boolean rmRet = (Boolean)rm.apply(Arrays.asList(prefix + "testPut.dat"), null);
    assertTrue(rmRet);
    rmRet = (Boolean)rm.apply(Arrays.asList(prefix + "testPut.dat"), null);
    assertTrue(rmRet);
    String lsOut = (String) ls.apply(Arrays.asList(prefix), null);
    assertTrue(lsOut.contains("(empty)"));
  }

  @ParameterizedTest
  @MethodSource("types")
  public void testRecursiveRm(FileSystemFunctions.FS_TYPE type) {
    setupFsTypeAndFunctions(type);
    Object putOut = put.apply(Arrays.asList("foo", prefix + "blah/testPut.dat"), null);
    assertTrue((Boolean) putOut);
    putOut = put.apply(Arrays.asList("grok", prefix + "blah/testPut2.dat"), null);
    assertTrue((Boolean) putOut);
    assertEquals("foo", get.apply(Arrays.asList(prefix + "blah/testPut.dat"), null));
    assertEquals("grok", get.apply(Arrays.asList(prefix + "blah/testPut2.dat"), null));
    boolean rmRet = (Boolean)rm.apply(Arrays.asList(prefix + "blah", true), null);
    assertTrue(rmRet);
    String lsOut = (String) ls.apply(Arrays.asList(prefix), null);
    assertTrue(lsOut.contains("(empty)"));
  }

}
