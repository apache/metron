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
package org.apache.metron.solr.integration.components;

import com.google.common.base.Function;
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.metaalert.MetaAlertUpdateDao;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.UpdateDao;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.solr.dao.SolrUtilities;
import org.apache.metron.solr.writer.MetronSolrClient;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.apache.solr.common.SolrDocument;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.solr.common.SolrInputDocument;
import org.apache.zookeeper.KeeperException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SolrComponent implements InMemoryComponent {

  public static class Builder {

    private int port = 8983;
    private String solrXmlPath = "../metron-solr/src/test/resources/solr/solr.xml";
    private Map<String, String> initialCollections = new HashMap<>();
    private Function<SolrComponent, Void> postStartCallback;

    public Builder withPort(int port) {
      this.port = port;
      return this;
    }

    public Builder withSolrXmlPath(String solrXmlPath) {
      this.solrXmlPath = solrXmlPath;
      return this;
    }

    public Builder addInitialCollection(String name, String configPath) {
      initialCollections.put(name, configPath);
      return this;
    }

    public Builder withPostStartCallback(Function<SolrComponent, Void> f) {
      postStartCallback = f;
      return this;
    }

    public SolrComponent build() {
      return new SolrComponent(port, solrXmlPath, initialCollections, postStartCallback);
    }
  }

  private int port;
  private String solrXmlPath;
  private Map<String, String> collections;
  private MiniSolrCloudCluster miniSolrCloudCluster;
  private Function<SolrComponent, Void> postStartCallback;

  private SolrComponent(int port, String solrXmlPath, Map<String, String> collections,
      Function<SolrComponent, Void> postStartCallback) {
    this.port = port;
    this.solrXmlPath = solrXmlPath;
    this.collections = collections;
    this.postStartCallback = postStartCallback;
  }

  @Override
  public void start() throws UnableToStartException {
    try {
      File baseDir = Files.createTempDirectory("solrcomponent").toFile();
      baseDir.deleteOnExit();
      miniSolrCloudCluster = new MiniSolrCloudCluster(1, baseDir.toPath(),
          JettyConfig.builder().setPort(port).build());
      for(String name: collections.keySet()) {
        String configPath = collections.get(name);
        miniSolrCloudCluster.uploadConfigSet(new File(configPath).toPath(), name);
        CollectionAdminRequest.createCollection(name, 1, 1).process(miniSolrCloudCluster.getSolrClient());
      }
      if (postStartCallback != null) {
        postStartCallback.apply(this);
      }
    } catch (Exception e) {
      throw new UnableToStartException(e.getMessage(), e);
    }
  }

  @Override
  public void stop() {
    try {
      miniSolrCloudCluster.deleteAllCollections();
      miniSolrCloudCluster.shutdown();
    } catch (Exception e) {
      // Do nothing
    }
  }

  @Override
  public void reset() {
    try {
      miniSolrCloudCluster.deleteAllCollections();
    } catch (Exception e) {
      // Do nothing
    }
  }

  public MetronSolrClient getSolrClient() {
    return new MetronSolrClient(getZookeeperUrl());
  }

  public MiniSolrCloudCluster getMiniSolrCloudCluster() {
    return this.miniSolrCloudCluster;
  }

  public String getZookeeperUrl() {
    return miniSolrCloudCluster.getZkServer().getZkAddress();
  }

  public void addCollection(String name, String configPath)
      throws InterruptedException, IOException, KeeperException, SolrServerException {
    miniSolrCloudCluster.uploadConfigSet(new File(configPath).toPath(), name);
    CollectionAdminRequest.createCollection(name, 1, 1)
        .process(miniSolrCloudCluster.getSolrClient());
  }

  public boolean hasCollection(String collection) {
    MetronSolrClient solr = getSolrClient();
    boolean collectionFound = false;
    try {
      collectionFound = solr.listCollections().contains(collection);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return collectionFound;
  }

  public List<Map<String, Object>> getAllIndexedDocs(String collection) {
    List<Map<String, Object>> docs = new ArrayList<>();
    CloudSolrClient solr = miniSolrCloudCluster.getSolrClient();
    solr.setDefaultCollection(collection);
    SolrQuery parameters = new SolrQuery();

    // If it's metaalert, we need to adjust the query. We want child docs with the parent,
    // not separate.
    if (collection.equals("metaalert")) {
      parameters.setQuery("source.type:metaalert")
          .setFields("*", "[child parentFilter=source.type:metaalert limit=999]");
    } else {
      parameters.set("q", "*:*");
    }
    try {
      solr.commit();
      QueryResponse response = solr.query(parameters);
      for (SolrDocument solrDocument : response.getResults()) {
        // Use the utils to make sure we get child docs.
        docs.add(SolrUtilities.toDocument(solrDocument).getDocument());
      }
    } catch (SolrServerException | IOException e) {
      e.printStackTrace();
    }
    return docs;
  }

  public void addAlerts(UpdateDao updateDao, String collection, List<Map<String, Object>> messages) throws IOException {
    // transform each input message to a Document that can be indexed by the UpdateDao
    for(Map<String, Object> msg: messages) {
      updateDao.update(createDocument(msg, collection), Optional.of(collection));
    }

    // TODO do we need to wait until the documents are visible?
    try {
      Thread.sleep(2000);
    } catch(Exception e) {
    }

//
//    checkUpdateResponse(solr.add(collection, solrInputDocuments));
//    // Make sure to commit so things show up
//    checkUpdateResponse(solr.commit(true, true));
  }

  private List<GetRequest> getRequest(List<Map<String, Object>> alerts) {
    List<GetRequest> requests = new ArrayList<>();
    for(Map<String, Object> alert: alerts) {
      String guid = String.class.cast(alert.get(Constants.GUID));
      requests.add(new GetRequest(guid, MetaAlertConstants.METAALERT_TYPE));
    }
    return requests;
  }

  public void addMetaAlert(MetaAlertUpdateDao updateDao, String guid, MetaAlertStatus status,
                           Optional<List<Map<String, Object>>> alerts) throws IOException {

    MetaAlertCreateRequest request = new MetaAlertCreateRequest();
    alerts.ifPresent(theAlerts -> request.setAlerts(getRequest(theAlerts)));

    // transform each input message to a Document that can be indexed by the UpdateDao
    for(Map<String, Object> msg: messages) {



      {{
        setAlerts(new ArrayList<GetRequest>() {{
          add(new GetRequest("message_1", SENSOR_NAME));
          add(new GetRequest("message_2", SENSOR_NAME, getTestIndexFullName()));
        }});
        setGroups(Collections.singletonList("group"));
      }};

      MetaAlertCreateRequest create = new MetaAlertCreateRequest();
      create.setAlerts();
      updateDao.createMetaAlert(createDocument(msg, collection), Optional.of(collection));
    }

    try {
      Thread.sleep(2000);
    } catch(Exception e) {
    }
    // TODO do we need to wait until the documents are visible?
//
//    checkUpdateResponse(solr.add(collection, solrInputDocuments));
//    // Make sure to commit so things show up
//    checkUpdateResponse(solr.commit(true, true));
  }

  /**
   * Create an indexable Document from a JSON message.
   *
   * @param message The message that needs indexed.
   * @param docType The document type to write.
   * @return The {@link Document} that was written.
   * @throws IOException
   */
  private static Document createDocument(Map<String, Object> message, String docType) throws IOException {
    Long timestamp = ConversionUtils.convert(message.get("timestamp"), Long.class);
    String guid = (String) message.get("guid");
    return new Document(message, guid, docType, timestamp);
  }

  protected void checkUpdateResponse(UpdateResponse result) throws IOException {
    if (result.getStatus() != 0) {
      throw new IOException("Response error received while adding documents: " + result);
    }
  }
}
