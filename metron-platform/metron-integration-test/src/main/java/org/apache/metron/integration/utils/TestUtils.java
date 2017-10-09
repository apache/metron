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
package org.apache.metron.integration.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class TestUtils {

  public static List<byte[]> readSampleData(String samplePath) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(samplePath));
    List<byte[]> ret = new ArrayList<>();
    for (String line = null; (line = br.readLine()) != null; ) {
      ret.add(line.getBytes());
    }
    br.close();
    return ret;
  }

  public static void write(File file, String[] contents) throws IOException {
    StringBuilder b = new StringBuilder();
    for (String line : contents) {
      b.append(line);
      b.append(System.lineSeparator());
    }
    write(file, b.toString());
  }

  /**
   * Returns file passed in after writing
   *
   * @param file
   * @param contents
   * @return
   * @throws IOException
   */
  public static File write(File file, String contents) throws IOException {
    com.google.common.io.Files.createParentDirs(file);
    com.google.common.io.Files.write(contents, file, StandardCharsets.UTF_8);
    return file;
  }

  /**
   * Cleans up after test run via runtime shutdown hooks
   */
  public static File createTempDir(String prefix) throws IOException {
    final Path tmpDir = Files.createTempDirectory(prefix);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          cleanDir(tmpDir);
        } catch (IOException e) {
          System.out.println("Warning: Unable to clean tmp folder.");
        }
      }

    });
    return tmpDir.toFile();
  }

  public static void cleanDir(Path dir) throws IOException {
    Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc == null) {
          return FileVisitResult.CONTINUE;
        } else {
          throw exc;
        }
      }
    });
  }

  public static File createDir(File parent, String child) {
    File newDir = new File(parent, child);
    newDir.mkdirs();
    return newDir;
  }

}
