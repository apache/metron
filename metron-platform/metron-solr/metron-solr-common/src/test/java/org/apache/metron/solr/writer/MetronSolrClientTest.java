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

import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.CollectionParams;
import org.apache.solr.common.util.NamedList;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MetronSolrClientTest {

  static class CollectionRequestMatcher implements ArgumentMatcher<QueryRequest> {

    private String name;

    public CollectionRequestMatcher(String name) {
      this.name = name;
    }

    @Override
    public boolean matches(QueryRequest queryRequest) {
      return name.equals(queryRequest.getParams().get("action"));
    }

    @Override
    public String toString() {
        return name;
    }
  }

  @Test
  public void testClient() throws Exception {

    final String collection = "metron";
    String zookeeperUrl = "zookeeperUrl";
    MetronSolrClient metronSolrClient = Mockito.spy(new MetronSolrClient(zookeeperUrl));

    Mockito.doReturn(new NamedList<Object>() {{
      add("collections", new ArrayList<String>() {{
        add(collection);
      }});
    }}).when(metronSolrClient).request(argThat(new CollectionRequestMatcher(CollectionParams.CollectionAction.LIST.name())), isNull());
    metronSolrClient.createCollection(collection, 1, 1);
    verify(metronSolrClient, times(1)).request(argThat(new CollectionRequestMatcher(CollectionParams.CollectionAction.LIST.name())), isNull());
    verify(metronSolrClient, times(0)).request(argThat(new CollectionRequestMatcher(CollectionParams.CollectionAction.CREATE.name())), isNull());

    metronSolrClient = Mockito.spy(new MetronSolrClient(zookeeperUrl));
    Mockito.doReturn(new NamedList<Object>() {{
      add("collections", new ArrayList<String>());
    }}).when(metronSolrClient).request(argThat(new CollectionRequestMatcher(CollectionParams.CollectionAction.LIST.name())), isNull());
    Mockito.doReturn(new NamedList<>()).when(metronSolrClient).request(argThat(new CollectionRequestMatcher(CollectionParams.CollectionAction.CREATE.name())), isNull());
    metronSolrClient.createCollection(collection, 1, 1);
    verify(metronSolrClient, times(1)).request(argThat(new CollectionRequestMatcher(CollectionParams.CollectionAction.LIST.name())), isNull());
    verify(metronSolrClient, times(1)).request(argThat(new CollectionRequestMatcher(CollectionParams.CollectionAction.CREATE.name())), isNull());
  }
}
