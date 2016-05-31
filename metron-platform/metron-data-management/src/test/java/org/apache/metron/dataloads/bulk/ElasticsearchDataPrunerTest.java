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

import com.carrotsearch.hppc.ObjectObjectHashMap;
import org.apache.commons.collections.IteratorUtils;
import org.apache.metron.TestConstants;
import org.apache.metron.common.configuration.Configuration;
import org.easymock.EasyMock;
import org.elasticsearch.action.*;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequestBuilder;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.alias.exists.AliasesExistRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.exists.AliasesExistResponse;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequest;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequestBuilder;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequest;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequestBuilder;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheResponse;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.close.CloseIndexResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsResponse;
import org.elasticsearch.action.admin.indices.flush.*;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequestBuilder;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.get.*;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.open.OpenIndexResponse;
import org.elasticsearch.action.admin.indices.recovery.RecoveryRequest;
import org.elasticsearch.action.admin.indices.recovery.RecoveryRequestBuilder;
import org.elasticsearch.action.admin.indices.recovery.RecoveryResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.admin.indices.segments.IndicesSegmentResponse;
import org.elasticsearch.action.admin.indices.segments.IndicesSegmentsRequest;
import org.elasticsearch.action.admin.indices.segments.IndicesSegmentsRequestBuilder;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequestBuilder;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequestBuilder;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsResponse;
import org.elasticsearch.action.admin.indices.shards.IndicesShardStoreRequestBuilder;
import org.elasticsearch.action.admin.indices.shards.IndicesShardStoresRequest;
import org.elasticsearch.action.admin.indices.shards.IndicesShardStoresResponse;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequestBuilder;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequestBuilder;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateResponse;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesRequest;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesRequestBuilder;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequestBuilder;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.action.admin.indices.upgrade.get.UpgradeStatusRequest;
import org.elasticsearch.action.admin.indices.upgrade.get.UpgradeStatusRequestBuilder;
import org.elasticsearch.action.admin.indices.upgrade.get.UpgradeStatusResponse;
import org.elasticsearch.action.admin.indices.upgrade.post.UpgradeRequest;
import org.elasticsearch.action.admin.indices.upgrade.post.UpgradeRequestBuilder;
import org.elasticsearch.action.admin.indices.upgrade.post.UpgradeResponse;
import org.elasticsearch.action.admin.indices.validate.query.ValidateQueryRequest;
import org.elasticsearch.action.admin.indices.validate.query.ValidateQueryRequestBuilder;
import org.elasticsearch.action.admin.indices.validate.query.ValidateQueryResponse;
import org.elasticsearch.action.admin.indices.warmer.delete.DeleteWarmerRequest;
import org.elasticsearch.action.admin.indices.warmer.delete.DeleteWarmerRequestBuilder;
import org.elasticsearch.action.admin.indices.warmer.delete.DeleteWarmerResponse;
import org.elasticsearch.action.admin.indices.warmer.get.GetWarmersRequest;
import org.elasticsearch.action.admin.indices.warmer.get.GetWarmersRequestBuilder;
import org.elasticsearch.action.admin.indices.warmer.get.GetWarmersResponse;
import org.elasticsearch.action.admin.indices.warmer.put.PutWarmerRequest;
import org.elasticsearch.action.admin.indices.warmer.put.PutWarmerRequestBuilder;
import org.elasticsearch.action.admin.indices.warmer.put.PutWarmerResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.threadpool.ThreadPool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    private IndicesAdminClient indicesAdminClient = new TestIndicesAdminClient();
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
        when(deleteIndexRequestBuilder.request()).thenReturn(deleteIndexRequest);
        when(deleteIndexAction.actionGet()).thenReturn(deleteIndexResponse);

        File resourceFile = new File(TestConstants.SAMPLE_CONFIG_PATH);
        Path resourcePath = Paths.get(resourceFile.getCanonicalPath());

        configuration = new Configuration(resourcePath);

        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();

        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));

    }

    @Test(expected = IndexNotFoundException.class)
    public void testWillThrowOnMissingIndex() throws Exception {

        ((TestIndicesAdminClient)indicesAdminClient).throwMissingIndex = true;
        ElasticsearchDataPruner pruner = new ElasticsearchDataPruner(testDate, 30, configuration, indexClient,"*");
        pruner.deleteIndex(adminClient, "baz");
        ((TestIndicesAdminClient)indicesAdminClient).throwMissingIndex = false;

    }

    @Test
    public void testDeletesCorrectIndexes() throws Exception {

        //Mock Cluster Admin
        ClusterAdminClient clusterAdminClient = mock(ClusterAdminClient.class);
        ClusterStateRequestBuilder clusterStateRequestBuilder = mock(ClusterStateRequestBuilder.class);
        ClusterStateResponse clusterStateResponse = mock(ClusterStateResponse.class);
        ClusterState clusterState = mock(ClusterState.class);
        ObjectObjectHashMap<String, IndexMetaData> clusterIndexes = new ObjectObjectHashMap();
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

        ObjectObjectHashMap<String, IndexMetaData> indexNames = new ObjectObjectHashMap();
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

    class TestIndicesAdminClient implements IndicesAdminClient {

        public boolean throwMissingIndex = false;

        @Override
        public ActionFuture<DeleteIndexResponse> delete(DeleteIndexRequest request) {

            if(throwMissingIndex){

                throw new IndexNotFoundException("TEST EXCEPTION!");

            }

            return deleteIndexAction;

        }


        @Override
        public ActionFuture<IndicesExistsResponse> exists(IndicesExistsRequest request) {
            return null;
        }

        @Override
        public void exists(IndicesExistsRequest request, ActionListener<IndicesExistsResponse> listener) {

        }

        @Override
        public IndicesExistsRequestBuilder prepareExists(String... indices) {
            return null;
        }

        @Override
        public ActionFuture<TypesExistsResponse> typesExists(TypesExistsRequest request) {
            return null;
        }

        @Override
        public void typesExists(TypesExistsRequest request, ActionListener<TypesExistsResponse> listener) {

        }

        @Override
        public TypesExistsRequestBuilder prepareTypesExists(String... index) {
            return null;
        }

        @Override
        public ActionFuture<IndicesStatsResponse> stats(IndicesStatsRequest request) {
            return null;
        }

        @Override
        public void stats(IndicesStatsRequest request, ActionListener<IndicesStatsResponse> listener) {

        }

        @Override
        public IndicesStatsRequestBuilder prepareStats(String... indices) {
            return null;
        }

        @Override
        public ActionFuture<RecoveryResponse> recoveries(RecoveryRequest request) {
            return null;
        }

        @Override
        public void recoveries(RecoveryRequest request, ActionListener<RecoveryResponse> listener) {

        }

        @Override
        public RecoveryRequestBuilder prepareRecoveries(String... indices) {
            return null;
        }

        @Override
        public ActionFuture<IndicesSegmentResponse> segments(IndicesSegmentsRequest request) {
            return null;
        }

        @Override
        public void segments(IndicesSegmentsRequest request, ActionListener<IndicesSegmentResponse> listener) {

        }

        @Override
        public IndicesSegmentsRequestBuilder prepareSegments(String... indices) {
            return null;
        }

        @Override
        public ActionFuture<IndicesShardStoresResponse> shardStores(IndicesShardStoresRequest request) {
            return null;
        }

        @Override
        public void shardStores(IndicesShardStoresRequest request, ActionListener<IndicesShardStoresResponse> listener) {

        }

        @Override
        public IndicesShardStoreRequestBuilder prepareShardStores(String... indices) {
            return null;
        }

        @Override
        public ActionFuture<CreateIndexResponse> create(CreateIndexRequest request) {
            return null;
        }

        @Override
        public void create(CreateIndexRequest request, ActionListener<CreateIndexResponse> listener) {

        }

        @Override
        public CreateIndexRequestBuilder prepareCreate(String index) {
            return null;
        }


        @Override
        public void delete(DeleteIndexRequest request, ActionListener<DeleteIndexResponse> listener) {

        }

        @Override
        public DeleteIndexRequestBuilder prepareDelete(String... indices) {
            return deleteIndexRequestBuilder;
        }

        @Override
        public ActionFuture<CloseIndexResponse> close(CloseIndexRequest request) {
            return null;
        }

        @Override
        public void close(CloseIndexRequest request, ActionListener<CloseIndexResponse> listener) {

        }

        @Override
        public CloseIndexRequestBuilder prepareClose(String... indices) {
            return null;
        }

        @Override
        public ActionFuture<OpenIndexResponse> open(OpenIndexRequest request) {
            return null;
        }

        @Override
        public void open(OpenIndexRequest request, ActionListener<OpenIndexResponse> listener) {

        }

        @Override
        public OpenIndexRequestBuilder prepareOpen(String... indices) {
            return null;
        }

        @Override
        public ActionFuture<RefreshResponse> refresh(RefreshRequest request) {
            return null;
        }

        @Override
        public void refresh(RefreshRequest request, ActionListener<RefreshResponse> listener) {

        }

        @Override
        public RefreshRequestBuilder prepareRefresh(String... indices) {
            return null;
        }

        @Override
        public ActionFuture<FlushResponse> flush(FlushRequest request) {
            return null;
        }

        @Override
        public void flush(FlushRequest request, ActionListener<FlushResponse> listener) {

        }

        @Override
        public FlushRequestBuilder prepareFlush(String... indices) {
            return null;
        }

        @Override
        public ActionFuture<SyncedFlushResponse> syncedFlush(SyncedFlushRequest request) {
            return null;
        }

        @Override
        public void syncedFlush(SyncedFlushRequest request, ActionListener<SyncedFlushResponse> listener) {

        }

        @Override
        public SyncedFlushRequestBuilder prepareSyncedFlush(String... indices) {
            return null;
        }

        @Override
        public ActionFuture<ForceMergeResponse> forceMerge(ForceMergeRequest request) {
            return null;
        }

        @Override
        public void forceMerge(ForceMergeRequest request, ActionListener<ForceMergeResponse> listener) {

        }

        @Override
        public ForceMergeRequestBuilder prepareForceMerge(String... indices) {
            return null;
        }

        @Override
        public ActionFuture<UpgradeResponse> upgrade(UpgradeRequest request) {
            return null;
        }

        @Override
        public void upgrade(UpgradeRequest request, ActionListener<UpgradeResponse> listener) {

        }

        @Override
        public UpgradeStatusRequestBuilder prepareUpgradeStatus(String... indices) {
            return null;
        }

        @Override
        public ActionFuture<UpgradeStatusResponse> upgradeStatus(UpgradeStatusRequest request) {
            return null;
        }

        @Override
        public void upgradeStatus(UpgradeStatusRequest request, ActionListener<UpgradeStatusResponse> listener) {

        }

        @Override
        public UpgradeRequestBuilder prepareUpgrade(String... indices) {
            return null;
        }

        @Override
        public void getMappings(GetMappingsRequest request, ActionListener<GetMappingsResponse> listener) {

        }

        @Override
        public ActionFuture<GetMappingsResponse> getMappings(GetMappingsRequest request) {
            return null;
        }

        @Override
        public GetMappingsRequestBuilder prepareGetMappings(String... indices) {
            return null;
        }

        @Override
        public void getFieldMappings(GetFieldMappingsRequest request, ActionListener<GetFieldMappingsResponse> listener) {

        }

        @Override
        public GetFieldMappingsRequestBuilder prepareGetFieldMappings(String... indices) {
            return null;
        }

        @Override
        public ActionFuture<GetFieldMappingsResponse> getFieldMappings(GetFieldMappingsRequest request) {
            return null;
        }

        @Override
        public ActionFuture<PutMappingResponse> putMapping(PutMappingRequest request) {
            return null;
        }

        @Override
        public void putMapping(PutMappingRequest request, ActionListener<PutMappingResponse> listener) {

        }

        @Override
        public PutMappingRequestBuilder preparePutMapping(String... indices) {
            return null;
        }

        @Override
        public ActionFuture<IndicesAliasesResponse> aliases(IndicesAliasesRequest request) {
            return null;
        }

        @Override
        public void aliases(IndicesAliasesRequest request, ActionListener<IndicesAliasesResponse> listener) {

        }

        @Override
        public IndicesAliasesRequestBuilder prepareAliases() {
            return null;
        }

        @Override
        public ActionFuture<GetAliasesResponse> getAliases(GetAliasesRequest request) {
            return null;
        }

        @Override
        public void getAliases(GetAliasesRequest request, ActionListener<GetAliasesResponse> listener) {

        }

        @Override
        public GetAliasesRequestBuilder prepareGetAliases(String... aliases) {
            return null;
        }

        @Override
        public AliasesExistRequestBuilder prepareAliasesExist(String... aliases) {
            return null;
        }

        @Override
        public ActionFuture<AliasesExistResponse> aliasesExist(GetAliasesRequest request) {
            return null;
        }

        @Override
        public void aliasesExist(GetAliasesRequest request, ActionListener<AliasesExistResponse> listener) {

        }

        @Override
        public ActionFuture<GetIndexResponse> getIndex(GetIndexRequest request) {
            return null;
        }

        @Override
        public void getIndex(GetIndexRequest request, ActionListener<GetIndexResponse> listener) {

        }

        @Override
        public GetIndexRequestBuilder prepareGetIndex() {
            return null;
        }

        @Override
        public ActionFuture<ClearIndicesCacheResponse> clearCache(ClearIndicesCacheRequest request) {
            return null;
        }

        @Override
        public void clearCache(ClearIndicesCacheRequest request, ActionListener<ClearIndicesCacheResponse> listener) {

        }

        @Override
        public ClearIndicesCacheRequestBuilder prepareClearCache(String... indices) {
            return null;
        }

        @Override
        public ActionFuture<UpdateSettingsResponse> updateSettings(UpdateSettingsRequest request) {
            return null;
        }

        @Override
        public void updateSettings(UpdateSettingsRequest request, ActionListener<UpdateSettingsResponse> listener) {

        }

        @Override
        public UpdateSettingsRequestBuilder prepareUpdateSettings(String... indices) {
            return null;
        }

        @Override
        public ActionFuture<AnalyzeResponse> analyze(AnalyzeRequest request) {
            return null;
        }

        @Override
        public void analyze(AnalyzeRequest request, ActionListener<AnalyzeResponse> listener) {

        }

        @Override
        public AnalyzeRequestBuilder prepareAnalyze(@Nullable String index, String text) {
            return null;
        }

        @Override
        public AnalyzeRequestBuilder prepareAnalyze(String text) {
            return null;
        }

        @Override
        public AnalyzeRequestBuilder prepareAnalyze() {
            return null;
        }

        @Override
        public ActionFuture<PutIndexTemplateResponse> putTemplate(PutIndexTemplateRequest request) {
            return null;
        }

        @Override
        public void putTemplate(PutIndexTemplateRequest request, ActionListener<PutIndexTemplateResponse> listener) {

        }

        @Override
        public PutIndexTemplateRequestBuilder preparePutTemplate(String name) {
            return null;
        }

        @Override
        public ActionFuture<DeleteIndexTemplateResponse> deleteTemplate(DeleteIndexTemplateRequest request) {
            return null;
        }

        @Override
        public void deleteTemplate(DeleteIndexTemplateRequest request, ActionListener<DeleteIndexTemplateResponse> listener) {

        }

        @Override
        public DeleteIndexTemplateRequestBuilder prepareDeleteTemplate(String name) {
            return null;
        }

        @Override
        public ActionFuture<GetIndexTemplatesResponse> getTemplates(GetIndexTemplatesRequest request) {
            return null;
        }

        @Override
        public void getTemplates(GetIndexTemplatesRequest request, ActionListener<GetIndexTemplatesResponse> listener) {

        }

        @Override
        public GetIndexTemplatesRequestBuilder prepareGetTemplates(String... name) {
            return null;
        }

        @Override
        public ActionFuture<ValidateQueryResponse> validateQuery(ValidateQueryRequest request) {
            return null;
        }

        @Override
        public void validateQuery(ValidateQueryRequest request, ActionListener<ValidateQueryResponse> listener) {

        }

        @Override
        public ValidateQueryRequestBuilder prepareValidateQuery(String... indices) {
            return null;
        }

        @Override
        public ActionFuture<PutWarmerResponse> putWarmer(PutWarmerRequest request) {
            return null;
        }

        @Override
        public void putWarmer(PutWarmerRequest request, ActionListener<PutWarmerResponse> listener) {

        }

        @Override
        public PutWarmerRequestBuilder preparePutWarmer(String name) {
            return null;
        }

        @Override
        public ActionFuture<DeleteWarmerResponse> deleteWarmer(DeleteWarmerRequest request) {
            return null;
        }

        @Override
        public void deleteWarmer(DeleteWarmerRequest request, ActionListener<DeleteWarmerResponse> listener) {

        }

        @Override
        public DeleteWarmerRequestBuilder prepareDeleteWarmer() {
            return null;
        }

        @Override
        public void getWarmers(GetWarmersRequest request, ActionListener<GetWarmersResponse> listener) {

        }

        @Override
        public ActionFuture<GetWarmersResponse> getWarmers(GetWarmersRequest request) {
            return null;
        }

        @Override
        public GetWarmersRequestBuilder prepareGetWarmers(String... indices) {
            return null;
        }

        @Override
        public void getSettings(GetSettingsRequest request, ActionListener<GetSettingsResponse> listener) {

        }

        @Override
        public ActionFuture<GetSettingsResponse> getSettings(GetSettingsRequest request) {
            return null;
        }

        @Override
        public GetSettingsRequestBuilder prepareGetSettings(String... indices) {
            return null;
        }

        @Override
        public <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder>> ActionFuture<Response> execute(Action<Request, Response, RequestBuilder> action, Request request) {
            return null;
        }

        @Override
        public <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder>> void execute(Action<Request, Response, RequestBuilder> action, Request request, ActionListener<Response> listener) {

        }

        @Override
        public <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder>> RequestBuilder prepareExecute(Action<Request, Response, RequestBuilder> action) {
            return null;
        }

        @Override
        public ThreadPool threadPool() {
            return null;
        }
    }

}
