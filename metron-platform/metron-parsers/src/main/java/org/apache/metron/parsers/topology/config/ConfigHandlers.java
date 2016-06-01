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

package org.apache.metron.parsers.topology.config;

import backtype.storm.Config;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.FileUtils;
import org.apache.metron.common.utils.JSONUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

public class ConfigHandlers {
  public static class SetNumWorkersHandler implements Function<Arg, Config> {
    @Override
    public Config apply(Arg arg) {
      if(arg.getArg() != null) {
        arg.getConfig().setNumWorkers(Integer.parseInt(arg.getArg()));
      }
      return arg.getConfig();
    }
  }
  public static class SetNumAckersHandler implements Function<Arg, Config> {
    @Override
    public Config apply(Arg arg) {
      if(arg.getArg() != null) {
        arg.getConfig().setNumAckers(Integer.parseInt(arg.getArg()));
      }
      return arg.getConfig();
    }
  }
  public static class SetMaxTaskParallelismHandler implements Function<Arg, Config> {
    @Override
    public Config apply(Arg arg) {
      if(arg.getArg() != null) {
        arg.getConfig().setMaxTaskParallelism(Integer.parseInt(arg.getArg()));
      }
      return arg.getConfig();
    }
  }
  public static class SetMessageTimeoutHandler implements Function<Arg, Config> {
    @Override
    public Config apply(Arg arg) {
      if(arg.getArg() != null) {
        arg.getConfig().setMessageTimeoutSecs(Integer.parseInt(arg.getArg()));
      }
      return arg.getConfig();
    }
  }
  public static class LoadJSONHandler implements Function<Arg, Config> {
    @Override
    public Config apply(Arg arg) {
      if(arg.getArg() != null) {
        File inputFile = new File(arg.getArg());
        String json = null;
        if (inputFile.exists()) {
          try {
            json = FileUtils.readFileToString(inputFile);
          } catch (IOException e) {
            throw new IllegalStateException("Unable to process JSON file " + inputFile, e);
          }
        } else {
          json = arg.getArg();
        }
        try {
          arg.getConfig().putAll(JSONUtils.INSTANCE.load(json, new TypeReference<Map<String, Object>>() {
          }));
        } catch (IOException e) {
          throw new IllegalStateException("Unable to process JSON snippet.", e);
        }
      }
      return arg.getConfig();
    }
  }
}
