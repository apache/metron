/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Utility class to copy a whole directory.  This is useful for copying files from
 * src/test/resources to target/testbed for example
 */
public class ResourceCopier {

  /**
   * Copy the resources from sourcePath to targetPath. If a resource exists, it will be overwritten.
   * This is the equivolent of calling <code>copyResources(source,target,true)</code>
   *
   * @param sourcePath source {@link Path}
   * @param targetPath target {@link Path}
   */
  public static void copyResources(final Path sourcePath, final Path targetPath)
      throws IOException {
    synchronized (ResourceCopier.class) {
      copyResources(sourcePath, targetPath, true);
    }
  }

  /**
   * Copy the resources from sourcePath to targetPath. The overwrite flag determines if existing
   * resources are overwritten or not.
   *
   * @param sourcePath source {@link Path}
   * @param targetPath target {@link Path}
   * @param overwrite if true, the the target will be overwritten with {@link
   * java.nio.file.StandardCopyOption#REPLACE_EXISTING}
   */
  public static void copyResources(final Path sourcePath, final Path targetPath,
      final boolean overwrite) throws IOException {
    synchronized (ResourceCopier.class) {
      Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            throws IOException {

          Path relativeSource = sourcePath.relativize(dir);
          Path target = targetPath.resolve(relativeSource);

          Files.createDirectories(target);

          return FileVisitResult.CONTINUE;

        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            throws IOException {

          Path relativeSource = sourcePath.relativize(file);
          Path target = targetPath.resolve(relativeSource);

          if (overwrite) {
            Files.copy(file, target, REPLACE_EXISTING);
          } else {
            if (!target.toFile().exists()) {
              Files.copy(file, target);
            }
          }

          return FileVisitResult.CONTINUE;
        }
      });
    }
  }
}
