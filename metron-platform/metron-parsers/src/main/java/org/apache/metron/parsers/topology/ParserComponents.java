package org.apache.metron.parsers.topology;

import org.apache.metron.parsers.bolt.WriterHandler;
import org.apache.metron.parsers.interfaces.MessageFilter;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.json.simple.JSONObject;

public class ParserComponents {
  MessageParser<JSONObject> messageParser;
  MessageFilter<JSONObject> filter;
  WriterHandler writer;

  public ParserComponents(
      MessageParser<JSONObject> messageParser,
      MessageFilter<JSONObject> filter, WriterHandler writer) {
    this.messageParser = messageParser;
    this.filter = filter;
    this.writer = writer;
  }

  public MessageParser<JSONObject> getMessageParser() {
    return messageParser;
  }

  public MessageFilter<JSONObject> getFilter() {
    return filter;
  }

  public WriterHandler getWriter() {
    return writer;
  }

  public void setMessageParser(
      MessageParser<JSONObject> messageParser) {
    this.messageParser = messageParser;
  }

  public void setFilter(
      MessageFilter<JSONObject> filter) {
    this.filter = filter;
  }

  public void setWriter(WriterHandler writer) {
    this.writer = writer;
  }
}
