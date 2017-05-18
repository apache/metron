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

package org.apache.metron.pcap.mr;

import com.google.common.base.Joiner;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.metron.common.hadoop.SequenceFileIterable;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.pcap.PcapFilenameHelper;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.filter.PcapFilter;
import org.apache.metron.pcap.filter.PcapFilterConfigurator;
import org.apache.metron.pcap.filter.PcapFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PcapJob {
  private static final Logger LOG = LoggerFactory.getLogger(PcapJob.class);
  public static final String START_TS_CONF = "start_ts";
  public static final String END_TS_CONF = "end_ts";
  public static final String WIDTH_CONF = "width";

  public static enum PCAP_COUNTER {
    MALFORMED_PACKET_COUNT
  }

  public static class PcapPartitioner extends Partitioner<LongWritable, BytesWritable> implements Configurable {
    private Configuration configuration;
    Long start = null;
    Long end = null;
    Long width = null;
    @Override
    public int getPartition(LongWritable longWritable, BytesWritable bytesWritable, int numPartitions) {
      if(start == null) {
        initialize();
      }
      long x = longWritable.get();
      int ret = (int)Long.divideUnsigned(x - start, width);
      if(ret > numPartitions) {
        throw new IllegalArgumentException(String.format("Bad partition: key=%s, width=%d, partition=%d, numPartitions=%d"
                , Long.toUnsignedString(x), width, ret, numPartitions)
            );
      }
      return ret;
    }

    private void initialize() {
      start = Long.parseUnsignedLong(configuration.get(START_TS_CONF));
      end = Long.parseUnsignedLong(configuration.get(END_TS_CONF));
      width = Long.parseLong(configuration.get(WIDTH_CONF));
    }

    @Override
    public void setConf(Configuration conf) {
      this.configuration = conf;
    }

    @Override
    public Configuration getConf() {
      return configuration;
    }
  }
  public static class PcapMapper extends Mapper<LongWritable, BytesWritable, LongWritable, BytesWritable> {

    PcapFilter filter;
    long start;
    long end;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
      super.setup(context);
      filter = PcapFilters.valueOf(context.getConfiguration().get(PcapFilterConfigurator.PCAP_FILTER_NAME_CONF)).create();
      filter.configure(context.getConfiguration());
      start = Long.parseUnsignedLong(context.getConfiguration().get(START_TS_CONF));
      end = Long.parseUnsignedLong(context.getConfiguration().get(END_TS_CONF));
    }

    @Override
    protected void map(LongWritable key, BytesWritable value, Context context) throws IOException, InterruptedException {
      if (greaterThanOrEqualTo(key.get(), start) && lessThanOrEqualTo(key.get(), end)) {
        // It is assumed that the passed BytesWritable value is always a *single* PacketInfo object. Passing more than 1
        // object will result in the whole set being passed through if any pass the filter. We cannot serialize PacketInfo
        // objects back to byte arrays, otherwise we could support more than one packet.
        // Note: short-circuit findAny() func on stream
        List<PacketInfo> packetInfos;
        try {
          packetInfos = PcapHelper.toPacketInfo(value.copyBytes());
        } catch(Exception e) {
          // toPacketInfo is throwing RuntimeExceptions. Attempt to catch and count errors with malformed packets
          context.getCounter(PCAP_COUNTER.MALFORMED_PACKET_COUNT).increment(1);
          return;
        }
        boolean send = filteredPacketInfo(packetInfos).findAny().isPresent();
        if (send) {
          context.write(key, value);
        }
      }
    }

    private Stream<PacketInfo> filteredPacketInfo(List<PacketInfo> packetInfos) throws IOException {
      return packetInfos.stream().filter(filter);
    }
  }

  public static class PcapReducer extends Reducer<LongWritable, BytesWritable, LongWritable, BytesWritable> {
    @Override
    protected void reduce(LongWritable key, Iterable<BytesWritable> values, Context context) throws IOException, InterruptedException {
      for(BytesWritable value : values) {
        context.write(key, value);
      }
    }
  }

  /**
   * Returns a lazily-read Iterable over a set of sequence files
   */
  private SequenceFileIterable readResults(Path outputPath, Configuration config, FileSystem fs) throws IOException {
    List<Path> files = new ArrayList<>();
    for (RemoteIterator<LocatedFileStatus> it = fs.listFiles(outputPath, false); it.hasNext(); ) {
      Path p = it.next().getPath();
      if (p.getName().equals("_SUCCESS")) {
        fs.delete(p, false);
        continue;
      }
      files.add(p);
    }
    LOG.debug("Output path={}", outputPath);
    Collections.sort(files, (o1,o2) -> o1.getName().compareTo(o2.getName()));
    return new SequenceFileIterable(files, config);
  }

  public <T> SequenceFileIterable query(Path basePath
                            , Path baseOutputPath
                            , long beginNS
                            , long endNS
                            , int numReducers
                            , T fields
                            , Configuration conf
                            , FileSystem fs
                            , PcapFilterConfigurator<T> filterImpl
                            ) throws IOException, ClassNotFoundException, InterruptedException {
    String fileName = Joiner.on("_").join(beginNS, endNS, filterImpl.queryToString(fields), UUID.randomUUID().toString());
    if(LOG.isDebugEnabled()) {
      DateFormat format = SimpleDateFormat.getDateTimeInstance( SimpleDateFormat.LONG
                                                              , SimpleDateFormat.LONG
                                                              );
      String from = format.format(new Date(Long.divideUnsigned(beginNS, 1000000)));
      String to = format.format(new Date(Long.divideUnsigned(endNS, 1000000)));
      LOG.debug("Executing query " + filterImpl.queryToString(fields) + " on timerange " + from + " to " + to);
    }
    Path outputPath =  new Path(baseOutputPath, fileName);
    Job job = createJob( basePath
                       , outputPath
                       , beginNS
                       , endNS
                       , numReducers
                       , fields
                       , conf
                       , fs
                       , filterImpl
                       );
    if (job == null) {
      LOG.info("No files to process with specified date range.");
      return new SequenceFileIterable(new ArrayList<>(), conf);
    }
    boolean completed = job.waitForCompletion(true);
    if(completed) {
      return readResults(outputPath, conf, fs);
    }
    else {
      throw new RuntimeException("Unable to complete query due to errors.  Please check logs for full errors.");
    }
  }

  public static long findWidth(long start, long end, int numReducers) {
    return Long.divideUnsigned(end - start, numReducers) + 1;
  }

  public <T> Job createJob( Path basePath
                      , Path outputPath
                      , long beginNS
                      , long endNS
                      , int numReducers
                      , T fields
                      , Configuration conf
                      , FileSystem fs
                      , PcapFilterConfigurator<T> filterImpl
                      ) throws IOException
  {
    conf.set(START_TS_CONF, Long.toUnsignedString(beginNS));
    conf.set(END_TS_CONF, Long.toUnsignedString(endNS));
    conf.set(WIDTH_CONF, "" + findWidth(beginNS, endNS, numReducers));
    filterImpl.addToConfig(fields, conf);
    Job job = Job.getInstance(conf);
    job.setJarByClass(PcapJob.class);
    job.setMapperClass(PcapJob.PcapMapper.class);
    job.setMapOutputKeyClass(LongWritable.class);
    job.setMapOutputValueClass(BytesWritable.class);
    job.setNumReduceTasks(numReducers);
    job.setReducerClass(PcapReducer.class);
    job.setPartitionerClass(PcapPartitioner.class);
    job.setOutputKeyClass(LongWritable.class);
    job.setOutputValueClass(BytesWritable.class);
    String inputPaths = Joiner.on(',').join(getPathsInTimeRange(fs, basePath, beginNS, endNS));
    if (StringUtils.isEmpty(inputPaths)) {
      return null;
    }
    SequenceFileInputFormat.addInputPaths(job, inputPaths);
    job.setInputFormatClass(SequenceFileInputFormat.class);
    job.setOutputFormatClass(SequenceFileOutputFormat.class);
    SequenceFileOutputFormat.setOutputPath(job, outputPath);
    return job;
  }

  public Iterable<String> getPathsInTimeRange(FileSystem fs, Path basePath, long beginTs, long endTs)
      throws IOException {
    Map<Integer, List<Path>> filesByPartition = getFilesByPartition(listFiles(fs, basePath));

    /*
     * The trick here is that we need a trailing left endpoint, because we only capture the start of the
     * timeseries kept in the file.
     */
    List<String> filteredFiles = filterByTimestampLT(beginTs, endTs, filesByPartition);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Including files " + Joiner.on(",").join(filteredFiles));
    }
    return filteredFiles;
  }

  private Map<Integer, List<Path>> getFilesByPartition(Iterable<Path> files) {
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

  protected Iterable<Path> listFiles(FileSystem fs, Path basePath) throws IOException {
    List<Path> ret = new ArrayList<>();
    RemoteIterator<LocatedFileStatus> filesIt = fs.listFiles(basePath, true);
    while (filesIt.hasNext()) {
      ret.add(filesIt.next().getPath());
    }
    return ret;
  }

  /**
   * Given a map of partition numbers to files, return a list of files filtered by the supplied
   * beginning and ending timestamps. Includes a left-trailing file.
   *
   * @param filesByPartition list of files mapped to partitions. Incoming files do not need to be
   * sorted as this method will perform a lexicographical sort in normal ascending order.
   * @return filtered list of files, unsorted
   */
  private List<String> filterByTimestampLT(long beginTs, long endTs,
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
  private List<String> filterByTimestampLT(long beginTs, long endTs, List<Path> paths) {
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

  private static boolean greaterThanOrEqualTo(long a, long b) {
    return Long.compareUnsigned(a, b) >= 0;
  }

  private static boolean lessThanOrEqualTo(long fileTS, long endTs) {
    return Long.compareUnsigned(fileTS, endTs) <= 0;
  }

}
