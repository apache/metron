/*
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

package org.apache.metron.elasticsearch.writer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.storm.tuple.Tuple;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.junit.Test;

public class ElasticsearchWriterTest {
    @Test
    public void testSingleSuccesses() throws Exception {
        Tuple tuple1 = mock(Tuple.class);

        BulkResponse response = mock(BulkResponse.class);
        when(response.hasFailures()).thenReturn(false);

        BulkWriterResponse expected = new BulkWriterResponse();
        expected.addSuccess(tuple1);

        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        BulkWriterResponse actual = esWriter.buildWriteReponse(ImmutableList.of(tuple1), response);

        assertEquals("Response should have no errors and single success", expected, actual);
    }

    @Test
    public void testMultipleSuccesses() throws Exception {
        Tuple tuple1 = mock(Tuple.class);
        Tuple tuple2 = mock(Tuple.class);

        BulkResponse response = mock(BulkResponse.class);
        when(response.hasFailures()).thenReturn(false);

        BulkWriterResponse expected = new BulkWriterResponse();
        expected.addSuccess(tuple1);
        expected.addSuccess(tuple2);

        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        BulkWriterResponse actual = esWriter.buildWriteReponse(ImmutableList.of(tuple1, tuple2), response);

        assertEquals("Response should have no errors and two successes", expected, actual);
    }

    @Test
    public void testSingleFailure() throws Exception {
        Tuple tuple1 = mock(Tuple.class);

        BulkResponse response = mock(BulkResponse.class);
        when(response.hasFailures()).thenReturn(true);

        Exception e = new IllegalStateException();
        BulkItemResponse itemResponse = buildBulkItemFailure(e);
        when(response.iterator()).thenReturn(ImmutableList.of(itemResponse).iterator());

        BulkWriterResponse expected = new BulkWriterResponse();
        expected.addError(e, tuple1);

        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        BulkWriterResponse actual = esWriter.buildWriteReponse(ImmutableList.of(tuple1), response);

        assertEquals("Response should have one error and zero successes", expected, actual);
    }

    @Test
    public void testTwoSameFailure() throws Exception {
        Tuple tuple1 = mock(Tuple.class);
        Tuple tuple2 = mock(Tuple.class);

        BulkResponse response = mock(BulkResponse.class);
        when(response.hasFailures()).thenReturn(true);

        Exception e = new IllegalStateException();

        BulkItemResponse itemResponse = buildBulkItemFailure(e);
        BulkItemResponse itemResponse2 = buildBulkItemFailure(e);

        when(response.iterator()).thenReturn(ImmutableList.of(itemResponse, itemResponse2).iterator());

        BulkWriterResponse expected = new BulkWriterResponse();
        expected.addError(e, tuple1);
        expected.addError(e, tuple2);

        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        BulkWriterResponse actual = esWriter.buildWriteReponse(ImmutableList.of(tuple1, tuple2), response);

        assertEquals("Response should have two errors and no successes", expected, actual);

        // Ensure the errors actually get collapsed together
        Map<Throwable, Collection<Tuple>> actualErrors = actual.getErrors();
        HashMap<Throwable, Collection<Tuple>> expectedErrors = new HashMap<>();
        expectedErrors.put(e, ImmutableList.of(tuple1, tuple2));
        assertEquals("Errors should have collapsed together", expectedErrors, actualErrors);
    }

    @Test
    public void testTwoDifferentFailure() throws Exception {
        Tuple tuple1 = mock(Tuple.class);
        Tuple tuple2 = mock(Tuple.class);

        BulkResponse response = mock(BulkResponse.class);
        when(response.hasFailures()).thenReturn(true);

        Exception e = new IllegalStateException("Cause");
        Exception e2 = new IllegalStateException("Different Cause");
        BulkItemResponse itemResponse = buildBulkItemFailure(e);
        BulkItemResponse itemResponse2 = buildBulkItemFailure(e2);

        when(response.iterator()).thenReturn(ImmutableList.of(itemResponse, itemResponse2).iterator());

        BulkWriterResponse expected = new BulkWriterResponse();
        expected.addError(e, tuple1);
        expected.addError(e2, tuple2);

        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        BulkWriterResponse actual = esWriter.buildWriteReponse(ImmutableList.of(tuple1, tuple2), response);

        assertEquals("Response should have two errors and no successes", expected, actual);

        // Ensure the errors did not get collapsed together
        Map<Throwable, Collection<Tuple>> actualErrors = actual.getErrors();
        HashMap<Throwable, Collection<Tuple>> expectedErrors = new HashMap<>();
        expectedErrors.put(e, ImmutableList.of(tuple1));
        expectedErrors.put(e2, ImmutableList.of(tuple2));
        assertEquals("Errors should not have collapsed together", expectedErrors, actualErrors);
    }

    @Test
    public void testSuccessAndFailure() throws Exception {
        Tuple tuple1 = mock(Tuple.class);
        Tuple tuple2 = mock(Tuple.class);

        BulkResponse response = mock(BulkResponse.class);
        when(response.hasFailures()).thenReturn(true);

        Exception e = new IllegalStateException("Cause");
        BulkItemResponse itemResponse = buildBulkItemFailure(e);

        BulkItemResponse itemResponse2 = mock(BulkItemResponse.class);
        when(itemResponse2.isFailed()).thenReturn(false);

        when(response.iterator()).thenReturn(ImmutableList.of(itemResponse, itemResponse2).iterator());

        BulkWriterResponse expected = new BulkWriterResponse();
        expected.addError(e, tuple1);
        expected.addSuccess(tuple2);

        ElasticsearchWriter esWriter = new ElasticsearchWriter();
        BulkWriterResponse actual = esWriter.buildWriteReponse(ImmutableList.of(tuple1, tuple2), response);

        assertEquals("Response should have one error and one success", expected, actual);
    }

    private BulkItemResponse buildBulkItemFailure(Exception e) {
        BulkItemResponse itemResponse = mock(BulkItemResponse.class);
        when(itemResponse.isFailed()).thenReturn(true);
        BulkItemResponse.Failure failure = mock(BulkItemResponse.Failure.class);
        when(itemResponse.getFailure()).thenReturn(failure);
        when(failure.getCause()).thenReturn(e);
        return itemResponse;
    }
}
