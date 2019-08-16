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
package org.apache.metron.dataloads.nonbulk.flatfile.importer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.metron.common.utils.file.ReaderSpliterator;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.dataloads.extractor.inputformat.WholeFileFormat;
import org.apache.metron.common.utils.cli.CLIOptions;
import org.apache.metron.dataloads.nonbulk.flatfile.LoadOptions;
import org.apache.metron.dataloads.nonbulk.flatfile.location.Location;
import org.apache.metron.dataloads.nonbulk.flatfile.location.LocationStrategy;
import org.apache.metron.dataloads.nonbulk.flatfile.writer.InvalidWriterOutput;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractLocalImporter<OPTIONS_T extends Enum<OPTIONS_T> & CLIOptions, STATE_T>  implements Importer<OPTIONS_T> {

  @Override
  public void importData( final EnumMap<OPTIONS_T, Optional<Object>> config
                        , final ExtractorHandler handler
                        , final Configuration hadoopConfig
                         ) throws IOException, InvalidWriterOutput {
    validateState(config, handler);
    ThreadLocal<STATE_T> state = createState(config, hadoopConfig, handler);
    boolean quiet = isQuiet(config);
    boolean lineByLine = !handler.getInputFormat().getClass().equals(WholeFileFormat.class);
    List<String> inputs = getInputs(config);
    FileSystem fs = FileSystem.get(hadoopConfig);
    if(!lineByLine) {
      extractWholeFiles(inputs, fs, state, quiet);
    }
    else {
      int batchSize = batchSize(config);
      int numThreads = numThreads(config, handler);
      extractLineByLine(inputs, fs, state, batchSize, numThreads, quiet);
    }
    if(!quiet) {
      System.out.println();
    }
  }

  protected abstract List<String> getInputs(final EnumMap<OPTIONS_T, Optional<Object>> config);
  protected abstract boolean isQuiet(final EnumMap<OPTIONS_T, Optional<Object>> config);
  protected abstract int batchSize(final EnumMap<OPTIONS_T, Optional<Object>> config);
  protected abstract int numThreads(final EnumMap<OPTIONS_T, Optional<Object>> config, ExtractorHandler handler);

  protected abstract void validateState(final EnumMap<OPTIONS_T, Optional<Object>> config
                                       ,final ExtractorHandler handler
                                       );

  protected abstract ThreadLocal<STATE_T> createState( final EnumMap<OPTIONS_T, Optional<Object>> config
                                                     , final Configuration hadoopConfig
                                                     , final ExtractorHandler handler
                                                     );

  protected abstract void extract(STATE_T state
                                 , String line
                                 ) throws IOException;

  protected Location resolveLocation(String input, FileSystem fs) {
    return LocationStrategy.getLocation(input, fs);
  }

  public void extractLineByLine( List<String> inputs
                               , FileSystem fs
                               , ThreadLocal<STATE_T> state
                               , int batchSize
                               , int numThreads
                               , boolean quiet
                               ) throws IOException {
    inputs.stream().map(input -> resolveLocation(input, fs))
                   .forEach( loc -> {
                      final Progress progress = new Progress();
                      if(!quiet) {
                        System.out.println("\nProcessing " + loc.toString());
                      }
                      try (Stream<String> stream = ReaderSpliterator.lineStream(loc.openReader(), batchSize)) {
                        ForkJoinPool forkJoinPool = new ForkJoinPool(numThreads);
                        forkJoinPool.submit(() ->
                          stream.parallel().forEach(input ->  {
                                    try {
                                      extract(state.get(), input);
                                      if (!quiet) {
                                        progress.update();
                                      }
                                    }
                                    catch(IOException e) {
                                      throw new IllegalStateException("Unable to continue: " + e.getMessage(), e);
                                    }
                                  }
                                       )
                               ).get();
                             } catch (Exception e) {
                               throw new IllegalStateException(e.getMessage(), e);
                             }
                                  }
                   );
  }

  public void extractWholeFiles(List<String> inputs, FileSystem fs, ThreadLocal<STATE_T> state, boolean quiet) throws IOException {
    final Progress progress = new Progress();
    final List<Location> locations = getLocationsRecursive(inputs, fs);
    locations.parallelStream().forEach(loc -> {
      try(BufferedReader br = loc.openReader()) {
        String s = br.lines().collect(Collectors.joining());
        extract(state.get(), s);
        if(!quiet) {
          progress.update();
        }
      } catch (IOException e) {
        throw new IllegalStateException("Unable to read " + loc + ": " + e.getMessage(), e);
      }
    });
  }

  protected List<Location> getLocationsRecursive(List<String> inputs, FileSystem fs) throws IOException {
    final List<Location> locations = new ArrayList<>();
    Location.fileVisitor(inputs, loc -> locations.add(loc), fs);
    return locations;
  }

  public static class Progress {
    private int count = 0;
    private String anim= "|/-\\";

    public synchronized void update() {
      int currentCount = count++;
      System.out.print("\rProcessed " + currentCount + " - " + anim.charAt(currentCount % anim.length()));
    }
  }

  protected void assertOption(EnumMap<OPTIONS_T, Optional<Object>> config, OPTIONS_T option) {
    if(!config.containsKey(option)) {
      throw new IllegalStateException("Expected " + option.getOption().getOpt() + " to be set");
    }
  }
}
