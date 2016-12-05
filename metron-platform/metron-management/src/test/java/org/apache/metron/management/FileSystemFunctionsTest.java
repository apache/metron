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
import org.apache.metron.common.dsl.Context;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
  private File baseDir;
  private MiniDFSCluster hdfsCluster;
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

  @Before
  public void setup() throws IOException {
    if(type == FileSystemFunctions.FS_TYPE.HDFS) {
      baseDir = Files.createTempDirectory("test_hdfs").toFile().getAbsoluteFile();
      Configuration conf = new Configuration();
      conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());
      MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
      hdfsCluster = builder.build();

      fsGetter = () -> hdfsCluster.getFileSystem();
      prefix = "/";
    }
    else {
      fsGetter = FileSystemFunctions.FS_TYPE.LOCAL;
      prefix = "target/fsTest/";
      if(new File(prefix).exists()) {
        new File(prefix).delete();
      }
      new File(prefix).mkdirs();
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

  @After
  public void teardown() {
    if(type == FileSystemFunctions.FS_TYPE.HDFS) {
      hdfsCluster.shutdown();
      FileUtil.fullyDelete(baseDir);
    }
    else {
      new File(prefix).delete();
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
