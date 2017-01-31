package org.apache.metron.dataloads.nonbulk.flatfile.location;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface RawLocation<T> {
  Optional<List<String>> list(String loc) throws IOException;
  boolean exists(String loc) throws IOException;
  boolean isDirectory(String loc) throws IOException;
  BufferedReader openReader(String loc) throws IOException;
  boolean match(String loc);
  default void init(T state) {

  }
}
