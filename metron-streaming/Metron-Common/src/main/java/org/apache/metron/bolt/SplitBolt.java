package org.apache.metron.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class SplitBolt<T> extends
        BaseRichBolt {

  protected OutputCollector collector;
  private Set<String> streamIds;

  @Override
  public final void prepare(Map map, TopologyContext topologyContext,
                       OutputCollector outputCollector) {
    collector = outputCollector;
    streamIds = ImmutableSet.copyOf(getStreamIds());
    prepare(map, topologyContext);
  }

  @Override
  public final void execute(Tuple tuple) {
    emit(tuple, generateMessages(tuple));
  }

  @Override
  public final void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream("message", new Fields("key", "message"));
    for (String streamId : getStreamIds()) {
      declarer.declareStream(streamId, new Fields("key", "message"));
    }
    declarer.declareStream("error", new Fields("message"));
    declareOther(declarer);
  }

  public void emit(Tuple tuple, List<T> messages) {
    for(T message: messages) {
      String key = getKey(tuple, message);
      collector.emit("message", tuple, new Values(key, message));
      Map<String, T> streamValueMap = splitMessage(message);
      for (String streamId : streamIds) {
        T streamValue = streamValueMap.get(streamId);
        if (streamValue == null) {
          streamValue = getDefaultValue(streamId);
        }
        collector.emit(streamId, new Values(key, streamValue));
      }
      collector.ack(tuple);
    }
    emitOther(tuple, messages);
  }

  protected T getDefaultValue(String streamId) {
    throw new IllegalArgumentException("Could not find a message for" +
            " stream: " + streamId);
  }

  public abstract void prepare(Map map, TopologyContext topologyContext);

  public abstract Set<String> getStreamIds();

  public abstract String getKey(Tuple tuple, T message);

  public abstract List<T> generateMessages(Tuple tuple);

  public abstract Map<String, T> splitMessage(T message);

  public abstract void declareOther(OutputFieldsDeclarer declarer);

  public abstract void emitOther(Tuple tuple, List<T> messages);


}
