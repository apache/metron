package org.apache.metron.dataloads.nonbulk.flatfile.location;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HDFSLocation implements RawLocation<FileSystem> {

  FileSystem fs = null;

  @Override
  public Optional<List<String>> list(String loc) throws IOException {
    List<String> children = new ArrayList<>();
    for(FileStatus f : fs.listStatus(new Path(loc)) ) {
        children.add(f.getPath().toString());
      }
    return Optional.of(children);
  }

  @Override
  public boolean exists(String loc) throws IOException {
    return fs.exists(new Path(loc));
  }

  @Override
  public boolean isDirectory(String loc) throws IOException {
    return fs.isDirectory(new Path(loc));
  }

  @Override
  public BufferedReader openReader(String loc) throws IOException {
    return new BufferedReader(new InputStreamReader(fs.open(new Path(loc))));
  }

  @Override
  public boolean match(String loc) {
    try {
      return loc.startsWith("hdfs://") && exists(loc);
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  public void init(FileSystem state) {
    this.fs = state;
  }


}
