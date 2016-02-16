package org.apache.metron.bolt;

import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.apache.metron.enrichment.EnrichmentSplitterBolt;
import org.apache.metron.filters.GenericMessageFilter;
import org.apache.metron.helpers.topology.ErrorGenerator;
import org.apache.metron.parser.interfaces.MessageFilter;
import org.apache.metron.parser.interfaces.MessageParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TelemetryParserBolt extends EnrichmentSplitterBolt {

  protected static final Logger LOG = LoggerFactory
          .getLogger(TelemetryParserBolt.class);

  protected MessageParser<JSONObject> parser;
  protected MessageFilter<JSONObject> filter = new GenericMessageFilter();

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



  @Override
  public void prepare(Map map, TopologyContext topologyContext) {
    super.prepare(map, topologyContext);
    LOG.info("[Metron] Preparing TelemetryParser Bolt...");
    if (this.parser == null) {
      throw new IllegalStateException("MessageParser must be specified");
    }
    parser.init();
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


}
