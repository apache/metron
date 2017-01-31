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
