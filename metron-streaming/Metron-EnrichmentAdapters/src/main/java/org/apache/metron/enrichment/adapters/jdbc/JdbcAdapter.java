package org.apache.metron.enrichment.adapters.jdbc;

import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.InetAddress;
import java.sql.*;

public abstract class JdbcAdapter implements EnrichmentAdapter<String>,
        Serializable {

  protected static final Logger _LOG = LoggerFactory
          .getLogger(JdbcAdapter.class);

  protected Connection connection;
  protected Statement statement;

  private JdbcConfig config;
  private String host;

  public JdbcAdapter withJdbcConfig(JdbcConfig config) {
    this.config = config;
    this.host = config.getHost();
    return this;
  }

  @Override
  public boolean initializeAdapter() {
    try {
      if (!InetAddress.getByName(host).isReachable(500)) {
        throw new Exception("Unable to reach host " + host);
      }
      Class.forName(this.config.getClassName());
      connection = DriverManager.getConnection(this.config.getJdbcUrl());
      connection.setReadOnly(true);
      if (!connection.isValid(0))
        throw new Exception("Invalid connection string....");
      statement = connection.createStatement(
              ResultSet.TYPE_SCROLL_INSENSITIVE,
              ResultSet.CONCUR_READ_ONLY);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      _LOG.error("[Metron] JDBC connection failed....", e);

      return false;
    }
  }

  @Override
  public void cleanup() {
    try {
      if (statement != null) statement.close();
      if (connection != null) connection.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
