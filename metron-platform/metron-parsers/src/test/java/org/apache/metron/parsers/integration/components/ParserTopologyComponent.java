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
package org.apache.metron.parsers.integration.components;

import static org.apache.metron.integration.components.FluxTopologyComponent.assassinateSlots;
import static org.apache.metron.integration.components.FluxTopologyComponent.cleanupWorkerDir;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.parsers.topology.ParserTopologyBuilder;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.generated.KillOptions;
import org.apache.storm.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserTopologyComponent implements InMemoryComponent {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private Properties topologyProperties;
  private String brokerUrl;
  private String sensorType;
  private LocalCluster stormCluster;
  private String outputTopic;

  public static class Builder {
    Properties topologyProperties;
    String brokerUrl;
    String sensorType;
    String outputTopic;
    public Builder withTopologyProperties(Properties topologyProperties) {
      this.topologyProperties = topologyProperties;
      return this;
    }
    public Builder withBrokerUrl(String brokerUrl) {
      this.brokerUrl = brokerUrl;
      return this;
    }
    public Builder withSensorType(String sensorType) {
      this.sensorType = sensorType;
      return this;
    }

    public Builder withOutputTopic(String topic) {
      this.outputTopic = topic;
      return this;
    }

    public ParserTopologyComponent build() {
      return new ParserTopologyComponent(topologyProperties, brokerUrl, sensorType, outputTopic);
    }
  }

  public ParserTopologyComponent(Properties topologyProperties, String brokerUrl, String sensorType, String outputTopic) {
    this.topologyProperties = topologyProperties;
    this.brokerUrl = brokerUrl;
    this.sensorType = sensorType;
    this.outputTopic = outputTopic;
  }

  public void updateSensorType(String sensorType) {
    this.sensorType = sensorType;
  }

  @Override
  public void start() throws UnableToStartException {
    try {
      final Map<String, Object> stormConf = new HashMap<>();
      stormConf.put(Config.TOPOLOGY_DEBUG, true);
      ParserTopologyBuilder.ParserTopology topologyBuilder = ParserTopologyBuilder.build(topologyProperties.getProperty(ZKServerComponent.ZOOKEEPER_PROPERTY)
                                                                   , Optional.ofNullable(brokerUrl)
                                                                   , sensorType
                                                                   , (x,y) -> 1
                                                                   , (x,y) -> 1
                                                                   , (x,y) -> 1
                                                                   , (x,y) -> 1
                                                                   , (x,y) -> 1
                                                                   , (x,y) -> 1
                                                                   , (x,y) -> new HashMap<>()
                                                                   , (x,y) -> null
                                                                   , Optional.ofNullable(outputTopic)
                                                                   , (x,y) -> {
                                                                      Config c = new Config();
                                                                      c.putAll(stormConf);
                                                                      return c;
                                                                      }
                                                                   );

      stormCluster = new LocalCluster();
      stormCluster.submitTopology(sensorType, stormConf, topologyBuilder.getBuilder().createTopology());
    } catch (Exception e) {
      throw new UnableToStartException("Unable to start parser topology for sensorType: " + sensorType, e);
    }
  }

  @Override
  public void stop() {
    if (stormCluster != null) {
      try {
        try {
          // Kill the topology directly instead of sitting through the wait period
          killTopology();
          stormCluster.shutdown();
        } catch (IllegalStateException ise) {
          if (!(ise.getMessage().contains("It took over") && ise.getMessage().contains("to shut down slot"))) {
            throw ise;
          }
          else {
            assassinateSlots();
            LOG.error("Storm slots didn't shut down entirely cleanly *sigh*.  " +
                    "I gave them the old one-two-skadoo and killed the slots with prejudice.  " +
                    "If tests fail, we'll have to find a better way of killing them.", ise);
          }
        }
      }
      catch(Throwable t) {
        LOG.error(t.getMessage(), t);
      }
      finally {
        cleanupWorkerDir();
      }

    }
  }

  @Override
  public void reset() {
    if (stormCluster != null) {
      killTopology();
    }
  }

  protected void killTopology() {
    KillOptions ko = new KillOptions();
    ko.set_wait_secs(0);
    stormCluster.killTopologyWithOpts(sensorType, ko);
    try {
      // Actually wait for it to die.
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      // Do nothing
    }
  }
}
