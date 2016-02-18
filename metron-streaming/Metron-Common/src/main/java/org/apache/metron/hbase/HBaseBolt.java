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
package org.apache.metron.hbase;



import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Put;
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


  public Connector createConnector() throws IOException{
    initialize();
    return new HTableConnector(conf, _quorum, _port);
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
      if(connector == null) {
        this.connector = createConnector();
      }
		
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    LOG.info("Preparing HBaseBolt for table: " + this.conf.getTableName());
  }

  /** {@inheritDoc} */
  
  public void execute(Tuple input) {
    try {
      Put p = conf.getPutFromTuple(input);
      this.connector.put(p);
    } catch (IOException ex) {

  		JSONObject error = ErrorGenerator.generateErrorMessage(
  				"Alerts problem: " + input.toString(), ex);
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
