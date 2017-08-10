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

package org.apache.metron.storm.kafka.flux;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A thin wrapper atop the KafkaSpout to allow us to pass in the Builder rather than the SpoutConfig.
 * This enables creating a simplified interface suitable for use in flux for this spout.
 * @param <K>
 * @param <V>
 */
public class StormKafkaSpout<K, V> extends KafkaSpout<K, V> {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected KafkaSpoutConfig<K,V> _spoutConfig;
  protected AtomicBoolean isShutdown = new AtomicBoolean(false);

  public StormKafkaSpout(SimpleStormKafkaBuilder<K,V> builder) {
    super(builder.build());
    this._spoutConfig = builder.build();
  }

  @Override
  public void deactivate() {
    try {
      super.deactivate();
    }
    catch(WakeupException we) {
      //see https://issues.apache.org/jira/browse/STORM-2184
      LOG.warn("You can generally ignore these, as per https://issues.apache.org/jira/browse/STORM-2184 -- {}", we.getMessage(), we);
    }
    finally {
      isShutdown.set(true);
    }
  }

  @Override
  public void close() {
    try {
      if(!isShutdown.get()) {
        super.close();
        isShutdown.set(true);
      }
    }
    catch(WakeupException we) {
      //see https://issues.apache.org/jira/browse/STORM-2184
      LOG.warn("You can generally ignore these, as per https://issues.apache.org/jira/browse/STORM-2184 -- {}", we.getMessage(), we);
    }
    catch(IllegalStateException ise) {
      if(ise.getMessage().contains("This consumer has already been closed")) {
        LOG.warn(ise.getMessage());
      }
      else {
        throw ise;
      }
    }
  }
}
