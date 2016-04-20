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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.collections.IteratorUtils;
import org.apache.metron.domain.Configuration;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

public class ElasticsearchDataPruner extends DataPruner {

    private String indexPattern;
    private SimpleDateFormat dateFormat;
    protected Client indexClient = null;
    protected Configuration configuration;

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchDataPruner.class);
    private static final String defaultDateFormat = "yyyy.MM.dd.HH";



    private Predicate<String> filterWithRegex = new Predicate<String>() {

        @Override
        public boolean apply(String str) {

            try {
                String dateString = str.substring(indexPattern.length());
                Date indexCreateDate = dateFormat.parse(dateString);
                long indexCreatedDate = indexCreateDate.getTime();
                if (indexCreatedDate >= firstTimeMillis && indexCreatedDate < lastTimeMillis) {
                    return true;
                }
            } catch (ParseException e) {
                LOG.error("Unable to parse date from + " + str.substring(indexPattern.length()), e);
            }

            return false;
        }

    };

    ElasticsearchDataPruner(Date startDate, Integer numDays,Configuration configuration, Client indexClient, String indexPattern) throws Exception {

        super(startDate, numDays, indexPattern);

        this.indexPattern = indexPattern;
        this.dateFormat = new SimpleDateFormat(defaultDateFormat);
        this.configuration = configuration;
        this.indexClient = indexClient;


    }

    @Override
    public Long prune() throws IOException {

        try {

            configuration.update();

        }
        catch(Exception e) {

            LOG.error("Unable to update configs",e);

        }

        String dateString = configuration.getGlobalConfig().get("es.date.format").toString();

        if( null != dateString ){
            dateFormat = new SimpleDateFormat(dateString);
        }

        ImmutableOpenMap<String, IndexMetaData> allIndices = indexClient.admin().cluster().prepareState().get().getState().getMetaData().getIndices();
        Iterable indicesForDeletion = getFilteredIndices(allIndices);
        Object[] indexArray = IteratorUtils.toArray(indicesForDeletion.iterator());

        if(indexArray.length > 0) {
            String[] indexStringArray = new String[indexArray.length];
            System.arraycopy(indexArray, 0, indexStringArray, 0, indexArray.length);
            deleteIndex(indexClient.admin(), indexStringArray);
        }

        return new Long(indexArray.length);

    }

    protected Boolean deleteIndex(AdminClient adminClient, String... index) {

        boolean isAcknowledged = adminClient.indices().delete(adminClient.indices().prepareDelete(index).request()).actionGet().isAcknowledged();
        return new Boolean(isAcknowledged);

    }

    protected Iterable<String> getFilteredIndices(ImmutableOpenMap<String, IndexMetaData> indices) {

        String[] returnedIndices = new String[indices.size()];
        Iterator it = indices.keysIt();
        System.arraycopy(IteratorUtils.toArray(it), 0, returnedIndices, 0, returnedIndices.length);
        Iterable<String> matches = Iterables.filter(Arrays.asList(returnedIndices), filterWithRegex);

        return matches;

    }

}
