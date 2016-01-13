/**
 * 
 */
package com.opensoc.pcapservice;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.log4j.Logger;
import org.mortbay.log.Log;

/**
 * Utility class which creates HConnection instance when the first request is
 * received and registers a shut down hook which closes the connection when the
 * JVM exits. Creates new connection to the cluster only if the existing
 * connection is closed for unknown reasons. Also creates Configuration with
 * HBase resources using configuration properties.
 * 
 * @author Sayi
 * 
 */
public class HBaseConfigurationUtil {

  /** The Constant LOGGER. */
  private static final Logger LOGGER = Logger
      .getLogger(HBaseConfigurationUtil.class);

  /** Configuration which holds all HBase properties. */
  private static Configuration config;

  /**
   * A cluster connection which knows how to find master node and locate regions
   * on the cluster.
   */
  private static HConnection clusterConnection = null;

  /**
   * Creates HConnection instance when the first request is received and returns
   * the same instance for all subsequent requests if the connection is still
   * open.
   * 
   * @return HConnection instance
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static HConnection getConnection() throws IOException {
    if (!connectionAvailable()) {
      synchronized (HBaseConfigurationUtil.class) {
        createClusterConncetion();
      }
    }
    return clusterConnection;
  }

  /**
   * Creates the cluster conncetion.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  private static void createClusterConncetion() throws IOException {
    try {
      if (connectionAvailable()) {
        return;
      }
      clusterConnection = HConnectionManager.createConnection(read());
      addShutdownHook();
      System.out.println("Created HConnection and added shutDownHook");
    } catch (IOException e) {
      LOGGER
          .error(
              "Exception occurred while creating HConnection using HConnectionManager",
              e);
      throw e;
    }
  }

  /**
   * Connection available.
   * 
   * @return true, if successful
   */
  private static boolean connectionAvailable() {
    if (clusterConnection == null) {
      System.out.println("clusterConnection=" + clusterConnection);
      return false;
    }
    System.out.println("clusterConnection.isClosed()="
        + clusterConnection.isClosed());
    return clusterConnection != null && !clusterConnection.isClosed();
  }

  /**
   * Adds the shutdown hook.
   */
  private static void addShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() {
        System.out
            .println("Executing ShutdownHook HBaseConfigurationUtil : Closing HConnection");
        try {
          clusterConnection.close();
        } catch (IOException e) {
          Log.debug("Caught ignorable exception ", e);
        }
      }
    }, "HBaseConfigurationUtilShutDown"));
  }

  /**
   * Closes the underlying connection to cluster; ignores if any exception is
   * thrown.
   */
  public static void closeConnection() {
    if (clusterConnection != null) {
      try {
        clusterConnection.close();
      } catch (IOException e) {
        Log.debug("Caught ignorable exception ", e);
      }
    }
  }

  /**
   * This method creates Configuration with HBase resources using configuration
   * properties. The same Configuration object will be used to communicate with
   * all HBase tables;
   * 
   * @return Configuration object
   */
  public static Configuration read() {
    if (config == null) {
      synchronized (HBaseConfigurationUtil.class) {
        if (config == null) {
          config = HBaseConfiguration.create();

          config.set(
              HBaseConfigConstants.HBASE_ZOOKEEPER_QUORUM,
              ConfigurationUtil.getConfiguration().getString(
                  "hbase.zookeeper.quorum"));
          config.set(
              HBaseConfigConstants.HBASE_ZOOKEEPER_CLIENT_PORT,
              ConfigurationUtil.getConfiguration().getString(
                  "hbase.zookeeper.clientPort"));
          config.set(
              HBaseConfigConstants.HBASE_CLIENT_RETRIES_NUMBER,
              ConfigurationUtil.getConfiguration().getString(
                  "hbase.client.retries.number"));
          config.set(
              HBaseConfigConstants.HBASE_ZOOKEEPER_SESSION_TIMEOUT,
              ConfigurationUtil.getConfiguration().getString(
                  "zookeeper.session.timeout"));
          config.set(
              HBaseConfigConstants.HBASE_ZOOKEEPER_RECOVERY_RETRY,
              ConfigurationUtil.getConfiguration().getString(
                  "zookeeper.recovery.retry"));
        }
      }
    }
    return config;
  }
}
