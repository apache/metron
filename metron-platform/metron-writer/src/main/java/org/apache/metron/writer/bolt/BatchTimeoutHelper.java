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
package org.apache.metron.writer.bolt;

import org.apache.storm.Config;
import org.apache.storm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Routines to help figure out the effective batchTimeout(s), using information from
 * multiple configuration sources, topology.message.timeout.secs, and batchTimeoutDivisor,
 * and use it to calculate maxBatchTimeout and appropriate topology.tick.tuple.freq.secs.
 *
 * These methods cause no side effects outside of setting the internal member variables.
 * "base" config are from defaults and storm.yaml (subordinate to Component config settings)
 * "cli" config are from CLI arguments (superior to Component config settings)
 * 0 or Integer.MAX_VALUE means disabled, except in batchTimeout values, where
 * 0 means use the default.
 *
 * These lookups are fairly expensive, and changing the result values currently require
 * a restart of the Storm topology anyway, so this implementation caches its results for
 * re-reading.  If you want different results, you'll need a new instance or a restart.
 */
public class BatchTimeoutHelper {

  private static final Logger LOG = LoggerFactory
          .getLogger(BulkMessageWriterBolt.class);
  private boolean initialized = false;
  private Supplier<List<Integer>> listAllConfiguredTimeouts;
  protected int batchTimeoutDivisor;
  protected int baseMessageTimeoutSecs;
  protected int cliMessageTimeoutSecs;
  protected int baseTickTupleFreqSecs;
  protected int cliTickTupleFreqSecs;
  protected int effectiveMessageTimeoutSecs;
  protected int maxBatchTimeoutAllowedSecs; //derived from MessageTimeoutSecs value
  protected int minBatchTimeoutRequestedSecs; //min of all sensorType configured batchTimeout requests
  protected int recommendedTickIntervalSecs;  //the answer

  public BatchTimeoutHelper( Supplier<List<Integer>> listAllConfiguredTimeouts
          , int batchTimeoutDivisor
          )
  {
    // The two arguments to the constructor are information only available at the Bolt object (batchTimeoutDivisor)
    // and WriterConfiguration object (listAllConfiguredTimeouts).
    this.batchTimeoutDivisor = batchTimeoutDivisor;
    this.listAllConfiguredTimeouts = listAllConfiguredTimeouts;
    // Reads and calculations are deferred until first call, then frozen for the duration of this BatchTimeoutHelper instance.
  }

  private synchronized void init() {
    if (initialized) return;
    readGlobalTimeoutConfigs();
    calcMaxBatchTimeoutAllowed();
    readMinBatchTimeoutRequested();
    calcRecommendedTickInterval();
    initialized = true;
  }

  //modified from Utils.readStormConfig()
  private Map readStormConfigWithoutCLI() {
    Map ret = Utils.readDefaultConfig();
    String confFile = System.getProperty("storm.conf.file");
    Map storm;
    if (confFile == null || confFile.equals("")) {
      storm = Utils.findAndReadConfigFile("storm.yaml", false);
    } else {
      storm = Utils.findAndReadConfigFile(confFile, true);
    }
    ret.putAll(storm);
    return ret;
  }

  private void readGlobalTimeoutConfigs() {
    Map stormConf = readStormConfigWithoutCLI();
    Map cliConf   = Utils.readCommandLineOpts();
    //parameter TOPOLOGY_MESSAGE_TIMEOUT_SECS is declared @isInteger and @NotNull in storm-core (org.apache.storm.Config)
    baseMessageTimeoutSecs =
            (Integer) stormConf.getOrDefault(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS, 0);
    cliMessageTimeoutSecs =
            (Integer)   cliConf.getOrDefault(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS, 0);
    //parameter TOPOLOGY_TICK_TUPLE_FREQ_SECS is only declared @isInteger, and may in fact return null
    Object scratch;
    scratch = stormConf.getOrDefault(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, 0);
    baseTickTupleFreqSecs = (scratch == null) ? 0 : (Integer) scratch;
    scratch =   cliConf.getOrDefault(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, 0);
    cliTickTupleFreqSecs  = (scratch == null) ? 0 : (Integer) scratch;
  }

  private void calcMaxBatchTimeoutAllowed() {
    // The max batchTimeout allowed also becomes the max batchTimeout.
    effectiveMessageTimeoutSecs = (cliMessageTimeoutSecs == 0 ? baseMessageTimeoutSecs : cliMessageTimeoutSecs);
    if (effectiveMessageTimeoutSecs == 0) {
      LOG.info("topology.message.timeout.secs is disabled in both Storm config and CLI.  Allowing unlimited batchTimeouts.");
      maxBatchTimeoutAllowedSecs = Integer.MAX_VALUE;
    }
    else {
      //Recommended value for safe batchTimeout is 1/2 * TOPOLOGY_MESSAGE_TIMEOUT_SECS.
      //We further divide this by batchTimeoutDivisor for the particular Writer Bolt we are in,
      //and subtract a delta of 1 second for surety (as well as rounding down).
      //So if everything is defaulted, maxBatchTimeoutAllowedSecs is 14.
      maxBatchTimeoutAllowedSecs = effectiveMessageTimeoutSecs / 2 / batchTimeoutDivisor - 1;
      if (maxBatchTimeoutAllowedSecs <= 0) { //disallowed, and shouldn't happen with reasonable configs
        maxBatchTimeoutAllowedSecs = 1;
      }
    }
  }

  /**
   * @return the max batchTimeout allowed, in seconds
   * Guaranteed positive number.
   */
  public int getMaxBatchTimeout() {
    if (!initialized) {this.init();}
    return maxBatchTimeoutAllowedSecs;
  }

  private void readMinBatchTimeoutRequested() {
    // The knowledge of how to list the currently configured batchTimeouts
    // is delegated to the WriterConfiguration for the bolt that called us.
    List<Integer> configuredTimeouts = listAllConfiguredTimeouts.get();

    // Discard non-positive values, which mean "use default"
    int minval = Integer.MAX_VALUE;
    for (int k : configuredTimeouts) {
      if (k < minval && k > 0) minval = k;
    }
    minBatchTimeoutRequestedSecs = minval;
  }

  private void calcRecommendedTickInterval() {
    recommendedTickIntervalSecs = Integer.min(minBatchTimeoutRequestedSecs, maxBatchTimeoutAllowedSecs);
    //If needed, we can +=1 to assure triggering on each cycle, but this shouldn't be necessary.
    //Note that this strategy means that sensors with batchTimeout requested less frequently
    //may now have latency of "their requested batchTimeout" + "this recommended tick interval",
    //in the worst case.
  }

  /**
   * @return the recommended TickInterval to request, in seconds
   * Guaranteed positive number.
   */
  public int getRecommendedTickInterval() {
    if (!initialized) {this.init();}
    // Remember that parameter settings in the CLI override parameter settings set by the Storm component.
    // We shouldn't have to deal with this in the Metron environment, but just in case,
    // warn if our recommended value will be overridden by cliTickTupleFreqSecs.
    if (cliTickTupleFreqSecs > 0 && cliTickTupleFreqSecs > recommendedTickIntervalSecs) {
      LOG.warn("Parameter '" + Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS + "' has been forced to value '" +
              Integer.toString(cliTickTupleFreqSecs) + "' via CLI configuration.  This will override the desired " +
              "setting of '" + Integer.toString(recommendedTickIntervalSecs) +
              "' and may lead to delayed batch flushing.");
    }
    if (cliTickTupleFreqSecs > 0 && cliTickTupleFreqSecs < recommendedTickIntervalSecs) {
      LOG.info("Parameter '" + Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS + "' has been forced to value '" +
              Integer.toString(cliTickTupleFreqSecs) + "' via CLI configuration.  This will override the desired " +
              "setting of '" + Integer.toString(recommendedTickIntervalSecs) +
              "' and may lead to unexpected periodicity in batch flushing.");
    }
    return recommendedTickIntervalSecs;
  }
}
