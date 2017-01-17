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
package org.apache.metron.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.log4j.Logger;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.dsl.StellarFunction;
import org.apache.metron.common.utils.ConversionUtils;
import org.apache.metron.common.utils.JSONUtils;

import java.util.List;
import java.util.Map;

import static org.apache.metron.common.configuration.ConfigurationType.INDEXING;


public class IndexingConfigFunctions {
  private static final Logger LOG = Logger.getLogger(IndexingConfigFunctions.class);
  @Stellar(
           namespace = "INDEXING"
          ,name = "SET_BATCH"
          ,description = "Set batch size"
          ,params = {"sensorConfig - Sensor config to add transformation to."
                    ,"size - batch size (integer)"
                    }
          ,returns = "The String representation of the config in zookeeper"
          )
  public static class SetBatchSize implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      int i = 0;
      String config = (String) args.get(i++);
      Map<String, Object> configObj;
      if(config == null || config.isEmpty()) {
        throw new IllegalStateException("Invalid config: " + config);
      }
      else {
        configObj = (Map<String, Object>) INDEXING.deserialize(config);
      }
      int batchSize = 5;
      if(args.size() > 1) {
        batchSize = ConversionUtils.convert(args.get(i++), Integer.class);
      }
      configObj = IndexingConfigurations.setBatchSize(configObj, batchSize);
      try {
        return JSONUtils.INSTANCE.toJSON(configObj, true);
      } catch (JsonProcessingException e) {
        LOG.error("Unable to convert object to JSON: " + configObj, e);
        return config;
      }
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  @Stellar(
           namespace = "INDEXING"
          ,name = "SET_INDEX"
          ,description = "Set the index for the sensor"
          ,params = {"sensorConfig - Sensor config to add transformation to."
                    ,"sensor - sensor name"
                    }
          ,returns = "The String representation of the config in zookeeper"
          )
  public static class SetIndex implements StellarFunction{

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      int i = 0;
      String config = (String) args.get(i++);
      Map<String, Object> configObj;
      if(config == null || config.isEmpty()) {
        throw new IllegalStateException("Invalid config: " + config);
      }
      else {
        configObj = (Map<String, Object>) INDEXING.deserialize(config);
      }
      String sensorName = ConversionUtils.convert(args.get(i++), String.class);
      if(sensorName == null) {
        throw new IllegalStateException("Invalid sensor name: " + config);
      }
      configObj = IndexingConfigurations.setIndex(configObj, sensorName);
      try {
        return JSONUtils.INSTANCE.toJSON(configObj, true);
      } catch (JsonProcessingException e) {
        LOG.error("Unable to convert object to JSON: " + configObj, e);
        return config;
      }
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }
}
