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

import static org.apache.metron.pcap.PcapHelper.greaterThanOrEqualTo;
import static org.apache.metron.pcap.PcapHelper.lessThanOrEqualTo;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.apache.metron.job.JobStatus;
import org.apache.metron.job.JobStatus.State;
import org.apache.metron.job.Pageable;
import org.apache.metron.job.Statusable;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.pcap.PcapFiles;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.filter.PcapFilter;
import org.apache.metron.pcap.filter.PcapFilterConfigurator;
import org.apache.metron.pcap.filter.PcapFilters;
import org.apache.metron.pcap.utils.FileFilterUtil;
import org.apache.metron.pcap.writer.ResultsWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PcapJob implements Statusable {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String START_TS_CONF = "start_ts";
  public static final String END_TS_CONF = "end_ts";
  public static final String WIDTH_CONF = "width";
  private Job job; // store a running MR job reference for async status check
  private Path outputPath;

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
      if (start == null) {
        initialize();
      }
      long x = longWritable.get();
      int ret = (int)Long.divideUnsigned(x - start, width);
      if (ret > numPartitions) {
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
        } catch (Exception e) {
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
      for (BytesWritable value : values) {
        context.write(key, value);
      }
    }
  }

  /**
   * Run query synchronously.
   */
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
    Statusable statusable = query(Optional.empty(), basePath, baseOutputPath, beginNS, endNS, numReducers, fields,
        conf,
        fs, filterImpl, true);
    JobStatus jobStatus = statusable.getStatus();
    if (jobStatus.getState() == State.SUCCEEDED) {
      Path resultPath = jobStatus.getResultPath();
      return readResults(resultPath, conf, fs);
    } else {
      throw new RuntimeException(
          "Unable to complete query due to errors.  Please check logs for full errors.");
    }
  }

  /**
   * Run query sync OR async based on flag. Async mode allows the client to check the returned
   * statusable object for status details.
   */
  public <T> Statusable query(Optional<String> jobName,
      Path basePath,
      Path baseOutputPath,
      long beginNS,
      long endNS,
      int numReducers,
      T fields,
      Configuration conf,
      FileSystem fs,
      PcapFilterConfigurator<T> filterImpl,
      boolean sync)
      throws IOException, ClassNotFoundException, InterruptedException {
    String outputDirName = Joiner.on("_").join(beginNS, endNS, filterImpl.queryToString(fields), UUID.randomUUID().toString());
    if(LOG.isDebugEnabled()) {
      DateFormat format = SimpleDateFormat.getDateTimeInstance( SimpleDateFormat.LONG
          , SimpleDateFormat.LONG
      );
      String from = format.format(new Date(Long.divideUnsigned(beginNS, 1000000)));
      String to = format.format(new Date(Long.divideUnsigned(endNS, 1000000)));
      LOG.debug("Executing query {} on timerange from {} to {}", filterImpl.queryToString(fields), from, to);
    }
    outputPath =  new Path(baseOutputPath, outputDirName);
    job = createJob(jobName
        , basePath
        , outputPath
        , beginNS
        , endNS
        , numReducers
        , fields
        , conf
        , fs
        , filterImpl
    );
    if (sync) {
      job.waitForCompletion(true);
    } else {
      job.submit();
    }
    return this;
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
    if (files.size() == 0) {
      LOG.info("No files to process with specified date range.");
    } else {
      LOG.debug("Output path={}", outputPath);
      Collections.sort(files, (o1, o2) -> o1.getName().compareTo(o2.getName()));
    }
    return new SequenceFileIterable(files, config);
  }

  public Pageable<Path> writeResults(SequenceFileIterable results, ResultsWriter resultsWriter,
      Path outPath, int recPerFile, String prefix) throws IOException {
    List<Path> outFiles = new ArrayList<>();
    try {
      Iterable<List<byte[]>> partitions = Iterables.partition(results, recPerFile);
      int part = 1;
      if (partitions.iterator().hasNext()) {
        for (List<byte[]> data : partitions) {
          String outFileName = String.format("%s/pcap-data-%s+%04d.pcap", outPath, prefix, part++);
          if (data.size() > 0) {
            resultsWriter.write(new Configuration(), data, outFileName);
            outFiles.add(new Path(outFileName));
          }
        }
      } else {
        LOG.info("No results returned.");
      }
    } finally {
      try {
        results.cleanup();
      } catch (IOException e) {
        LOG.warn("Unable to cleanup files in HDFS", e);
      }
    }
    return new PcapFiles(outFiles);
  }

  /**
   * Creates, but does not submit the job.
   */
  public <T> Job createJob(Optional<String> jobName
                      ,Path basePath
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
    jobName.ifPresent(job::setJobName);
    job.setJarByClass(PcapJob.class);
    job.setMapperClass(PcapJob.PcapMapper.class);
    job.setMapOutputKeyClass(LongWritable.class);
    job.setMapOutputValueClass(BytesWritable.class);
    job.setNumReduceTasks(numReducers);
    job.setReducerClass(PcapReducer.class);
    job.setPartitionerClass(PcapPartitioner.class);
    job.setOutputKeyClass(LongWritable.class);
    job.setOutputValueClass(BytesWritable.class);
    Iterable<String> filteredPaths = FileFilterUtil.getPathsInTimeRange(beginNS, endNS, listFiles(fs, basePath));
    String inputPaths = Joiner.on(',').join(filteredPaths);
    if (StringUtils.isEmpty(inputPaths)) {
      return null;
    }
    SequenceFileInputFormat.addInputPaths(job, inputPaths);
    job.setInputFormatClass(SequenceFileInputFormat.class);
    job.setOutputFormatClass(SequenceFileOutputFormat.class);
    SequenceFileOutputFormat.setOutputPath(job, outputPath);
    return job;
  }

  public static long findWidth(long start, long end, int numReducers) {
    return Long.divideUnsigned(end - start, numReducers) + 1;
  }

  protected Iterable<Path> listFiles(FileSystem fs, Path basePath) throws IOException {
    List<Path> ret = new ArrayList<>();
    RemoteIterator<LocatedFileStatus> filesIt = fs.listFiles(basePath, true);
    while (filesIt.hasNext()) {
      ret.add(filesIt.next().getPath());
    }
    return ret;
  }

  @Override
  public JobStatus getStatus() {
    // Note: this method is only reading state from the underlying job, so locking not needed
    JobStatus status = new JobStatus().withResultPath(outputPath);
    if (job == null) {
      status.withPercentComplete(100).withState(State.SUCCEEDED);
    } else {
      try {
        status.withJobId(job.getStatus().getJobID().toString());
        if (job.isComplete()) {
          status.withPercentComplete(100);
          switch (job.getStatus().getState()) {
            case SUCCEEDED:
              status.withState(State.SUCCEEDED).withDescription(State.SUCCEEDED.toString());
              break;
            case FAILED:
              status.withState(State.FAILED);
              break;
            case KILLED:
              status.withState(State.KILLED);
              break;
          }
        } else {
          float mapProg = job.mapProgress();
          float reduceProg = job.reduceProgress();
          float totalProgress = ((mapProg / 2) + (reduceProg / 2)) * 100;
          String description = String.format("map: %s%%, reduce: %s%%", mapProg * 100, reduceProg * 100);
          status.withPercentComplete(totalProgress).withState(State.RUNNING)
              .withDescription(description);
        }
      } catch (Exception e) {
        throw new RuntimeException("Error occurred while attempting to retrieve job status.", e);
      }
    }
    return status;
  }

  @Override
  public boolean isDone() {
    // Note: this method is only reading state from the underlying job, so locking not needed
    try {
      return job.isComplete();
    } catch (Exception e) {
      throw new RuntimeException("Error occurred while attempting to retrieve job status.", e);
    }
  }

  @Override
  public void kill() throws IOException {
    job.killJob();
  }

  @Override
  public boolean validate(Map<String, Object> configuration) {
    // default implementation placeholder
    return true;
  }

}
