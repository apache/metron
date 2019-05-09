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
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class HDFSUtils {

  public static byte[] readBytes(String path) throws IOException {
    return readBytes(new Path(path));
  }

  /**
   * Reads a provided path as raw bytes.
   *
   * @param inPath The path to be read
   * @return The raw bytes of the contents
   * @throws IOException If an error occurs during reading the path
   */
  public static byte[] readBytes(Path inPath) throws IOException {
    FileSystem fs = FileSystem.get(inPath.toUri(), new Configuration());
    try (FSDataInputStream inputStream = fs.open(inPath)) {
      return IOUtils.toByteArray(inputStream);
    }
  }

  /**
   * Reads full file contents into a List of Strings. Reads from local FS if file:/// used as the
   * scheme. Initializes file system with default configuration.
   * Automatically handles file system translation for the provided path's scheme.
   * Opens and closes the file system on each call. Never null.
   * Null/empty scheme defaults to default configured FS.
   *
   * @param path path to file
   * @return file contents as a String
   * @throws IOException If an error occurs during reading the path
   */
  public static List<String> readFile(String path) throws IOException {
    return readFile(new Configuration(), path);
  }

  /**
   * Reads full file contents into a String. Reads from local FS if file:/// used as the scheme.
   * Opens and closes the file system on each call.
   * Never null. Automatically handles file system translation for the provided path's scheme.
   * Null/empty scheme defaults to default configured FS.
   *
   * @param config Hadoop configuration
   * @param path path to file
   * @return file contents as a String
   * @throws IOException If an error occurs during reading the path
   */
  public static List<String> readFile(Configuration config, String path) throws IOException {
    Path inPath = new Path(path);
    FileSystem fs = FileSystem.get(inPath.toUri(), config);
    try (FSDataInputStream inputStream = fs.open(inPath)) {
      return IOUtils.readLines(inputStream, "UTF-8");
    }
  }

  /**
   * Write file to HDFS. Writes to local FS if file:/// used as the scheme.
   * Automatically handles file system translation for the provided path's scheme.
   * Null/empty scheme defaults to default configured FS.
   *
   * @param config filesystem configuration
   * @param bytes bytes to write
   * @param path output path
   * @throws IOException This is the generic Hadoop "everything is an IOException."
   */
  public static void write(Configuration config, byte[] bytes, String path) throws IOException {
    Path outPath = new Path(path);
    FileSystem fs = FileSystem.get(outPath.toUri(), config);
    fs.mkdirs(outPath.getParent());
    try (FSDataOutputStream outputStream = fs.create(outPath)) {
      outputStream.write(bytes);
      outputStream.flush();
    }
  }
}
