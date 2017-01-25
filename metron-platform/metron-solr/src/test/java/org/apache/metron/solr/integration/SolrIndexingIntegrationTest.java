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
package org.apache.metron.solr.integration;

import com.google.common.base.Function;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.Configurations;
import org.apache.metron.common.interfaces.FieldNameConverter;
import org.apache.metron.enrichment.integration.utils.SampleUtil;
import org.apache.metron.indexing.integration.IndexingIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.Processor;
import org.apache.metron.integration.ProcessorResult;
import org.apache.metron.integration.ReadinessState;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.solr.integration.components.SolrComponent;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.utils.JSONUtils;

import javax.annotation.Nullable;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class SolrIndexingIntegrationTest extends IndexingIntegrationTest {

  private String collection = "metron";
  private FieldNameConverter fieldNameConverter = fieldName -> fieldName;
  @Override
  public FieldNameConverter getFieldNameConverter() {
    return fieldNameConverter;
  }

  public static class Util {
    private static long lastCpuTimeMillis;
    private static long lastPollTimeMillis;
    private static final DecimalFormat MILLIS_FORMAT = new DecimalFormat("#0.000");
    public static final String NEWLINE = System.getProperty("line.separator");
    private static ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
    public static void checkLoadAverage(String fileName) {
      long now = System.currentTimeMillis();
      long currentCpuMillis = getTotalCpuTimeMillis();
      double loadAvg = calcLoadAveragePercentage(now, currentCpuMillis);
      try (FileWriter fw = new FileWriter(fileName, true)) {
        System.out.println(String.format("Writing to '%s'", fileName));
        dumpStack("Load average percentage is " + loadAvg, fw);
      } catch (IOException e) {
        // Oh well, we tried
      }
      lastCpuTimeMillis = currentCpuMillis;
      lastPollTimeMillis = now;
    }
    private static long getTotalCpuTimeMillis() {
      long total = 0;
      for (long id : threadMxBean.getAllThreadIds()) {
        long cpuTime = threadMxBean.getThreadCpuTime(id);
        if (cpuTime > 0) {
          total += cpuTime;
        }
      }
      // since is in nano-seconds
      long currentCpuMillis = total / 1000000;
      return currentCpuMillis;
    }
    private static double calcLoadAveragePercentage(long now, long currentCpuMillis) {
      long timeDiff = now - lastPollTimeMillis;
      if (timeDiff == 0) {
        timeDiff = 1;
      }
      long cpuDiff = currentCpuMillis - lastCpuTimeMillis;
      double loadAvg = (double) cpuDiff / (double) timeDiff;
      return loadAvg;
    }
    /*
     * Method that dumps the stack trace for all of the threads to a file
     */
    public static void dumpStack(String message, Writer writer) throws IOException {
      ThreadInfo[] threadInfos = threadMxBean.getThreadInfo(threadMxBean.getAllThreadIds(), 0);
      Map<Long, ThreadInfo> threadInfoMap = new HashMap<>();
      for (ThreadInfo threadInfo : threadInfos) {
        threadInfoMap.put(threadInfo.getThreadId(), threadInfo);
      }
      try {
        if (message != null) {
          writer.write(message);
          writer.write(NEWLINE);
        }
        Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();
        writer.write("Dump of " + stacks.size() + " threads at "
                + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z").format(new Date(System.currentTimeMillis()))
                + NEWLINE + NEWLINE);
        for (Map.Entry<Thread, StackTraceElement[]> entry : stacks.entrySet()) {
          Thread thread = entry.getKey();
          writer.write("\"" + thread.getName() + "\" prio=" + thread.getPriority() + " tid=" + thread.getId()
                  + " " + thread.getState() + " " + (thread.isDaemon() ? "deamon" : "worker") + NEWLINE);
          ThreadInfo threadInfo = threadInfoMap.get(thread.getId());
          if (threadInfo != null) {
            writer.write("    native=" + threadInfo.isInNative() + ", suspended=" + threadInfo.isSuspended()
                    + ", block=" + threadInfo.getBlockedCount() + ", wait=" + threadInfo.getWaitedCount()
                    + NEWLINE);
            writer.write("    lock="
                    + threadInfo.getLockName()
                    + " owned by "
                    + threadInfo.getLockOwnerName()
                    + " ("
                    + threadInfo.getLockOwnerId()
                    + "), cpu="
                    + durationMillisToString(threadMxBean.getThreadCpuTime(threadInfo.getThreadId()) / 1000000L)
                    + ", user="
                    + durationMillisToString(threadMxBean.getThreadUserTime(threadInfo.getThreadId()) / 1000000L)
                    + NEWLINE);
          }
          for (StackTraceElement element : entry.getValue()) {
            writer.write("    ");
            String eleStr = element.toString();
            if (eleStr.startsWith("com.mprew")) {
              writer.write(">>  ");
            } else {
              writer.write("    ");
            }
            writer.write(eleStr);
            writer.write(NEWLINE);
          }
          writer.write(NEWLINE);
        }
        writer.write("------------------------------------------------------");
        writer.write(NEWLINE);
        writer.write("Non-daemon threads: ");
        for (Thread thread : stacks.keySet()) {
          if (!thread.isDaemon()) {
            writer.write("\"" + thread.getName() + "\", ");
          }
        }
        writer.write(NEWLINE);
        writer.write("------------------------------------------------------");
        writer.write(NEWLINE);
        writer.write("Blocked threads: ");
        for (Thread thread : stacks.keySet()) {
          if (thread.getState() == Thread.State.BLOCKED) {
            writer.write("\"" + thread.getName() + "\", ");
          }
        }
        writer.write(NEWLINE);
      } finally {
        writer.close();
      }
    }
    private static String durationMillisToString(long millis) {
      return MILLIS_FORMAT.format(millis);
    }
  }

  @Override
  public InMemoryComponent getSearchComponent(final Properties topologyProperties) throws Exception {
    SolrComponent solrComponent = new SolrComponent.Builder()
            .addCollection(collection, "../metron-solr/src/test/resources/solr/conf")
            .withPostStartCallback(new Function<SolrComponent, Void>() {
              @Nullable
              @Override
              public Void apply(@Nullable SolrComponent solrComponent) {
                topologyProperties.setProperty("solr.zk", solrComponent.getZookeeperUrl());
                try {
                  String testZookeeperUrl = topologyProperties.getProperty(ZKServerComponent.ZOOKEEPER_PROPERTY);
                  Configurations configurations = SampleUtil.getSampleConfigs();
                  Map<String, Object> globalConfig = configurations.getGlobalConfig();
                  globalConfig.put("solr.zookeeper", solrComponent.getZookeeperUrl());
                  ConfigurationsUtils.writeGlobalConfigToZookeeper(JSONUtils.INSTANCE.toJSON(globalConfig), testZookeeperUrl);
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }
            })
            .build();
    return solrComponent;
  }

  @Override
  public Processor<List<Map<String, Object>>> getProcessor(final List<byte[]> inputMessages) {
    return new Processor<List<Map<String, Object>>>() {
      List<Map<String, Object>> docs = null;
      List<byte[]> errors = null;
      @Override
      public ReadinessState process(ComponentRunner runner) {
        SolrComponent solrComponent = runner.getComponent("search", SolrComponent.class);
        KafkaComponent kafkaComponent = runner.getComponent("kafka", KafkaComponent.class);
        if (solrComponent.hasCollection(collection)) {
          List<Map<String, Object>> docsFromDisk;
          try {
            docs = solrComponent.getAllIndexedDocs(collection);
            docsFromDisk = readDocsFromDisk(hdfsDir);
            int docsFromDiskSize = docsFromDisk.size();
            if(docsFromDiskSize == 6) {
              Util.dumpStack("Threads", new PrintWriter(System.out));
            }
            System.out.println(docs.size() + " vs " + inputMessages.size() + " vs " + docsFromDisk.size());
          } catch (IOException e) {
            throw new IllegalStateException("Unable to retrieve indexed documents.", e);
          }
          if (docs.size() < inputMessages.size() || docs.size() != docsFromDisk.size()) {
            errors = kafkaComponent.readMessages(Constants.INDEXING_ERROR_TOPIC);
            if(errors.size() > 0){
              return ReadinessState.READY;
            }
            return ReadinessState.NOT_READY;
          } else {
            return ReadinessState.READY;
          }
        } else {
          return ReadinessState.NOT_READY;
        }
      }

      @Override
      public ProcessorResult<List<Map<String, Object>>> getResult() {
        ProcessorResult.Builder<List<Map<String,Object>>> builder = new ProcessorResult.Builder();
        return builder.withResult(docs).withProcessErrors(errors).build();
      }
    };
  }

  @Override
  public void setAdditionalProperties(Properties topologyProperties) {
    topologyProperties.setProperty("writer.class.name", "org.apache.metron.solr.writer.SolrWriter");
  }

  @Override
  public String cleanField(String field) {
    return field.replaceFirst("_[dfils]$", "");
  }
}
