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

package org.apache.metron.pcap.utils;

import static org.apache.metron.pcap.PcapHelper.greaterThanOrEqualTo;
import static org.apache.metron.pcap.PcapHelper.lessThanOrEqualTo;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.fs.Path;
import org.apache.metron.pcap.PcapFilenameHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileFilterUtil {

  private static final Logger LOG = LoggerFactory.getLogger(FileFilterUtil.class);

  private FileFilterUtil() {
  }

  /*
   * The trick here is that we need a trailing left endpoint, because we only capture the start of the
   * timeseries kept in the file.
   */
  public static Iterable<String> getPathsInTimeRange(long beginTs, long endTs,
      Iterable<Path> files) {
    Map<Integer, List<Path>> filesByPartition = getFilesByPartition(files);
    List<String> filteredFiles = filterByTimestampLT(beginTs, endTs, filesByPartition);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Including files " + Joiner.on(",").join(filteredFiles));
    }
    return filteredFiles;
  }

  public static Map<Integer, List<Path>> getFilesByPartition(Iterable<Path> files) {
    Iterator<Path> filesIt = files.iterator();
    Map<Integer, List<Path>> filesByPartition = new HashMap<>();
    while (filesIt.hasNext()) {
      Path p = filesIt.next();
      Integer partition = PcapFilenameHelper.getKafkaPartition(p.getName());
      if (!filesByPartition.containsKey(partition)) {
        filesByPartition.put(partition, new ArrayList<>());
      }
      filesByPartition.get(partition).add(p);
    }
    return filesByPartition;
  }

  /**
   * Given a map of partition numbers to files, return a list of files filtered by the supplied
   * beginning and ending timestamps. Includes a left-trailing file.
   *
   * @param filesByPartition list of files mapped to partitions. Incoming files do not need to be
   * sorted as this method will perform a lexicographical sort in normal ascending order.
   * @return filtered list of files, unsorted
   */
  public static List<String> filterByTimestampLT(long beginTs, long endTs,
      Map<Integer, List<Path>> filesByPartition) {
    List<String> filteredFiles = new ArrayList<>();
    for (Integer key : filesByPartition.keySet()) {
      List<Path> paths = filesByPartition.get(key);
      filteredFiles.addAll(filterByTimestampLT(beginTs, endTs, paths));
    }
    return filteredFiles;
  }

  /**
   * Return a list of files filtered by the supplied beginning and ending timestamps. Includes a
   * left-trailing file.
   *
   * @param paths list of files. Incoming files do not need to be sorted as this method will perform
   * a lexicographical sort in normal ascending order.
   * @return filtered list of files
   */
  public static List<String> filterByTimestampLT(long beginTs, long endTs, List<Path> paths) {
    List<String> filteredFiles = new ArrayList<>();

    //noinspection unchecked - hadoop fs uses non-generic Comparable interface
    Collections.sort(paths);
    Iterator<Path> filesIt = paths.iterator();
    Path leftTrailing = filesIt.hasNext() ? filesIt.next() : null;
    if (leftTrailing == null) {
      return filteredFiles;
    }
    boolean first = true;
    Long fileTS = PcapFilenameHelper.getTimestamp(leftTrailing.getName());
    if (fileTS != null
        && greaterThanOrEqualTo(fileTS, beginTs) && lessThanOrEqualTo(fileTS, endTs)) {
      filteredFiles.add(leftTrailing.toString());
      first = false;
    }

    if (first && !filesIt.hasNext()) {
      filteredFiles.add(leftTrailing.toString());
      return filteredFiles;
    }

    while (filesIt.hasNext()) {
      Path p = filesIt.next();
      fileTS = PcapFilenameHelper.getTimestamp(p.getName());
      if (fileTS != null
          && greaterThanOrEqualTo(fileTS, beginTs) && lessThanOrEqualTo(fileTS, endTs)) {
        if (first) {
          filteredFiles.add(leftTrailing.toString());
          first = false;
        }
        filteredFiles.add(p.toString());
      } else {
        leftTrailing = p;
      }
    }

    return filteredFiles;
  }

}
