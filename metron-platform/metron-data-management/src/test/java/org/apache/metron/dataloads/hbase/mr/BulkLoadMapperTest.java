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
package org.apache.metron.dataloads.hbase.mr;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.HbaseConverter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BulkLoadMapperTest {
    /**
     * {
     *   "config": {
     *     "columns":{
     *       "host": 0,
     *       "meta": 2
     *     },
     *     "indicator_column": "host",
     *     "type": "threat",
     *     "separator": ","
     *   },
     *   "extractor": "CSV"
     * }
     */
    @Multiline
    private static String extractorConfig;

    private Configuration config;

    @Before
    public void setup() {
        config = new Configuration() {{
            set(BulkLoadMapper.COLUMN_FAMILY_KEY, "cf");
            set(BulkLoadMapper.CONFIG_KEY, extractorConfig);
            set(BulkLoadMapper.LAST_SEEN_KEY, "0");
            set(BulkLoadMapper.CONVERTER_KEY, EnrichmentConverter.class.getName());
        }};
    }

    @Test
    public void testSetup() throws IOException, InterruptedException {
        final String expectedConverter = EnrichmentConverter.class.getName();
        final String expectedColumnFamily = "columnFamily";
        config = new Configuration() {{
            set(BulkLoadMapper.COLUMN_FAMILY_KEY, expectedColumnFamily);
            set(BulkLoadMapper.CONFIG_KEY, extractorConfig);
            set(BulkLoadMapper.LAST_SEEN_KEY, "0");
            set(BulkLoadMapper.CONVERTER_KEY, expectedConverter);
        }};

        // need the Context to return the configuration
        Mapper.Context context = mock(Mapper.Context.class);
        when(context.getConfiguration()).thenReturn(config);

        // setup the mapper
        BulkLoadMapper mapper = new BulkLoadMapper();
        mapper.setup(context);

        // ensure setup was successful
        Assert.assertNotNull(mapper.getConverter());
        Assert.assertEquals(expectedConverter, mapper.getConverter().getClass().getName());
        Assert.assertNotNull(mapper.getExtractor());
        Assert.assertEquals(expectedColumnFamily, mapper.getColumnFamily());
    }

    @Test
    public void testLoadCsv() throws IOException, InterruptedException {
        // the converter will return a Put
        Put expectedValue = mock(Put.class);
        HbaseConverter converter = mock(HbaseConverter.class);
        when(converter.toPut(any(), any(), any())).thenReturn(expectedValue);

        // need the Context to return the configuration
        Mapper.Context context = mock(Mapper.Context.class);
        when(context.getConfiguration()).thenReturn(config);

        // use the mapper to load some data
        BulkLoadMapper mapper = new BulkLoadMapper();
        mapper.setup(context);
        mapper.withHBaseConverter(converter);
        mapper.map(new Object(), new Text("google.com,1,foo"), context);

        // the data should have been mapped to the expected Put
        ArgumentCaptor<Object> keyCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(context, times(1)).write(keyCaptor.capture(), valueCaptor.capture());
        Object value = valueCaptor.getValue();
        Assert.assertEquals(expectedValue, value);
    }

    @Test
    public void testLoadCommentedCsv() throws IOException, InterruptedException {
        // the converter will return a Put
        Put expectedValue = mock(Put.class);
        HbaseConverter converter = mock(HbaseConverter.class);
        when(converter.toPut(any(), any(), any())).thenReturn(expectedValue);

        // need the Context to return the configuration
        Mapper.Context context = mock(Mapper.Context.class);
        when(context.getConfiguration()).thenReturn(config);

        // use the mapper to load some data
        BulkLoadMapper mapper = new BulkLoadMapper();
        mapper.setup(context);
        mapper.withHBaseConverter(converter);
        mapper.map(new Object(), new Text("#google.com,1,foo"), context);

        // there is no data to load because it is commented
        verify(context, times(0)).write(any(), any());
    }
}
