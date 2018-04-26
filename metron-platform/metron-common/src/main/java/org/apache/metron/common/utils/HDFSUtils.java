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

package org.apache.metron.common.utils;

import java.io.IOException;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class HDFSUtils {

  /**
   * Reads full HDFS FS file contents into a List of Strings. Initializes file system with default
   * configuration. Opens and closes the file system on each call. Never null.
   *
   * @param path path to file
   * @return file contents as a String
   * @throws IOException
   */
  public static List<String> readFile(String path) throws IOException {
    return readFile(new Configuration(), path);
  }

  /**
   * Reads full HDFS FS file contents into a String. Opens and closes the file system on each call.
   * Never null.
   *
   * @param config Hadoop configuration
   * @param path path to file
   * @return file contents as a String
   * @throws IOException
   */
  public static List<String> readFile(Configuration config, String path) throws IOException {
    FileSystem fs = FileSystem.newInstance(config);
    Path hdfsPath = new Path(path);
    FSDataInputStream inputStream = fs.open(hdfsPath);
    return IOUtils.readLines(inputStream, "UTF-8");
  }

}
