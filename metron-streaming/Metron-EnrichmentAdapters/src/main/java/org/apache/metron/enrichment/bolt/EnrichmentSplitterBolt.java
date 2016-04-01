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
package org.apache.metron.enrichment.bolt;

import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import org.apache.metron.Constants;
import org.apache.metron.bolt.SplitBolt;
import org.apache.metron.domain.Enrichment;
import org.apache.metron.enrichment.utils.EnrichmentUtils;
import org.apache.metron.topology.TopologyUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class EnrichmentSplitterBolt extends SplitBolt<JSONObject> {
    protected static final Logger LOG = LoggerFactory.getLogger(EnrichmentSplitterBolt.class);
    private List<Enrichment> enrichments;
    protected String messageFieldName;
    private transient JSONParser parser;


    public EnrichmentSplitterBolt(String zookeeperUrl) {
        super(zookeeperUrl);
    }

    public EnrichmentSplitterBolt withEnrichments(List<Enrichment> enrichments) {
        this.enrichments = enrichments;
        return this;
    }

    public EnrichmentSplitterBolt withMessageFieldName(String messageFieldName) {
        this.messageFieldName = messageFieldName;
        return this;
    }
    @Override
    public void prepare(Map map, TopologyContext topologyContext) {
        parser = new JSONParser();
    }
    @Override
    public String getKey(Tuple tuple, JSONObject message) {
        String key = null;
        try {
            key = tuple.getStringByField("key");
        }
        catch(Throwable t) {
            //swallowing this just in case.
        }
        if(key != null) {
            return key;
        }
        else {
            return UUID.randomUUID().toString();
        }
    }

    @Override
    public JSONObject generateMessage(Tuple tuple) {
        JSONObject message = null;
        if (messageFieldName == null) {
            byte[] data = tuple.getBinary(0);
            try {
                message = (JSONObject) parser.parse(new String(data, "UTF8"));
                message.put(getClass().getSimpleName().toLowerCase() + ".splitter.begin.ts", "" + System.currentTimeMillis());
            } catch (ParseException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            message = (JSONObject) tuple.getValueByField(messageFieldName);
            message.put(getClass().getSimpleName().toLowerCase() + ".splitter.begin.ts", "" + System.currentTimeMillis());
        }
        return message;
    }

    @Override
    public Set<String> getStreamIds() {
        Set<String> streamIds = new HashSet<>();
        for(Enrichment enrichment: enrichments) {
            streamIds.add(enrichment.getType());
        }
        return streamIds;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, JSONObject> splitMessage(JSONObject message) {
        Map<String, JSONObject> streamMessageMap = new HashMap<>();
        String sensorType = TopologyUtils.getSensorType(message);
        Map<String, List<String>> enrichmentFieldMap = getFieldMap(sensorType);
        for (String enrichmentType : enrichmentFieldMap.keySet()) {
            List<String> fields = enrichmentFieldMap.get(enrichmentType);
            JSONObject enrichmentObject = new JSONObject();
            if (fields != null && fields.size() > 0) {
                for (String field : fields) {
                    enrichmentObject.put(getKeyName(enrichmentType, field), message.get(field));
                }
                enrichmentObject.put(Constants.SENSOR_TYPE, sensorType);
                streamMessageMap.put(enrichmentType, enrichmentObject);
            }
        }
        message.put(getClass().getSimpleName().toLowerCase() + ".splitter.end.ts", "" + System.currentTimeMillis());
        return streamMessageMap;
    }

    protected Map<String, List<String>> getFieldMap(String sensorType) {
        return configurations.getSensorEnrichmentConfig(sensorType).getEnrichmentFieldMap();
    }

    protected String getKeyName(String type, String field) {
        return EnrichmentUtils.getEnrichmentKey(type, field);
    }

    @Override
    public void declareOther(OutputFieldsDeclarer declarer) {

    }

    @Override
    public void emitOther(Tuple tuple, JSONObject message) {

    }
}
