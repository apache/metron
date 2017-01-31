package org.apache.metron.dataloads.nonbulk.flatfile.location;

import org.apache.hadoop.fs.*;
import org.apache.metron.dataloads.nonbulk.flatfile.importer.LocalImporter;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * Location can be either a local file or a file on HDFS.
 */
public class Location {

  private String loc;
  private RawLocation<?> rawLocation;

  public Location(String loc, RawLocation rawLocation) {
    this.loc = loc;
    this.rawLocation = rawLocation;

  }

  public Optional<List<Location>> getChildren() throws IOException {
      if(exists() && isDirectory()) {
        List<Location> children = new ArrayList<>();
        for(String child : rawLocation.list(loc).orElse(new ArrayList<>())) {
          children.add(new Location(child, rawLocation));
        }
        return Optional.of(children);
      }
      else {
        return Optional.empty();
      }
  }


  public boolean exists() throws IOException {
    return rawLocation.exists(loc);
  }

  public boolean isDirectory() throws IOException {
    return rawLocation.isDirectory(loc);
  }

  public BufferedReader openReader() throws IOException {
    return rawLocation.openReader(loc);
  }

  @Override
  public String toString() {
    return loc;
  }

  public static void fileVisitor(List<String> inputs
                         , final Consumer<Location> importConsumer
                         , final FileSystem fs
                         ) throws IOException {
    Stack<Location> stack = new Stack<>();
    for(String input : inputs) {
      Location loc = LocationStrategy.getLocation(input, fs);
      if(loc.exists()) {
        stack.add(loc);
      }
    }
    while(!stack.empty()) {
      Location loc = stack.pop();
      if(loc.isDirectory()) {
        for(Location child : loc.getChildren().orElse(Collections.emptyList())) {
          stack.push(child);
        }
      }
      else {
        importConsumer.accept(loc);
      }
    }
  }
}
