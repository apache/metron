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
package org.apache.metron.solr.writer;

import static org.apache.metron.solr.SolrConstants.SOLR_WRITER_NAME;

import com.google.common.base.Joiner;
import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.metron.common.writer.MessageId;
import org.apache.metron.solr.SolrConstants;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.storm.task.TopologyContext;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrWriter implements BulkMessageWriter<JSONObject>, Serializable {

  public static final String JAVA_SECURITY_CONFIG_PROPERTY = "java.security.auth.login.config";

  public enum SolrProperties {
    ZOOKEEPER_QUORUM(SolrConstants.SOLR_ZOOKEEPER),
    COMMIT_PER_BATCH("solr.commitPerBatch", Optional.of(true)),
    COMMIT_WAIT_SEARCHER("solr.commit.waitSearcher", Optional.of(true)),
    COMMIT_WAIT_FLUSH("solr.commit.waitFlush", Optional.of(true)),
    COMMIT_SOFT("solr.commit.soft", Optional.of(false)),
    DEFAULT_COLLECTION("solr.collection", Optional.of("metron")),
    HTTP_CONFIG("solr.http.config", Optional.of(new HashMap<>()))
    ;
    String name;
    Optional<Object> defaultValue;

    SolrProperties(String name) {
      this(name, Optional.empty());
    }
    SolrProperties(String name, Optional<Object> defaultValue) {
      this.name = name;
      this.defaultValue = defaultValue;
    }

    public <T> Optional<T> coerceOrDefault(Map<String, Object> globalConfig, Class<T> clazz) {
      Object val = globalConfig.get(name);
      if(val != null) {
        T ret = null;
        try {
          ret = ConversionUtils.convert(val, clazz);
        }
        catch(ClassCastException cce) {
          ret = null;
        }
        if(ret == null) {
          //unable to convert value
          LOG.warn("Unable to convert {} to {}, was {}", name, clazz.getName(), "" + val);
          if(defaultValue.isPresent()) {
            return Optional.ofNullable(ConversionUtils.convert(defaultValue.get(), clazz));
          }
          else {
            return Optional.empty();
          }
        }
        else {
          return Optional.ofNullable(ret);
        }
      }
      else {
        if(defaultValue.isPresent()) {
          return Optional.ofNullable(ConversionUtils.convert(defaultValue.get(), clazz));
        }
        else {
          return Optional.empty();
        }
      }
    }

    public Supplier<IllegalArgumentException> errorOut(Map<String, Object> globalConfig) {
      String message = "Unable to retrieve " + name + " from global config, value associated is " + globalConfig.get(name);
      return () -> new IllegalArgumentException(message);
    }

    public <T> T coerceOrDefaultOrExcept(Map<String, Object> globalConfig, Class<T> clazz) {
         return this.coerceOrDefault(globalConfig, clazz).orElseThrow(this.errorOut(globalConfig));
    }

  }


  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Boolean shouldCommit;
  private Boolean softCommit;
  private Boolean waitSearcher;
  private Boolean waitFlush;
  private String zookeeperUrl;
  private String defaultCollection;
  private Map<String, Object> solrHttpConfig;

  private MetronSolrClient solr;

  public SolrWriter withMetronSolrClient(MetronSolrClient solr) {
    this.solr = solr;
    return this;
  }

  public void initializeFromGlobalConfig(Map<String, Object> globalConfiguration) {
    zookeeperUrl = SolrProperties.ZOOKEEPER_QUORUM.coerceOrDefaultOrExcept(globalConfiguration, String.class);
    defaultCollection = SolrProperties.DEFAULT_COLLECTION.coerceOrDefaultOrExcept(globalConfiguration, String.class);
    solrHttpConfig = SolrProperties.HTTP_CONFIG.coerceOrDefaultOrExcept(globalConfiguration, Map.class);
    shouldCommit = SolrProperties.COMMIT_PER_BATCH.coerceOrDefaultOrExcept(globalConfiguration, Boolean.class);
    softCommit = SolrProperties.COMMIT_SOFT.coerceOrDefaultOrExcept(globalConfiguration, Boolean.class);
    waitSearcher = SolrProperties.COMMIT_WAIT_SEARCHER.coerceOrDefaultOrExcept(globalConfiguration, Boolean.class);
    waitFlush = SolrProperties.COMMIT_WAIT_FLUSH.coerceOrDefaultOrExcept(globalConfiguration, Boolean.class);
  }

  @Override
  public void init(Map stormConf, TopologyContext topologyContext, WriterConfiguration configurations) throws IOException, SolrServerException {
    Map<String, Object> globalConfiguration = configurations.getGlobalConfig();
    initializeFromGlobalConfig(globalConfiguration);
    LOG.info("Initializing SOLR writer: {}", zookeeperUrl);
    LOG.info("Forcing commit per batch: {}", shouldCommit);
    LOG.info("Soft commit: {}", softCommit);
    LOG.info("Commit Wait Searcher: {}", waitSearcher);
    LOG.info("Commit Wait Flush: {}", waitFlush);
    LOG.info("Default Collection: {}", "" + defaultCollection );
    if(solr == null) {
      if (isKerberosEnabled(stormConf)) {
        HttpClientUtil.addConfigurer(new Krb5HttpClientConfigurer());
      }
      solr = new MetronSolrClient(zookeeperUrl, solrHttpConfig);
    }
    solr.setDefaultCollection(defaultCollection);

  }

  public Collection<SolrInputDocument> toDocs(Iterable<BulkMessage<JSONObject>> messages) {
    Collection<SolrInputDocument> ret = new ArrayList<>();
    for(BulkMessage<JSONObject> bulkWriterMessage: messages) {
      SolrInputDocument document = new SolrInputDocument();
      JSONObject message = bulkWriterMessage.getMessage();
      for (Object key : message.keySet()) {
        Object value = message.get(key);
        if (value instanceof Iterable) {
          for (Object v : (Iterable) value) {
            document.addField("" + key, v);
          }
        } else {
          document.addField("" + key, value);
        }
      }
      if (!document.containsKey(Constants.GUID)) {
        document.addField(Constants.GUID, UUID.randomUUID().toString());
      }
      ret.add(document);
    }
    return ret;
  }

  protected String getCollection(String sourceType, WriterConfiguration configurations) {
    String collection = configurations.getIndex(sourceType);
    if(StringUtils.isEmpty(collection)) {
      return solr.getDefaultCollection();
    }
    return collection;
  }

  @Override
  public BulkWriterResponse write(String sourceType, WriterConfiguration configurations, List<BulkMessage<JSONObject>> messages) throws Exception {
    String collection = getCollection(sourceType, configurations);
    BulkWriterResponse bulkResponse = new BulkWriterResponse();
    Collection<SolrInputDocument> docs = toDocs(messages);
    Set<MessageId> ids = messages.stream().map(BulkMessage::getId).collect(Collectors.toSet());
    try {
      Optional<SolrException> exceptionOptional = fromUpdateResponse(solr.add(collection, docs));
      // Solr commits the entire batch or throws an exception for it.  There's no way to get partial failures.
      if(exceptionOptional.isPresent()) {
        bulkResponse.addAllErrors(exceptionOptional.get(), ids);
      }
      else {
        if (shouldCommit) {
          exceptionOptional = fromUpdateResponse(solr.commit(collection, waitFlush, waitSearcher, softCommit));
          if(exceptionOptional.isPresent()) {
            bulkResponse.addAllErrors(exceptionOptional.get(), ids);
          }
        }
        if(!exceptionOptional.isPresent()) {
          bulkResponse.addAllSuccesses(ids);
        }
      }
    }
    catch(HttpSolrClient.RemoteSolrException sse) {
      bulkResponse.addAllErrors(sse, ids);
    }

    return bulkResponse;
  }

  protected Optional<SolrException> fromUpdateResponse(UpdateResponse response) {
    if(response != null && response.getStatus() > 0) {
      String message = "Solr Update response: " + Joiner.on(",").join(response.getResponse());
      return Optional.of(new SolrException(SolrException.ErrorCode.BAD_REQUEST, message));
    }
    return Optional.empty();
  }

  @Override
  public String getName() {
    return SOLR_WRITER_NAME;
  }

  @Override
  public void close() throws Exception {
    if(solr != null) {
      solr.close();
    }
  }

  private boolean isKerberosEnabled(Map stormConfig) {
    if (stormConfig == null) {
      return false;
    }
    String value = (String) stormConfig.get(JAVA_SECURITY_CONFIG_PROPERTY);
    return value != null && !value.isEmpty();
  }
}
