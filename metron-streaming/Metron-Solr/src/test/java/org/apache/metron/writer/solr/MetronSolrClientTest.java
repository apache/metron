/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.writer.solr;

import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.CollectionParams;
import org.apache.solr.common.util.NamedList;
import org.hamcrest.Description;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MetronSolrClientTest {

  class CollectionRequestMatcher extends ArgumentMatcher<QueryRequest> {

    private String name;

    public CollectionRequestMatcher(String name) {
      this.name = name;
    }

    @Override
    public boolean matches(Object o) {
      QueryRequest queryRequest = (QueryRequest) o;
      return name.equals(queryRequest.getParams().get("action"));
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(name);
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
    }}).when(metronSolrClient).request(argThat(new CollectionRequestMatcher(CollectionParams.CollectionAction.LIST.name())), (String) isNull());
    metronSolrClient.createCollection(collection, 1, 1);
    verify(metronSolrClient, times(1)).request(argThat(new CollectionRequestMatcher(CollectionParams.CollectionAction.LIST.name())), (String) isNull());
    verify(metronSolrClient, times(0)).request(argThat(new CollectionRequestMatcher(CollectionParams.CollectionAction.CREATE.name())), (String) isNull());

    metronSolrClient = Mockito.spy(new MetronSolrClient(zookeeperUrl));
    Mockito.doReturn(new NamedList<Object>() {{
      add("collections", new ArrayList<String>());
    }}).when(metronSolrClient).request(argThat(new CollectionRequestMatcher(CollectionParams.CollectionAction.LIST.name())), (String) isNull());
    Mockito.doReturn(new NamedList<>()).when(metronSolrClient).request(argThat(new CollectionRequestMatcher(CollectionParams.CollectionAction.CREATE.name())), (String) isNull());
    metronSolrClient.createCollection(collection, 1, 1);
    verify(metronSolrClient, times(1)).request(argThat(new CollectionRequestMatcher(CollectionParams.CollectionAction.LIST.name())), (String) isNull());
    verify(metronSolrClient, times(1)).request(argThat(new CollectionRequestMatcher(CollectionParams.CollectionAction.CREATE.name())), (String) isNull());
  }
}
