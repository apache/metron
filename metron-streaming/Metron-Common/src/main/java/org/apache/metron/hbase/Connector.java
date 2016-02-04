package org.apache.metron.hbase;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Serializable;

/**
 * Created by cstella on 1/29/16.
 */
public abstract class Connector {
  protected TupleTableConfig tableConf;
  protected String _quorum;
  protected String _port;

  public Connector(final TupleTableConfig conf, String _quorum, String _port) throws IOException {
    this.tableConf = conf;
    this._quorum = _quorum;
    this._port = _port;
  }
  public abstract void put(Put put) throws InterruptedIOException, RetriesExhaustedWithDetailsException;
  public abstract void close();
}
