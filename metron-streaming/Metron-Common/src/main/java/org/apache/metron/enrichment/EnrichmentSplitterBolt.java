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
package org.apache.metron.enrichment;

import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import com.google.common.base.Splitter;
import org.apache.metron.bolt.SplitBolt;
import org.apache.metron.domain.Enrichment;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by cstella on 2/10/16.
 */
public class EnrichmentSplitterBolt extends SplitBolt<JSONObject> {
    protected static final Logger LOG = LoggerFactory.getLogger(EnrichmentSplitterBolt.class);
    protected List<Enrichment> enrichments = new ArrayList<>();
    protected String messageFieldName = "message";
    /**
     * @param enrichments A class for sending tuples to enrichment bolt
     * @return Instance of this class
     */
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
    public List<JSONObject> generateMessages(Tuple tuple) {
        return Arrays.asList((JSONObject)tuple.getValueByField(messageFieldName));
    }

    @Override
    public Set<String> getStreamIds() {
        Set<String> streamIds = new HashSet<>();
        for(Enrichment enrichment: enrichments) {
            streamIds.add(enrichment.getName());
        }
        return streamIds;
    }
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, JSONObject> splitMessage(JSONObject message) {

        Map<String, JSONObject> streamMessageMap = new HashMap<>();
        for (Enrichment enrichment : enrichments) {
            List<String> fields = enrichment.getFields();
            if (fields != null && fields.size() > 0) {
                JSONObject enrichmentObject = new JSONObject();
                for (String field : fields) {
                    enrichmentObject.put(field, getField(message,field));
                }
                streamMessageMap.put(enrichment.getName(), enrichmentObject);
            }
        }
        /*if(message != null && enrichments.size() != 1) {
            throw new RuntimeException("JSON: " + message.toJSONString() + " => " + streamMessageMap);
        }*/
        return streamMessageMap;
    }

    public Object getField(JSONObject object, String path) {
        Map ret = object;
        for(String node: Splitter.on('/').split(path))  {
            Object o = ret.get(node);
            if(o instanceof Map) {
                ret = (Map) o;
            }
            else {
                return o;
            }
        }
        return ret;
    }

    @Override
    public void declareOther(OutputFieldsDeclarer declarer) {

    }

    @Override
    public void emitOther(Tuple tuple, List<JSONObject> messages) {

    }
}
