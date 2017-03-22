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

import org.apache.kafka.common.errors.WakeupException;
import org.apache.log4j.Logger;
import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;

public class StormKafkaSpout<K, V> extends KafkaSpout<K, V> {
  private static final Logger LOG = Logger.getLogger(StormKafkaSpout.class);
  protected KafkaSpoutConfig<K,V> _spoutConfig;
  protected String _topic;
  public StormKafkaSpout(SimpleStormKafkaBuilder<K,V> builder) {
    super(builder.build());
    this._topic = builder.getTopic();
    this._spoutConfig = builder.build();
  }

  @Override
  public void close() {
    try {
      super.close();
    }
    catch(WakeupException we) {
      LOG.error(we.getMessage(), we);
    }
  }
}
