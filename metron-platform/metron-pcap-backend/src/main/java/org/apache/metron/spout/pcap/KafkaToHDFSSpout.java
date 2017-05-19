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

package org.apache.metron.spout.pcap;

import org.apache.metron.storm.kafka.flux.SimpleStormKafkaBuilder;
import org.apache.storm.kafka.Callback;
import org.apache.storm.kafka.CallbackKafkaSpout;

import java.util.ArrayList;
import java.util.List;

public class KafkaToHDFSSpout extends CallbackKafkaSpout<byte[], byte[]> {
  static final long serialVersionUID = 0xDEADBEEFL;
  HDFSWriterConfig config = null;
  private static ThreadLocal<List<Object>> messagesToBeAcked = new ThreadLocal<List<Object>>() {
    @Override
    protected List<Object> initialValue() {
      return new ArrayList<>();
    }
  };

  public KafkaToHDFSSpout( SimpleStormKafkaBuilder<byte[], byte[]> spoutConfig
                         , HDFSWriterConfig config
                         )
  {
    super(spoutConfig
         , HDFSWriterCallback.class
         );
    this.config = config;
  }

  @Override
  protected Callback createCallback(Class<? extends Callback> callbackClass) {
    return new HDFSWriterCallback().withConfig(config);
  }

  private void clearMessagesToBeAcked() {
      for (Object messageId : messagesToBeAcked.get()) {
        super.ack(messageId);
      }
      messagesToBeAcked.get().clear();
  }

  @Override
  public void nextTuple() {
    super.nextTuple();
    clearMessagesToBeAcked();
  }

  @Override
  public void ack(Object messageId) {
    messagesToBeAcked.get().add(messageId);
  }

  @Override
  public void close() {
    try {
      clearMessagesToBeAcked();
    }
    finally {
      super.close();
    }
  }
}
