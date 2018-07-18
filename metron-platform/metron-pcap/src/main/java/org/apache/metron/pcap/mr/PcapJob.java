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
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
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
import org.apache.metron.job.Finalizer;
import org.apache.metron.job.JobException;
import org.apache.metron.job.JobStatus;
import org.apache.metron.job.JobStatus.State;
import org.apache.metron.job.Pageable;
import org.apache.metron.job.Statusable;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.PcapPages;
import org.apache.metron.pcap.config.PcapOptions;
import org.apache.metron.pcap.filter.PcapFilter;
import org.apache.metron.pcap.filter.PcapFilterConfigurator;
import org.apache.metron.pcap.filter.PcapFilters;
import org.apache.metron.pcap.utils.FileFilterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encompasses MapReduce job and final writing of Pageable results to specified location.
 * Cleans up MapReduce results from HDFS on completion.
 */
public class PcapJob<T> implements Statusable<Path> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String START_TS_CONF = "start_ts";
  public static final String END_TS_CONF = "end_ts";
  public static final String WIDTH_CONF = "width";
  private static final long THREE_SECONDS = 3000;
  private static final long ONE_SECOND = 1000;
  private Job mrJob; // store a running MR job reference for async status check
  private State jobState; // overall job state, including finalization step
  private Finalizer<Path> finalizer;
  private Map<String, Object> configuration;
  private Pageable<Path> finalResults;
  private Timer timer;
  private long statusInterval; // how often timer thread checks job status.
  private long completeCheckInterval; // how long we sleep between isDone checks in get()

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

  public PcapJob() {
    jobState = State.NOT_RUNNING;
    finalResults = new PcapPages();
    statusInterval = THREE_SECONDS;
    completeCheckInterval = ONE_SECOND;
  }

  /**
   * Primarily for testing.
   *
   * @param interval time in millis
   */
  public void setStatusInterval(long interval) {
    statusInterval = interval;
  }

  /**
   * Primarily for testing.
   *
   * @param interval time in millis
   */
  public void setCompleteCheckInterval(long interval) {
    completeCheckInterval = interval;
  }

  @Override
  public Statusable<Path> submit(Finalizer<Path> finalizer, Map<String, Object> configuration)
      throws JobException {
    this.finalizer = finalizer;
    this.configuration = configuration;
    Optional<String> jobName = Optional.ofNullable(PcapOptions.JOB_NAME.get(configuration, String.class));
    Configuration hadoopConf = PcapOptions.HADOOP_CONF.get(configuration, Configuration.class);
    FileSystem fileSystem = PcapOptions.FILESYSTEM.get(configuration, FileSystem.class);
    Path basePath = PcapOptions.BASE_PATH.getTransformed(configuration, Path.class);
    Path baseInterimResultPath = PcapOptions.BASE_INTERIM_RESULT_PATH.getTransformed(configuration, Path.class);
    long startTime = PcapOptions.START_TIME_NS.get(configuration, Long.class);
    long endTime = PcapOptions.END_TIME_NS.get(configuration, Long.class);
    int numReducers = PcapOptions.NUM_REDUCERS.get(configuration, Integer.class);
    T fields = (T) PcapOptions.FIELDS.get(configuration, Object.class);
    PcapFilterConfigurator<T> filterImpl = PcapOptions.FILTER_IMPL.get(configuration, PcapFilterConfigurator.class);

    try {
      return query(jobName,
          basePath,
          baseInterimResultPath,
          startTime,
          endTime,
          numReducers,
          fields,
          // create a new copy for each job, bad things happen when hadoop config is reused
          new Configuration(hadoopConf),
          fileSystem,
          filterImpl);
    } catch (IOException | InterruptedException | ClassNotFoundException e) {
      throw new JobException("Failed to run pcap query.", e);
    }
  }

  /**
   * Run query asynchronously.
   */
  public Statusable<Path> query(Optional<String> jobName,
      Path basePath,
      Path baseInterimResultPath,
      long beginNS,
      long endNS,
      int numReducers,
      T fields,
      Configuration conf,
      FileSystem fs,
      PcapFilterConfigurator<T> filterImpl)
      throws IOException, ClassNotFoundException, InterruptedException {
    String outputDirName = Joiner.on("_").join(beginNS, endNS, filterImpl.queryToString(fields), UUID.randomUUID().toString());
    if(LOG.isDebugEnabled()) {
      DateFormat format = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG
          , SimpleDateFormat.LONG
      );
      String from = format.format(new Date(Long.divideUnsigned(beginNS, 1000000)));
      String to = format.format(new Date(Long.divideUnsigned(endNS, 1000000)));
      LOG.debug("Executing query {} on timerange from {} to {}", filterImpl.queryToString(fields), from, to);
    }
    Path interimResultPath =  new Path(baseInterimResultPath, outputDirName);
    PcapOptions.INTERIM_RESULT_PATH.put(configuration, interimResultPath);
    mrJob = createJob(jobName
        , basePath
        , interimResultPath
        , beginNS
        , endNS
        , numReducers
        , fields
        , conf
        , fs
        , filterImpl
    );
    mrJob.submit();
    jobState = State.RUNNING;
    startJobStatusTimerThread(statusInterval);
    return this;
  }

  private void startJobStatusTimerThread(long interval) {
    timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        try {
          synchronized (this) {
            if (jobState == State.RUNNING) {
              if (mrJob.isComplete()) {
                switch (mrJob.getStatus().getState()) {
                  case SUCCEEDED:
                    jobState = State.FINALIZING;
                    if (setFinalResults(finalizer, configuration)) {
                      jobState = State.SUCCEEDED;
                    } else {
                      jobState = State.FAILED;
                    }
                    break;
                  case FAILED:
                    jobState = State.FAILED;
                    break;
                  case KILLED:
                    jobState = State.KILLED;
                    break;
                }
                cancel(); // be gone, ye!
              }
            }
          }
        } catch (InterruptedException | IOException e) {
          jobState = State.FAILED;
          cancel();
        }
      }
    }, interval, interval);
  }

  /**
   * Writes results using finalizer. Returns true on success, false otherwise.
   *
   * @param finalizer Writes results.
   * @param configuration Configure the finalizer.
   * @return Returns true on success, false otherwise.
   */
  private boolean setFinalResults(Finalizer<Path> finalizer, Map<String, Object> configuration) {
    boolean success = true;
    Pageable<Path> results = new PcapPages();
    try {
      results = finalizer.finalizeJob(configuration);
    } catch (JobException e) {
      LOG.error("Failed to finalize job.", e);
      success = false;
    }
    synchronized (this) {
      finalResults = results;
    }
    return success;
  }

  /**
   * Creates, but does not submit the job. This is the core MapReduce mrJob.
   */
  public Job createJob(Optional<String> jobName
                      ,Path basePath
                      , Path jobOutputPath
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
    SequenceFileOutputFormat.setOutputPath(job, jobOutputPath);
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
  public JobType getJobType() {
    return JobType.MAP_REDUCE;
  }

  /**
   * Synchronized for mrJob and jobState
   */
  @Override
  public synchronized JobStatus getStatus() throws JobException {
    JobStatus status = new JobStatus();
    if (mrJob == null) {
      status.withPercentComplete(100).withState(State.SUCCEEDED);
    } else {
      try {
        org.apache.hadoop.mapreduce.JobStatus mrJobStatus = mrJob.getStatus();
        status.withJobId(mrJobStatus.getJobID().toString());
        if (jobState == State.SUCCEEDED) {
          status.withPercentComplete(100).withState(State.SUCCEEDED)
              .withDescription("Job complete");
        } else {
          if (mrJob.isComplete()) {
            status.withPercentComplete(100);
            switch (mrJobStatus.getState()) {
              case SUCCEEDED:
                status.withState(State.FINALIZING).withDescription(State.FINALIZING.toString());
                break;
              case FAILED:
                status.withState(State.FAILED).withDescription(State.FAILED.toString());
                break;
              case KILLED:
                status.withState(State.KILLED).withDescription(State.KILLED.toString());
                break;
              default:
                throw new IllegalStateException(
                    "Unknown job state reported as 'complete' by mapreduce framework: "
                        + mrJobStatus.getState());
            }
          } else {
            float mapProg = mrJob.mapProgress();
            float reduceProg = mrJob.reduceProgress();
            float totalProgress = ((mapProg / 2) + (reduceProg / 2)) * 100;
            String description = String
                .format("map: %s%%, reduce: %s%%", mapProg * 100, reduceProg * 100);
            status.withPercentComplete(totalProgress).withState(State.RUNNING)
                .withDescription(description);
          }
        }
      } catch (Exception e) {
        throw new JobException("Error occurred while attempting to retrieve job status.", e);
      }
    }
    return status;
  }

  /**
   * Synchronous call blocks until completion.
   */
  @Override
  public Pageable<Path> get() throws JobException, InterruptedException {
    for (; ; ) {
      JobStatus status = getStatus();
      if (status.getState() == State.SUCCEEDED
          || status.getState() == State.KILLED
          || status.getState() == State.FAILED) {
        return getFinalResults();
      }
      Thread.sleep(completeCheckInterval);
    }
  }

  private synchronized Pageable<Path> getFinalResults() {
    return new PcapPages(finalResults);
  }

  @Override
  public synchronized boolean isDone() {
    return (jobState == State.SUCCEEDED
        || jobState == State.KILLED
        || jobState == State.FAILED);
  }

  @Override
  public void kill() throws JobException {
    try {
      mrJob.killJob();
    } catch (IOException e) {
      throw new JobException("Unable to kill pcap job.", e);
    }
  }

  @Override
  public boolean validate(Map<String, Object> configuration) {
    // default implementation placeholder
    return true;
  }

}
