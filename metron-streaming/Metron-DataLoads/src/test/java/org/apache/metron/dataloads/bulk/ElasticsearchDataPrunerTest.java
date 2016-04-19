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
package org.apache.metron.dataloads.bulk;

import org.apache.commons.collections.IteratorUtils;
import org.apache.metron.domain.Configuration;
import org.easymock.EasyMock;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequestBuilder;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.hppc.ObjectObjectOpenHashMap;
import org.elasticsearch.index.Index;
import org.elasticsearch.indices.IndexMissingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DeleteIndexResponse.class)
public class ElasticsearchDataPrunerTest {

    private Date testDate;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH");
    private Configuration configuration;

    private Client indexClient = mock(Client.class);
    private AdminClient adminClient = mock(AdminClient.class);
    private IndicesAdminClient indicesAdminClient = mock(FilterClient.IndicesAdmin.class);
    private DeleteIndexRequestBuilder deleteIndexRequestBuilder = mock(DeleteIndexRequestBuilder.class);
    private DeleteIndexRequest deleteIndexRequest = mock(DeleteIndexRequest.class);
    private ActionFuture<DeleteIndexResponse> deleteIndexAction = mock(ActionFuture.class);
    private DeleteIndexResponse deleteIndexResponse = PowerMock.createMock(DeleteIndexResponse.class);


    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;

    @Before
    public void setUp() throws Exception {

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, Calendar.MARCH);
        calendar.set(Calendar.YEAR, 2016);
        calendar.set(Calendar.DATE, 31);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND,0);
        testDate = calendar.getTime();

        when(indexClient.admin()).thenReturn(adminClient);
        when(adminClient.indices()).thenReturn(indicesAdminClient);
        when(indicesAdminClient.prepareDelete(Matchers.<String>anyVararg())).thenReturn(deleteIndexRequestBuilder);
        when(indicesAdminClient.delete((DeleteIndexRequest) any())).thenReturn(deleteIndexAction);
        when(deleteIndexRequestBuilder.request()).thenReturn(deleteIndexRequest);
        when(deleteIndexAction.actionGet()).thenReturn(deleteIndexResponse);

        File resourceFile = new File("../Metron-Testing/src/main/resources/sample/config/");
        Path resourcePath = Paths.get(resourceFile.getCanonicalPath());

        configuration = new Configuration(resourcePath);

        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();

        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));

    }

    @Test(expected = IndexMissingException.class)
    public void testWillThrowOnMissingIndex() throws Exception {

        when(indicesAdminClient.delete((DeleteIndexRequest) any())).thenThrow(new IndexMissingException(new Index("Test Exception")));
        ElasticsearchDataPruner pruner = new ElasticsearchDataPruner(testDate, 30, configuration, indexClient,"*");
        pruner.deleteIndex(adminClient, "baz");

    }

    @Test
    public void testDeletesCorrectIndexes() throws Exception {

        //Mock Cluster Admin
        ClusterAdminClient clusterAdminClient = mock(ClusterAdminClient.class);
        ClusterStateRequestBuilder clusterStateRequestBuilder = mock(ClusterStateRequestBuilder.class);
        ClusterStateResponse clusterStateResponse = mock(ClusterStateResponse.class);
        ClusterState clusterState = mock(ClusterState.class);
        ObjectObjectOpenHashMap<String, IndexMetaData> clusterIndexes = new ObjectObjectOpenHashMap();
        MetaData clusterMetadata = mock(MetaData.class);
        when(adminClient.cluster()).thenReturn(clusterAdminClient);
        when(clusterAdminClient.prepareState()).thenReturn(clusterStateRequestBuilder);
        when(clusterStateRequestBuilder.get()).thenReturn(clusterStateResponse);
        when(clusterStateResponse.getState()).thenReturn(clusterState);
        when(clusterState.getMetaData()).thenReturn(clusterMetadata);

        int numDays = 5;

        Date indexDate = new Date();

        indexDate.setTime(testDate.getTime() - TimeUnit.DAYS.toMillis(numDays));

        for (int i = 0; i < numDays * 24; i++) {

            String indexName = "sensor_index_" + dateFormat.format(indexDate);
            clusterIndexes.put(indexName, null);
            indexDate.setTime(indexDate.getTime() + TimeUnit.HOURS.toMillis(1));

        }

        when(clusterMetadata.getIndices()).thenReturn(ImmutableOpenMap.copyOf(clusterIndexes));


        EasyMock.expect(deleteIndexResponse.isAcknowledged()).andReturn(true);

        replayAll();
        ElasticsearchDataPruner pruner = new ElasticsearchDataPruner(testDate, 1, configuration, indexClient, "sensor_index_");
        pruner.indexClient = indexClient;
        Long deleteCount = pruner.prune();
        assertEquals("Should have pruned 24 indices", 24L, deleteCount.longValue());
        verifyAll();

    }

    @Test
    public void testFilter() throws Exception {

        ObjectObjectOpenHashMap<String, IndexMetaData> indexNames = new ObjectObjectOpenHashMap();
        SimpleDateFormat dateChecker = new SimpleDateFormat("yyyyMMdd");
        int numDays = 5;
        String[] expectedIndices = new String[24];
        Date indexDate = new Date();

        indexDate.setTime(testDate.getTime() - TimeUnit.DAYS.toMillis(numDays));

        for (int i = 0, j=0; i < numDays * 24; i++) {

            String indexName = "sensor_index_" + dateFormat.format(indexDate);
            //Delete 20160330
            if( dateChecker.format(indexDate).equals("20160330") ){
                expectedIndices[j++] = indexName;
            }

            indexNames.put(indexName, null);
            indexDate.setTime(indexDate.getTime() + TimeUnit.HOURS.toMillis(1));

        }

        ImmutableOpenMap<String, IndexMetaData> testIndices = ImmutableOpenMap.copyOf(indexNames);

        ElasticsearchDataPruner pruner = new ElasticsearchDataPruner(testDate, 1, configuration,  indexClient, "sensor_index_");
        pruner.indexClient = indexClient;

        Iterable<String> filteredIndices = pruner.getFilteredIndices(testIndices);

        Object[] indexArray = IteratorUtils.toArray(filteredIndices.iterator());
        Arrays.sort(indexArray);
        Arrays.sort(expectedIndices);

        assertArrayEquals(expectedIndices,indexArray);

    }

}
