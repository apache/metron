package org.apache.metron.pcapservice;

/**
 * HBase configuration properties.
 * 
 * @author Sayi
 */
public class HBaseConfigConstants {

  /** The Constant HBASE_ZOOKEEPER_QUORUM. */
  public static final String HBASE_ZOOKEEPER_QUORUM = "hbase.zookeeper.quorum";

  /** The Constant HBASE_ZOOKEEPER_CLIENT_PORT. */
  public static final String HBASE_ZOOKEEPER_CLIENT_PORT = "hbase.zookeeper.clientPort";

  /** The Constant HBASE_ZOOKEEPER_SESSION_TIMEOUT. */
  public static final String HBASE_ZOOKEEPER_SESSION_TIMEOUT = "zookeeper.session.timeout";

  /** The Constant HBASE_ZOOKEEPER_RECOVERY_RETRY. */
  public static final String HBASE_ZOOKEEPER_RECOVERY_RETRY = "zookeeper.recovery.retry";

  /** The Constant HBASE_CLIENT_RETRIES_NUMBER. */
  public static final String HBASE_CLIENT_RETRIES_NUMBER = "hbase.client.retries.number";

  /** The delimeter. */
  String delimeter = "-";

  /** The regex. */
  String regex = "\\-";

  /** The Constant PCAP_KEY_DELIMETER. */
  public static final String PCAP_KEY_DELIMETER = "-";

  /** The Constant START_KEY. */
  public static final String START_KEY = "startKey";

  /** The Constant END_KEY. */
  public static final String END_KEY = "endKey";

}
