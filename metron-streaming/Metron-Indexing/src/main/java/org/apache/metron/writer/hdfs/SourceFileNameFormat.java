package org.apache.metron.writer.hdfs;

import backtype.storm.task.TopologyContext;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;

import java.util.Map;

public class SourceFileNameFormat implements FileNameFormat {
  FileNameFormat delegate;
  String sourceType;
  public SourceFileNameFormat(String sourceType, FileNameFormat delegate) {
    this.delegate = delegate;
    this.sourceType = sourceType;
  }

  @Override
  public void prepare(Map map, TopologyContext topologyContext) {
    this.delegate.prepare(map, topologyContext);
  }

  @Override
  public String getName(long l, long l1) {
    return delegate.getName(l, l1);
  }

  @Override
  public String getPath() {
    return delegate.getPath() + "/" + sourceType;
  }
}
