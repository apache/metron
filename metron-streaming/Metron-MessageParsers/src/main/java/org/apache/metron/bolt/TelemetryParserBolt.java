package org.apache.metron.bolt;

import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.apache.metron.domain.Enrichment;
import org.apache.metron.filters.GenericMessageFilter;
import org.apache.metron.helpers.topology.ErrorGenerator;
import org.apache.metron.parser.interfaces.MessageFilter;
import org.apache.metron.parser.interfaces.MessageParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TelemetryParserBolt extends
        SplitBolt<JSONObject> {

  protected static final Logger LOG = LoggerFactory
          .getLogger(TelemetryParserBolt.class);

  protected MessageParser<JSONObject> parser;
  protected MessageFilter<JSONObject> filter = new GenericMessageFilter();
  protected List<Enrichment> enrichments = new ArrayList<>();

  /**
   * @param parser The parser class for parsing the incoming raw message byte
   *               stream
   * @return Instance of this class
   */
  public TelemetryParserBolt withMessageParser(MessageParser<JSONObject>
                                                      parser) {
    this.parser = parser;
    return this;
  }

  /**
   * @param filter A class for filtering/dropping incomming telemetry messages
   * @return Instance of this class
   */
  public TelemetryParserBolt withMessageFilter(MessageFilter<JSONObject>
                                                      filter) {
    this.filter = filter;
    return this;
  }

  /**
   * @param enrichments A class for sending tuples to enrichment bolt
   * @return Instance of this class
   */
  public TelemetryParserBolt withEnrichments(List<Enrichment> enrichments) {
    this.enrichments = enrichments;
    return this;
  }

  @Override
  public void prepare(Map map, TopologyContext topologyContext) {
    LOG.info("[Metron] Preparing TelemetryParser Bolt...");
    if (this.parser == null) {
      throw new IllegalStateException("MessageParser must be specified");
    }
    parser.init();
  }

  @Override
  public String getKey(Tuple tuple, JSONObject message) {
    return UUID.randomUUID().toString();
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
  public List<JSONObject> generateMessages(Tuple tuple) {
    List<JSONObject> filteredMessages = new ArrayList<>();
    byte[] originalMessage = tuple.getBinary(0);
    try {
      originalMessage = tuple.getBinary(0);
      if (originalMessage == null || originalMessage.length == 0) {
        throw new Exception("Invalid message length");
      }
      List<JSONObject> messages = parser.parse(originalMessage);
      for (JSONObject message : messages) {
        if (!parser.validate(message)) {
          throw new Exception("Message validation failed: "
                  + message);
        } else {
          if (filter != null && filter.emitTuple(message)) {
            filteredMessages.add(message);
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to parse telemetry message", e);
      collector.fail(tuple);
      JSONObject error = ErrorGenerator.generateErrorMessage(
              "Parsing problem: " + new String(originalMessage), e);
      collector.emit("error", new Values(error));
    }
    return filteredMessages;
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
          enrichmentObject.put(field, message.get(field));
        }
        streamMessageMap.put(enrichment.getName(), enrichmentObject);
      }
    }
    return streamMessageMap;
  }

  @Override
  public void declareOther(OutputFieldsDeclarer declarer) {

  }

  @Override
  public void emitOther(Tuple tuple, List<JSONObject> messages) {

  }
}
