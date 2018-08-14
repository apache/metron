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
import static org.apache.metron.pcap.config.PcapGlobalDefaults.NUM_REDUCERS_DEFAULT;

import com.google.common.base.Joiner;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
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
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.job.Finalizer;
import org.apache.metron.job.JobException;
import org.apache.metron.job.JobStatus;
import org.apache.metron.job.JobStatus.State;
import org.apache.metron.job.Pageable;
import org.apache.metron.job.Statusable;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.PcapPages;
import org.apache.metron.pcap.config.PcapGlobalDefaults;
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
  private final OutputDirFormatter outputDirFormatter;
  private volatile Job mrJob; // store a running MR job reference for async status check
  private volatile JobStatus jobStatus; // overall job status, including finalization step
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
    jobStatus = new JobStatus();
    finalResults = new PcapPages();
    outputDirFormatter = new OutputDirFormatter();
    timer = new Timer();
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
    Path baseInterimResultPath = PcapOptions.BASE_INTERIM_RESULT_PATH
        .getTransformedOrDefault(configuration, Path.class,
            new Path(PcapGlobalDefaults.BASE_INTERIM_RESULT_PATH_DEFAULT));
    long startTimeNs;
    if (configuration.containsKey(PcapOptions.START_TIME_NS.getKey())) {
      startTimeNs = PcapOptions.START_TIME_NS.getOrDefault(configuration, Long.class, 0L);
    } else {
      startTimeNs = TimestampConverters.MILLISECONDS.toNanoseconds(PcapOptions.START_TIME_MS.getOrDefault(configuration, Long.class, 0L));
    }
    long endTimeNs;
    if (configuration.containsKey(PcapOptions.END_TIME_NS.getKey())) {
      endTimeNs = PcapOptions.END_TIME_NS.getOrDefault(configuration, Long.class, TimestampConverters.MILLISECONDS.toNanoseconds(System.currentTimeMillis()));
    } else {
      endTimeNs = TimestampConverters.MILLISECONDS.toNanoseconds(PcapOptions.END_TIME_MS.getOrDefault(configuration, Long.class, System.currentTimeMillis()));
    }
    int numReducers = PcapOptions.NUM_REDUCERS.getOrDefault(configuration, Integer.class, NUM_REDUCERS_DEFAULT);
    T fields = (T) PcapOptions.FIELDS.get(configuration, Object.class);
    PcapFilterConfigurator<T> filterImpl = PcapOptions.FILTER_IMPL.get(configuration, PcapFilterConfigurator.class);

    try {
      Statusable<Path> statusable = query(jobName,
          basePath,
          baseInterimResultPath,
          startTimeNs,
          endTimeNs,
          numReducers,
          fields,
          // create a new copy for each job, bad things happen when hadoop config is reused
          new Configuration(hadoopConf),
          fileSystem,
          filterImpl);
      PcapOptions.JOB_ID.put(configuration, statusable.getStatus().getJobId());
      return statusable;
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
    String outputDirName = outputDirFormatter.format(beginNS, endNS, filterImpl.queryToString(fields));
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
    if (mrJob == null) {
      LOG.info("No files to process with specified date range.");
      try {
        setFinalResults(input -> new PcapPages(), configuration);
        jobStatus.withState(State.SUCCEEDED).withDescription("No results in specified date range.")
            .withPercentComplete(100.0);
      } catch (JobException e) {
        // This should not cause an error as we simply set results to an empty result set.
        jobStatus.withState(State.FAILED).withDescription("Unable to finalize empty job.")
            .withFailureException(e);
      }
      return this;
    }
    mrJob.submit();
    jobStatus.withState(State.SUBMITTED).withDescription("Job submitted").withJobId(mrJob.getJobID().toString());
    startJobStatusTimerThread(statusInterval);
    return this;
  }

  private void startJobStatusTimerThread(long interval) {
    getTimer().scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        if (!updateStatus()) {
          cancel(); // be gone, ye!
        }
      }
    }, interval, interval);
  }

  public void setTimer(Timer timer) {
    this.timer = timer;
  }

  private Timer getTimer() {
    return timer;
  }

  /**
   * Update job status info. Will finalize job when underlying MR job completes.
   *
   * @return true if should continue updating status, false otherwise.
   */
  private synchronized boolean updateStatus() {
    try {
      org.apache.hadoop.mapreduce.JobStatus mrJobStatus = mrJob.getStatus();
      org.apache.hadoop.mapreduce.JobStatus.State mrJobState = mrJob.getStatus().getState();
      if (mrJob.isComplete()) {
        jobStatus.withPercentComplete(100.0);
        switch (mrJobState) {
          case SUCCEEDED:
            jobStatus.withState(State.FINALIZING).withDescription("Finalizing job.");
            try {
              setFinalResults(finalizer, configuration);
              jobStatus.withState(State.SUCCEEDED).withDescription("Job completed.");
            } catch (JobException je) {
              jobStatus.withState(State.FAILED).withDescription("Job finalize failed.")
                  .withFailureException(je);
            }
            break;
          case FAILED:
            jobStatus.withState(State.FAILED).withDescription(mrJob.getStatus().getFailureInfo());
            break;
          case KILLED:
            jobStatus.withState(State.KILLED).withDescription(mrJob.getStatus().getFailureInfo());
            break;
        }
        return false;
      } else {
        float mapProg = mrJob.mapProgress();
        float reduceProg = mrJob.reduceProgress();
        float totalProgress = ((mapProg / 2) + (reduceProg / 2)) * 100;
        String description = String
            .format("map: %s%%, reduce: %s%%", mapProg * 100, reduceProg * 100);
        jobStatus.withPercentComplete(totalProgress).withState(State.RUNNING)
            .withDescription(description);
      }
    } catch (InterruptedException | IOException e) {
      jobStatus.withPercentComplete(100.0).withState(State.FAILED).withFailureException(e);
      return false;
    }
    return true;
  }

  /**
   * Writes results using finalizer. Returns true on success, false otherwise. If no results
   * to finalize, returns empty Pageable.
   *
   * @param finalizer Writes results.
   * @param configuration Configure the finalizer.
   * @return Returns true on success, false otherwise.
   */
  private void setFinalResults(Finalizer<Path> finalizer, Map<String, Object> configuration)
      throws JobException {
    Pageable<Path> results = finalizer.finalizeJob(configuration);
    if (results == null) {
      results = new PcapPages();
    }
    synchronized (this) {
      finalResults = results;
    }
  }

  /**
   * Creates, but does not submit the job. This is the core MapReduce mrJob. Empty input path
   * results in a null to be returned instead of creating the job.
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
    Iterable<String> filteredPaths = FileFilterUtil.getPathsInTimeRange(beginNS, endNS, listFiles(fs, basePath));
    String inputPaths = Joiner.on(',').join(filteredPaths);
    if (StringUtils.isEmpty(inputPaths)) {
      return null;
    }
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

  @Override
  public synchronized JobStatus getStatus() throws JobException {
    return new JobStatus(jobStatus);
  }

  protected void setJobStatus(JobStatus jobStatus) {
    this.jobStatus = jobStatus;
  }

  protected void setMrJob(Job mrJob) {
    this.mrJob = mrJob;
  }

  /**
   * Synchronous call blocks until completion.
   */
  @Override
  public Pageable<Path> get() throws JobException, InterruptedException {
    if (PcapOptions.PRINT_JOB_STATUS.getOrDefault(configuration, Boolean.class, false) && mrJob != null) {
      try {
        mrJob.monitorAndPrintJob();
      } catch (IOException e) {
        throw new JobException("Could not monitor job status", e);
      }
    }
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
    State jobState = jobStatus.getState();
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

  @Override
  public Map<String, Object> getConfiguration() {
    return new HashMap<>(this.configuration);
  }

  protected void setConfiguration(Map<String, Object> configuration) {
    this.configuration = configuration;
  }
}
