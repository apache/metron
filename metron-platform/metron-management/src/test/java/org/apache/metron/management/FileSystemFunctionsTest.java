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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.metron.stellar.dsl.Context;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@RunWith(Parameterized.class)
public class FileSystemFunctionsTest {
  private FileSystemFunctions.FS_TYPE type;
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

  public FileSystemFunctionsTest(FileSystemFunctions.FS_TYPE type) {
    this.type = type;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> types() {
    return Arrays.asList(new Object[][]{
      {FileSystemFunctions.FS_TYPE.HDFS}
    , {FileSystemFunctions.FS_TYPE.LOCAL}
    });
  }

  @BeforeClass
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

  @Before
  public void setup() throws IOException {
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

  @AfterClass
  public static void teardown() {
    {
      hdfsCluster.shutdown();
      FileUtil.fullyDelete(hdfsBaseDir);
    }
    {
      new File(localPrefix).delete();
    }
  }

  @Test
  public void testHappyPath() {
    Object putOut = put.apply(Arrays.asList("foo", prefix + "testPut.dat"), null);
    Assert.assertTrue((Boolean) putOut);
    String getOut = (String)get.apply(Arrays.asList(prefix + "testPut.dat"), null);
    Assert.assertEquals("foo", getOut);
    String lsOut = (String) ls.apply(Arrays.asList(prefix), null);
    Assert.assertFalse(lsOut.contains("(empty)"));
    Boolean rmRet = (Boolean)rm.apply(Arrays.asList(prefix + "testPut.dat"), null);
    Assert.assertTrue(rmRet);
    lsOut = (String) ls.apply(Arrays.asList(prefix), null);
    Assert.assertTrue(lsOut.contains("(empty)"));
  }

  @Test
  public void testGetList() {
    Object putOut = put.apply(Arrays.asList("foo\nbar", prefix + "testPut.dat"), null);
    Assert.assertTrue((Boolean) putOut);
    String getOut = (String)get.apply(Arrays.asList(prefix + "testPut.dat"), null);
    Assert.assertEquals("foo\nbar", getOut);
    List<String> list = (List<String>) getList.apply(Arrays.asList(prefix + "testPut.dat"), null);
    Assert.assertEquals(2,list.size());
    Assert.assertEquals("foo",list.get(0));
    Assert.assertEquals("bar",list.get(1));
  }

  @Test
  public void testPutMissingFile() {
    Object o = put.apply(Arrays.asList("foo", null), null);
    Assert.assertFalse((Boolean) o);
    String lsOut = (String) ls.apply(Arrays.asList(prefix), null);
    Assert.assertTrue(lsOut.contains("(empty)"));
  }

  @Test
  public void testRmTwice() {
    Object putOut = put.apply(Arrays.asList("foo", prefix + "testPut.dat"), null);
    Assert.assertTrue((Boolean) putOut);
    Boolean rmRet = (Boolean)rm.apply(Arrays.asList(prefix + "testPut.dat"), null);
    Assert.assertTrue(rmRet);
    rmRet = (Boolean)rm.apply(Arrays.asList(prefix + "testPut.dat"), null);
    Assert.assertTrue(rmRet);
    String lsOut = (String) ls.apply(Arrays.asList(prefix), null);
    Assert.assertTrue(lsOut.contains("(empty)"));
  }

  @Test
  public void testRecursiveRm() {
    Object putOut = put.apply(Arrays.asList("foo", prefix + "blah/testPut.dat"), null);
    Assert.assertTrue((Boolean) putOut);
    putOut = put.apply(Arrays.asList("grok", prefix + "blah/testPut2.dat"), null);
    Assert.assertTrue((Boolean) putOut);
    Assert.assertEquals("foo", (String)get.apply(Arrays.asList(prefix + "blah/testPut.dat"), null));
    Assert.assertEquals("grok", (String)get.apply(Arrays.asList(prefix + "blah/testPut2.dat"), null));
    boolean rmRet = (Boolean)rm.apply(Arrays.asList(prefix + "blah", true), null);
    Assert.assertTrue(rmRet);
    String lsOut = (String) ls.apply(Arrays.asList(prefix), null);
    Assert.assertTrue(lsOut.contains("(empty)"));
  }

}
