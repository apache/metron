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

  public RawLocation<?> getRawLocation() {
    return rawLocation;
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
