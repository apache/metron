package com.opensoc.hbase;



import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import com.opensoc.helpers.topology.ErrorGenerator;

/**
 * A Storm bolt for putting data into HBase.
 * <p>
 * By default works in batch mode by enabling HBase's client-side write buffer. Enabling batch mode
 * is recommended for high throughput, but it can be disabled in {@link TupleTableConfig}.
 * <p>
 * The HBase configuration is picked up from the first <tt>hbase-site.xml</tt> encountered in the
 * classpath
 * @see TupleTableConfig
 * @see HTableConnector
 */
@SuppressWarnings("serial")
public class HBaseBolt implements IRichBolt {
  private static final Logger LOG = Logger.getLogger(HBaseBolt.class);

  protected OutputCollector collector;
  protected HTableConnector connector;
  protected TupleTableConfig conf;
  protected boolean autoAck = true;
  
  private String _quorum;
  private String _port;

  public HBaseBolt(TupleTableConfig conf, String quorum, String port) {
    this.conf = conf;
    _quorum = quorum;
    _port = port;

  }

  /** {@inheritDoc} */
  @SuppressWarnings("rawtypes")
  
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    this.collector = collector;

    try {
      this.connector = new HTableConnector(conf, _quorum, _port);

		
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    LOG.info("Preparing HBaseBolt for table: " + this.conf.getTableName());
  }

  /** {@inheritDoc} */
  
  public void execute(Tuple input) {
    try {
      this.connector.getTable().put(conf.getPutFromTuple(input));
    } catch (IOException ex) {

  		JSONObject error = ErrorGenerator.generateErrorMessage(
  				"Alerts problem: " + input.getBinary(0), ex);
  		collector.emit("error", new Values(error));
  		
      throw new RuntimeException(ex);
    }

    if (this.autoAck) {
      this.collector.ack(input);
    }
  }

  /** {@inheritDoc} */
  
  public void cleanup() {
    this.connector.close();
  }

  /** {@inheritDoc} */
  
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
	  declarer.declareStream("error", new Fields("HBase"));
  }

  /** {@inheritDoc} */
  
  public Map<String, Object> getComponentConfiguration() {
    return null;
  }

  /**
   * @return the autoAck
   */
  public boolean isAutoAck() {
    return autoAck;
  }

  /**
   * @param autoAck the autoAck to set
   */
  public void setAutoAck(boolean autoAck) {
    this.autoAck = autoAck;
  }
}