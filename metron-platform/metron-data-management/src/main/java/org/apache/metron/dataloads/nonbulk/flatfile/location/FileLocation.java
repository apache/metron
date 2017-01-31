package org.apache.metron.dataloads.nonbulk.flatfile.location;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileLocation implements RawLocation {
  @Override
  public Optional<List<String>> list(String loc) {
    List<String> children = new ArrayList<>();
    for(File f : new File(loc).listFiles()) {
        children.add(f.getPath());
      }
    return Optional.of(children);
  }

  @Override
  public boolean exists(String loc) throws IOException {
    return new File(loc).exists();
  }

  @Override
  public boolean isDirectory(String loc) throws IOException {
    return new File(loc).isDirectory();
  }

  @Override
  public BufferedReader openReader(String loc) throws IOException {
    return new BufferedReader(new FileReader(loc));
  }

  @Override
  public boolean match(String loc) {
    return new File(loc).exists();
  }
}
