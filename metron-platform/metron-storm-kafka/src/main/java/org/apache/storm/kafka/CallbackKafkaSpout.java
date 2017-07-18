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
package org.apache.storm.kafka;


import org.apache.metron.storm.kafka.flux.SimpleStormKafkaBuilder;
import org.apache.metron.storm.kafka.flux.StormKafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * A kafka spout with a callback that is executed on message commit.
 * @param <K> The Kafka key type
 * @param <V> The Kafka value type
 */
public class CallbackKafkaSpout<K, V> extends StormKafkaSpout<K, V> {
  static final long serialVersionUID = 0xDEADBEEFL;
  Class<? extends Callback> callbackClazz;
  Callback _callback;
  EmitContext _context;
  public CallbackKafkaSpout(SimpleStormKafkaBuilder<K, V> spoutConfig, String callbackClass) {
    this(spoutConfig, toCallbackClass(callbackClass));
  }

  public CallbackKafkaSpout(SimpleStormKafkaBuilder<K, V> spoutConf, Class<? extends Callback> callback) {
    super(spoutConf);
    callbackClazz = callback;
  }

  public void initialize(TopologyContext context) {
    _callback = createCallback(callbackClazz);
    _context = new EmitContext().with(EmitContext.Type.SPOUT_CONFIG, _spoutConfig)
                                .with(EmitContext.Type.UUID, context.getStormId())
                                ;
    _callback.initialize(_context);
  }


  private static Class<? extends Callback> toCallbackClass(String callbackClass)  {
    try{
      return (Class<? extends Callback>) Callback.class.forName(callbackClass);
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException(callbackClass + " not found", e);
    }
  }

  protected Callback createCallback(Class<? extends Callback> callbackClass)  {
    try {
      return callbackClass.getConstructor().newInstance();
    } catch (InstantiationException | NoSuchMethodException | InvocationTargetException e) {
      throw new RuntimeException("Unable to instantiate callback", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Illegal access", e);
    }
  }

  /**
   * This overrides and wraps the SpoutOutputCollector so that the callback can operate upon emit.
   * @param conf
   * @param context
   * @param collector
   */
  @Override
  public void open(Map conf, final TopologyContext context, final SpoutOutputCollector collector) {
    if(_callback == null) {
      initialize(context);
    }
    super.open( conf, context
            , new CallbackCollector(_callback, collector
                    ,_context.cloneContext().with(EmitContext.Type.OPEN_CONFIG, conf)
                    .with(EmitContext.Type.TOPOLOGY_CONTEXT, context)
            )
    );
  }

  @Override
  public void close() {
    super.close();
    if(_callback != null) {
      try {
        _callback.close();
      } catch (Exception e) {
        throw new IllegalStateException("Unable to close callback", e);
      }
    }
  }
}
