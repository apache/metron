package org.apache.metron.enrichment.bolt;

import backtype.storm.task.TopologyContext;
import org.apache.metron.bolt.JoinBolt;
import org.apache.metron.domain.Enrichment;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EnrichmentJoinBolt extends JoinBolt<JSONObject> {

  protected static final Logger LOG = LoggerFactory
          .getLogger(EnrichmentJoinBolt.class);

  protected List<Enrichment> enrichments;

  /**
   * @param enrichments A class for sending tuples to enrichment bolt
   * @return Instance of this class
   */
  public EnrichmentJoinBolt withEnrichments(List<Enrichment> enrichments) {
    this.enrichments = enrichments;
    return this;
  }

  @Override
  public void prepare(Map map, TopologyContext topologyContext) {

  }

  @Override
  public Set<String> getStreamIds() {
    Set<String> streamIds = new HashSet<>();
    for(Enrichment enrichment: enrichments) {
      streamIds.add(enrichment.getName());
    }
    return streamIds;
  }


  @Override
  public JSONObject joinValues(Map<String, JSONObject> streamValueMap) {
    JSONObject message = new JSONObject();
    message.put("message", streamValueMap.get("message"));
    JSONObject enrichment = new JSONObject();
    for(String streamId: getStreamIds()) {
      enrichment.put(streamId, streamValueMap.get(streamId));
    }
    message.put("enrichment", enrichment);
    return message;
  }
}
