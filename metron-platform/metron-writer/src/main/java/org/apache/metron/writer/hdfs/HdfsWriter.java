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

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.metron.common.writer.MessageId;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.MapVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.stellar.dsl.VariableResolver;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.rotation.FileRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.NoRotationPolicy;
import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;
import org.apache.storm.hdfs.common.rotation.RotationAction;
import org.apache.storm.task.TopologyContext;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HdfsWriter implements BulkMessageWriter<JSONObject>, Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  List<RotationAction> rotationActions = new ArrayList<>();
  FileRotationPolicy rotationPolicy = new NoRotationPolicy();
  SyncPolicy syncPolicy;
  FileNameFormat fileNameFormat;
  Map<SourceHandlerKey, SourceHandler> sourceHandlerMap = new HashMap<>();
  int maxOpenFiles = 500;
  transient StellarProcessor stellarProcessor;
  transient Map stormConfig;
  transient SyncPolicyCreator syncPolicyCreator;


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
  public void init(Map stormConfig, TopologyContext topologyContext, WriterConfiguration configurations) {
    this.stormConfig = stormConfig;
    this.stellarProcessor = new StellarProcessor();
    this.fileNameFormat.prepare(stormConfig,topologyContext);
    if(syncPolicy != null) {
      //if the user has specified the sync policy, we don't want to override their wishes.
      LOG.debug("Using user specified sync policy {}", syncPolicy.getClass().getSimpleName());
      syncPolicyCreator = new ClonedSyncPolicyCreator(syncPolicy);
    }
    else {
      //if the user has not, then we want to have the sync policy depend on the batch size.
      LOG.debug("No user specified sync policy, using CountSyncPolicy based on batch size");
      syncPolicyCreator = (source, config) -> new CountSyncPolicy(config == null?1:config.getBatchSize(source));
    }
  }

  @Override
  public BulkWriterResponse write(String sensorType, WriterConfiguration configurations, List<BulkMessage<JSONObject>> messages) throws Exception {
    BulkWriterResponse response = new BulkWriterResponse();
    Set<MessageId> ids = messages.stream().map(BulkMessage::getId).collect(Collectors.toSet());

    // Currently treating all the messages in a group for pass/failure.
    // Messages can all result in different HDFS paths, because of Stellar Expressions, so we'll need to iterate through
    for (BulkMessage<JSONObject> bulkWriterMessage : messages) {
      JSONObject message = bulkWriterMessage.getMessage();
      String path = getHdfsPathExtension(
              sensorType,
              (String) configurations.getSensorConfig(sensorType)
                      .getOrDefault(IndexingConfigurations.OUTPUT_PATH_FUNCTION_CONF, ""),
              message
      );

      try {
        LOG.trace("Writing message {} to path: {}", message.toJSONString(), path);
        SourceHandler handler = getSourceHandler(sensorType, path, configurations);
        handler.handle(message, sensorType, configurations, syncPolicyCreator);
      } catch (Exception e) {
        LOG.error(
                "HdfsWriter encountered error writing. Source type: {}. # messages: {}. Output path: {}.",
                sensorType,
                messages.size(),
                path,
                e
        );
        response.addAllErrors(e, ids);
      }
    }

    response.addAllSuccesses(ids);
    return response;
  }

  public String getHdfsPathExtension(String sourceType, String stellarFunction, JSONObject message) {
    // If no function is provided, just use the sourceType directly
    if(stellarFunction == null || stellarFunction.trim().isEmpty()) {
      LOG.debug("No HDFS path extension provided; using source type {} directly", sourceType);
      return sourceType;
    }

    //processor is a StellarProcessor();
    VariableResolver resolver = new MapVariableResolver(message);
    Object objResult = stellarProcessor.parse(stellarFunction, resolver, StellarFunctions.FUNCTION_RESOLVER(), Context.EMPTY_CONTEXT());
    if(objResult != null && !(objResult instanceof String)) {
      String errorMsg = "Stellar Function <" + stellarFunction + "> did not return a String value. Returned: " + objResult;
      LOG.error(errorMsg);
      throw new IllegalArgumentException(errorMsg);
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
      LOG.debug("Closing SourceHandler {}", handler.toString());
      handler.close();
    }
    // Everything is closed, so just clear it
    sourceHandlerMap.clear();
  }

  synchronized SourceHandler getSourceHandler(String sourceType, String stellarResult, WriterConfiguration config) throws IOException {
    SourceHandlerKey key = new SourceHandlerKey(sourceType, stellarResult);
    SourceHandler ret = sourceHandlerMap.get(key);
    if(ret == null) {
      if(sourceHandlerMap.size() >= maxOpenFiles) {
        String errorMsg = "Too many HDFS files open! Maximum number of open files is: " + maxOpenFiles +
            ". Current number of open files is: " + sourceHandlerMap.size();
        LOG.error(errorMsg);
        throw new IllegalStateException(errorMsg);
      }
      ret = new SourceHandler(rotationActions,
                              rotationPolicy,
                              syncPolicyCreator.create(sourceType, config),
                              new PathExtensionFileNameFormat(key.getStellarResult(), fileNameFormat),
                              new SourceHandlerCallback(sourceHandlerMap, key));
      LOG.debug("Placing key in sourceHandlerMap: {}", key);
      sourceHandlerMap.put(key, ret);
    }
    return ret;
  }
}
