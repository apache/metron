package org.apache.metron.bundles.util;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class ResourceCopier {

  public static void copyResources(Path sourcePath, Path targetPath) throws IOException{
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

        Files.copy(file, target, REPLACE_EXISTING);

        return FileVisitResult.CONTINUE;
      }
    });
  }
}
