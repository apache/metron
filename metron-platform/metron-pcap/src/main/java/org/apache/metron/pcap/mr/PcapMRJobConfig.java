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

import java.util.Optional;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.job.writer.ResultsWriter;
import org.apache.metron.pcap.filter.PcapFilterConfigurator;

public class PcapMRJobConfig<T> {

  private Optional<String> jobName;
  private Path basePath;
  private Path baseOutputPath;
  private Path finalOutputPath;
  private ResultsWriter<byte[]> resultsWriter;
  private int numRecordsPerFile;
  private String outputFilePrefix;
  private long beginNS;
  private long endNS;
  private int numReducers;
  private T fields;
  private Configuration conf;
  private FileSystem fs;
  private PcapFilterConfigurator<T> filterImpl;
  private boolean synchronous;

  public Optional<String> getJobName() {
    return jobName;
  }

  public PcapMRJobConfig<T> setJobName(Optional<String> jobName) {
    this.jobName = jobName;
    return this;
  }

  public Path getBasePath() {
    return basePath;
  }

  public PcapMRJobConfig<T> setBasePath(Path basePath) {
    this.basePath = basePath;
    return this;
  }

  public Path getBaseOutputPath() {
    return baseOutputPath;
  }

  public PcapMRJobConfig<T> setBaseOutputPath(Path baseOutputPath) {
    this.baseOutputPath = baseOutputPath;
    return this;
  }

  public long getBeginNS() {
    return beginNS;
  }

  public PcapMRJobConfig<T> setBeginNS(long beginNS) {
    this.beginNS = beginNS;
    return this;
  }

  public long getEndNS() {
    return endNS;
  }

  public PcapMRJobConfig<T> setEndNS(long endNS) {
    this.endNS = endNS;
    return this;
  }

  public int getNumReducers() {
    return numReducers;
  }

  public PcapMRJobConfig<T> setNumReducers(int numReducers) {
    this.numReducers = numReducers;
    return this;
  }

  public T getFields() {
    return fields;
  }

  public PcapMRJobConfig<T> setFields(T fields) {
    this.fields = fields;
    return this;
  }

  public Configuration getConf() {
    return conf;
  }

  public PcapMRJobConfig<T> setConf(Configuration conf) {
    this.conf = conf;
    return this;
  }

  public FileSystem getFs() {
    return fs;
  }

  public PcapMRJobConfig<T> setFs(FileSystem fs) {
    this.fs = fs;
    return this;
  }

  public PcapFilterConfigurator<T> getFilterImpl() {
    return filterImpl;
  }

  public PcapMRJobConfig<T> setFilterImpl(PcapFilterConfigurator<T> filterImpl) {
    this.filterImpl = filterImpl;
    return this;
  }

  public boolean isSynchronous() {
    return synchronous;
  }

  public PcapMRJobConfig<T> setSynchronous(boolean synchronous) {
    this.synchronous = synchronous;
    return this;
  }

  public Path getFinalOutputPath() {
    return finalOutputPath;
  }

  public PcapMRJobConfig<T> setFinalOutputPath(Path finalOutputPath) {
    this.finalOutputPath = finalOutputPath;
    return this;
  }

  public ResultsWriter<byte[]> getResultsWriter() {
    return resultsWriter;
  }

  public PcapMRJobConfig<T> setResultsWriter(ResultsWriter<byte[]> resultsWriter) {
    this.resultsWriter = resultsWriter;
    return this;
  }

  public int getNumRecordsPerFile() {
    return numRecordsPerFile;
  }

  public PcapMRJobConfig<T> setNumRecordsPerFile(int numRecordsPerFile) {
    this.numRecordsPerFile = numRecordsPerFile;
    return this;
  }

  public String getOutputFilePrefix() {
    return outputFilePrefix;
  }

  public PcapMRJobConfig<T> setOutputFilePrefix(String outputFilePrefix) {
    this.outputFilePrefix = outputFilePrefix;
    return this;
  }

}
