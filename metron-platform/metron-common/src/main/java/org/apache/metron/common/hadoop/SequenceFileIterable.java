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

package org.apache.metron.common.hadoop;

import com.google.common.collect.Iterators;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SequenceFileIterable implements Iterable<byte[]> {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private List<Path> files;
  private Configuration config;

  public SequenceFileIterable(List<Path> files, Configuration config) {
    this.files = files;
    this.config = config;
  }

  @Override
  public Iterator<byte[]> iterator() {
    return Iterators.concat(getIterators(files, config));
  }

  private Iterator<byte[]>[] getIterators(List<Path> files, Configuration config) {
    return files.stream().map(f -> new SequenceFileIterator(f, config)).toArray(Iterator[]::new);
  }

  /**
   * Cleans up all files read by this Iterable.
   *
   * @return true if success, false if any files were not deleted
   * @throws IOException if there's an error cleaning up files
   */
  public boolean cleanup() throws IOException {
    FileSystem fileSystem = FileSystem.get(config);
    boolean success = true;
    for (Path file : files) {
      success &= fileSystem.delete(file, false);
    }
    return success;
  }

  private static class SequenceFileIterator implements Iterator<byte[]> {
    private Path path;
    private Configuration config;
    private SequenceFile.Reader reader;
    private LongWritable key = new LongWritable();
    private BytesWritable value = new BytesWritable();
    private byte[] next;
    private boolean finished = false;

    public SequenceFileIterator(Path path, Configuration config) {
      this.path = path;
      this.config = config;
    }

    @Override
    public boolean hasNext() {
      if (!finished && null == reader) {
        try {
          reader = new SequenceFile.Reader(config, SequenceFile.Reader.file(path));
          LOGGER.debug("Writing file: {}", path.toString());
        } catch (IOException e) {
          throw new RuntimeException("Failed to get reader", e);
        }
      } else {
        LOGGER.debug("finished={}, reader={}, next={}", finished, reader, next);
      }
      try {
        //ensure hasnext is idempotent
        if (!finished) {
          if (null == next && reader.next(key, value)) {
            next = value.copyBytes();
          } else if (null == next) {
            close();
          }
        }
      } catch (IOException e) {
        close();
        throw new RuntimeException("Failed to get next record", e);
      }
      return (null != next);
    }

    private void close() {
      LOGGER.debug("Closing file: {}", path.toString());
      finished = true;
      try {
        if (reader != null) {
          reader.close();
          reader = null;
        }
      } catch (IOException e) {
        // ah well, we tried...
        LOGGER.warn("Error closing file", e);
      }
    }

    @Override
    public byte[] next() {
      byte[] ret = null;
      if (hasNext()) {
        ret = next;
        next = null; //don't want same record more than once
      } else {
        throw new NoSuchElementException("No more records");
      }
      return ret;
    }
  }
}
