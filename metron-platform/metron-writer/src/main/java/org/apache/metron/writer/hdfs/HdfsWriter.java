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
package org.apache.metron.writer.hdfs;

import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.MapVariableResolver;
import org.apache.metron.common.dsl.StellarFunctions;
import org.apache.metron.common.dsl.VariableResolver;
import org.apache.metron.common.stellar.StellarCompiler;
import org.apache.metron.common.stellar.StellarProcessor;
import org.apache.storm.tuple.Tuple;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.rotation.FileRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.NoRotationPolicy;
import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;
import org.apache.storm.hdfs.common.rotation.RotationAction;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class HdfsWriter implements BulkMessageWriter<JSONObject>, Serializable {
  List<RotationAction> rotationActions = new ArrayList<>();
  FileRotationPolicy rotationPolicy = new NoRotationPolicy();
  SyncPolicy syncPolicy = new CountSyncPolicy(1); //sync every time, duh.
  FileNameFormat fileNameFormat;
  Map<SourceHandlerKey, SourceHandler> sourceHandlerMap = new HashMap<>();
  int maxOpenFiles = 500;
  transient StellarProcessor stellarProcessor;
  transient Map stormConfig;


  public HdfsWriter withFileNameFormat(FileNameFormat fileNameFormat){
    this.fileNameFormat = fileNameFormat;
    return this;
  }

  public HdfsWriter withSyncPolicy(SyncPolicy syncPolicy){
    this.syncPolicy = syncPolicy;
    return this;
  }
  public HdfsWriter withRotationPolicy(FileRotationPolicy rotationPolicy){
    this.rotationPolicy = rotationPolicy;
    return this;
  }

  public HdfsWriter addRotationAction(RotationAction action){
    this.rotationActions.add(action);
    return this;
  }

  public HdfsWriter withMaxOpenFiles(int maxOpenFiles) {
    this.maxOpenFiles = maxOpenFiles;
    return this;
  }

  @Override
  public void init(Map stormConfig, WriterConfiguration configurations) {
    this.stormConfig = stormConfig;
    this.stellarProcessor = new StellarProcessor();
  }


  @Override
  public BulkWriterResponse write(String sourceType
                   , WriterConfiguration configurations
                   , Iterable<Tuple> tuples
                   , List<JSONObject> messages
                   ) throws Exception
  {
    BulkWriterResponse response = new BulkWriterResponse();
    // Currently treating all the messages in a group for pass/failure.
    try {
      // Messages can all result in different HDFS paths, because of Stellar Expressions, so we'll need to iterate through
      for(JSONObject message : messages) {
        Map<String, Object> val = configurations.getSensorConfig(sourceType);
        String path = getHdfsPathExtension(
                sourceType,
                (String)configurations.getSensorConfig(sourceType).getOrDefault(IndexingConfigurations.OUTPUT_PATH_FUNCTION_CONF, ""),
                message
        );
        SourceHandler handler = getSourceHandler(sourceType, path);
        handler.handle(message);
      }
    } catch (Exception e) {
      response.addAllErrors(e, tuples);
    }

    response.addAllSuccesses(tuples);
    return response;
  }

  public String getHdfsPathExtension(String sourceType, String stellarFunction, JSONObject message) {
    // If no function is provided, just use the sourceType directly
    if(stellarFunction == null || stellarFunction.trim().isEmpty()) {
      return sourceType;
    }

    //processor is a StellarProcessor();
    VariableResolver resolver = new MapVariableResolver(message);
    Object objResult = stellarProcessor.parse(stellarFunction, resolver, StellarFunctions.FUNCTION_RESOLVER(), Context.EMPTY_CONTEXT());
    if(objResult != null && !(objResult instanceof String)) {
      throw new IllegalArgumentException("Stellar Function <" + stellarFunction + "> did not return a String value. Returned: " + objResult);
    }
    return objResult == null ? "" : (String)objResult;
  }

  @Override
  public String getName() {
    return "hdfs";
  }

  @Override
  public void close() {
    for(SourceHandler handler : sourceHandlerMap.values()) {
      handler.close();
    }
    // Everything is closed, so just clear it
    sourceHandlerMap.clear();
  }

  synchronized SourceHandler getSourceHandler(String sourceType, String stellarResult) throws IOException {
    SourceHandlerKey key = new SourceHandlerKey(sourceType, stellarResult);
    SourceHandler ret = sourceHandlerMap.get(key);
    if(ret == null) {
      if(sourceHandlerMap.size() >= maxOpenFiles) {
        throw new IllegalStateException("Too many HDFS files open!");
      }
      ret = new SourceHandler(rotationActions,
                              rotationPolicy,
                              syncPolicy,
                              new PathExtensionFileNameFormat(key.getStellarResult(), fileNameFormat),
                              new SourceHandlerCallback(sourceHandlerMap, key));
      sourceHandlerMap.put(key, ret);
    }
    return ret;
  }
}
