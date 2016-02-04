package org.apache.metron.hbase;



import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import org.apache.metron.helpers.topology.ErrorGenerator;

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
  private static final String DEFAULT_ZK_PORT = "2181";

  protected OutputCollector collector;
  protected TupleTableConfig conf;
  protected boolean autoAck = true;
  protected Connector connector;
  private String connectorImpl;
  private String _quorum;
  private String _port;

  public HBaseBolt(TupleTableConfig conf, String quorum, String port) {
    this.conf = conf;
    _quorum = quorum;
    _port = port;
  }
  public HBaseBolt(final TupleTableConfig conf, String zkConnectString) throws IOException {
    this(conf, zkConnectStringToHosts(zkConnectString), zkConnectStringToPort(zkConnectString));
  }
  public static String zkConnectStringToHosts(String connString) {
    Iterable<String> hostPortPairs = Splitter.on(',').split(connString);
    return Joiner.on(',').join(Iterables.transform(hostPortPairs, new Function<String, String>() {

      @Override
      public String apply(String hostPortPair) {
        return Iterables.getFirst(Splitter.on(':').split(hostPortPair), "");
      }
    }));
  }
  public static String zkConnectStringToPort(String connString) {
    String hostPortPair = Iterables.getFirst(Splitter.on(",").split(connString), "");
    return Iterables.getLast(Splitter.on(":").split(hostPortPair),DEFAULT_ZK_PORT);
  }
  public HBaseBolt withConnector(String connectorImpl) {
    this.connectorImpl = connectorImpl;
    return this;
  }

  public Connector createConnector() throws IOException{
    initialize();
    if(connectorImpl == null || connectorImpl.length() == 0 || connectorImpl.charAt(0) == '$') {
      return new HTableConnector(conf, _quorum, _port);
    }
    else {
      try {
        Class<? extends Connector> clazz = (Class<? extends Connector>) Class.forName(connectorImpl);
        return clazz.getConstructor(TupleTableConfig.class, String.class, String.class).newInstance(conf, _quorum, _port);
      } catch (InstantiationException e) {
        throw new IOException("Unable to instantiate connector.", e);
      } catch (IllegalAccessException e) {
        throw new IOException("Unable to instantiate connector: illegal access", e);
      } catch (InvocationTargetException e) {
        throw new IOException("Unable to instantiate connector", e);
      } catch (NoSuchMethodException e) {
        throw new IOException("Unable to instantiate connector: no such method", e);
      } catch (ClassNotFoundException e) {
        throw new IOException("Unable to instantiate connector: class not found", e);
      }
    }
  }

  public void initialize() {
    TupleTableConfig hbaseBoltConfig = conf;
    String allColumnFamiliesColumnQualifiers = conf.getFields();
    String[] tokenizedColumnFamiliesWithColumnQualifiers = StringUtils
            .split(allColumnFamiliesColumnQualifiers, "\\|");
    for (String tokenizedColumnFamilyWithColumnQualifiers : tokenizedColumnFamiliesWithColumnQualifiers) {
      String[] cfCqTokens = StringUtils.split( tokenizedColumnFamilyWithColumnQualifiers, ":");
      String columnFamily = cfCqTokens[0];
      String[] columnQualifiers = StringUtils.split(cfCqTokens[1], ",");
      for (String columnQualifier : columnQualifiers) {
        hbaseBoltConfig.addColumn(columnFamily, columnQualifier);
      }
      setAutoAck(true);
    }
  }

  /** {@inheritDoc} */
  @SuppressWarnings("rawtypes")
  
  public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
    this.collector = collector;

    try {
      this.connector = createConnector();

		
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    LOG.info("Preparing HBaseBolt for table: " + this.conf.getTableName());
  }

  /** {@inheritDoc} */
  
  public void execute(Tuple input) {
    try {
      this.connector.put(conf.getPutFromTuple(input));
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