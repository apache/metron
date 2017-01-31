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

import org.apache.hadoop.fs.FileSystem;

import java.util.Optional;
import java.util.function.Function;

public enum LocationStrategy {
  HDFS(fs -> {
    HDFSLocation location = new HDFSLocation();
    location.init(fs);
    return location;
  })
  ,FILE(fs -> {
    FileLocation location = new FileLocation();
    location.init(fs);
    return location;
  })
  ,URL(fs -> {
    URLLocation location = new URLLocation();
    location.init(fs);
    return location;
  })
  ;
  Function<FileSystem, RawLocation<?>> locationCreator;

  LocationStrategy(Function<FileSystem, RawLocation<?>> locationCreator) {
    this.locationCreator = locationCreator;
  }

  public static Optional<RawLocation<?>> getRawLocation(String loc, FileSystem fs) {
    for(LocationStrategy strategy : values()) {
      RawLocation<?> location = strategy.locationCreator.apply(fs);
      if(location.match(loc)) {
        return Optional.of(location);
      }
    }
    return Optional.empty();
  }

  public static Location getLocation(String loc, FileSystem fs) {
    Optional<RawLocation<?>> rawLoc = getRawLocation(loc, fs);
    if(rawLoc.isPresent()) {
      return new Location(loc, rawLoc.get());
    }
    else {
      throw new IllegalStateException("Unsupported type: " + loc);
    }
  }
}
