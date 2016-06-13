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
package org.apache.metron.test.utils;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.Stack;

import static java.lang.String.format;

public class UnitTestHelper {
  public static String findDir(String name) {
    return findDir(new File("."), name);
  }

  public static String findDir(File startDir, String name) {
    Stack<File> s = new Stack<File>();
    s.push(startDir);
    while (!s.empty()) {
      File parent = s.pop();
      if (parent.getName().equalsIgnoreCase(name)) {
        return parent.getAbsolutePath();
      } else {
        File[] children = parent.listFiles();
        if (children != null) {
          for (File child : children) {
            s.push(child);
          }
        }
      }
    }
    return null;
  }

  public static <T> void assertSetEqual(String type, Set<T> expectedPcapIds, Set<T> found) {
    boolean mismatch = false;
    for (T f : found) {
      if (!expectedPcapIds.contains(f)) {
        mismatch = true;
        System.out.println("Found " + type + " that I did not expect: " + f);
      }
    }
    for (T expectedId : expectedPcapIds) {
      if (!found.contains(expectedId)) {
        mismatch = true;
        System.out.println("Expected " + type + " that I did not index: " + expectedId);
      }
    }
    Assert.assertFalse(mismatch);
  }

  public static void verboseLogging() {
    verboseLogging("%d [%p|%c|%C{1}] %m%n", Level.ALL);
  }

  public static void verboseLogging(String pattern, Level level) {
    ConsoleAppender console = new ConsoleAppender(); //create appender
    //configure the appender
    console.setLayout(new PatternLayout(pattern));
    console.setThreshold(level);
    console.activateOptions();
    //add appender to any Logger (here is root)
    Logger.getRootLogger().addAppender(console);
  }

  /**
   * Create directory that is automatically cleaned up after the
   * JVM shuts down through use of a Runtime shutdown hook.
   *
   * @param dir Directory to create, including missing parent directories
   * @return File handle to the created temp directory
   */
  public static File createTempDir(File dir) throws IOException {
    return createTempDir(dir, true);
  }

  /**
   * Create directory that is automatically cleaned up after the
   * JVM shuts down through use of a Runtime shutdown hook.
   *
   * @param dir Directory to create, including missing parent directories
   * @param cleanup true/false
   * @return File handle to the created temp directory
   */
  public static File createTempDir(File dir, boolean cleanup) throws IOException {
    if (!dir.mkdirs() && !dir.exists()) {
      throw new IOException(String.format("Failed to create directory structure '%s'", dir.toString()));
    }
    if (cleanup) {
      addCleanupHook(dir.toPath());
    }
    return dir;
  }

  /**
   * Create directory that is automatically cleaned up after the
   * JVM shuts down through use of a Runtime shutdown hook.
   *
   * @param prefix Prefix to apply to temp directory name
   * @return File handle to the created temp directory
   * @throws IOException Unable to create temp directory
   */
  public static File createTempDir(String prefix) throws IOException {
    return createTempDir(prefix, true);
  }

  /**
   * Create directory that is optionally cleaned up after the
   * JVM shuts down through use of a Runtime shutdown hook.
   *
   * @param prefix Prefix to apply to temp directory name
   * @param cleanup true/false
   * @return File handle to the created temp directory
   * @throws IOException Unable to create temp directory
   */
  public static File createTempDir(String prefix, boolean cleanup) throws IOException {
    Path tmpDir = Files.createTempDirectory(prefix);
    addCleanupHook(tmpDir);
    return tmpDir.toFile();
  }

  /**
   * Adds JVM shutdown hook that will recursively delete the passed directory
   *
   * @param dir Directory that will be cleaned up upon JVM shutdown
   */
  public static void addCleanupHook(final Path dir) {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          cleanDir(dir);
        } catch (IOException e) {
          System.out.println(format("Warning: Unable to clean folder '%s'", dir.toString()));
        }
      }
    });
  }

  /**
   * Recursive directory delete
   *
   * @param dir Directory to delete
   * @throws IOException Unable to delete
   */
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
    Files.delete(dir);
  }

  /**
   * Write contents to a file
   *
   * @param file
   * @param contents
   * @return file handle
   * @throws IOException
   */
  public static File write(File file, String contents) throws IOException {
    com.google.common.io.Files.createParentDirs(file);
    com.google.common.io.Files.write(contents, file, StandardCharsets.UTF_8);
    return file;
  }
}
