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
package org.apache.metron.enrichment.adapters.jdbc;

import org.apache.metron.enrichment.bolt.CacheKey;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.InetAddress;
import java.sql.*;

public abstract class JdbcAdapter implements EnrichmentAdapter<CacheKey>, Serializable {

  protected static final Logger _LOG = LoggerFactory
          .getLogger(JdbcAdapter.class);

  protected Connection connection;
  protected Statement statement;

  private JdbcConfig config;
  private String host;

  protected boolean isConnectionClosed() {
    boolean isClosed = statement == null || connection == null;
    if(!isClosed) {
      try {
        isClosed = statement.isClosed() || connection.isClosed();
      } catch (SQLException e) {
        _LOG.error("Unable to maintain open JDBC connection: " + e.getMessage(), e);
        isClosed = true;
      }
    }
    return isClosed;
  }

  protected boolean resetConnectionIfNecessary() {
    if(isConnectionClosed()) {
      this.cleanup();
      return this.initializeAdapter();
    }
    return true;
  }
  public void setStatement(Statement statement) {
    this.statement = statement;
  }

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
      if (!connection.isValid(0)) {
        throw new Exception("Invalid connection string....");
      }
      statement = connection.createStatement(
              ResultSet.TYPE_SCROLL_INSENSITIVE,
              ResultSet.CONCUR_READ_ONLY);
      return true;
    } catch (Exception e) {
      _LOG.error("[Metron] JDBC connection failed....", e);
      return false;
    }
  }

  @Override
  public void cleanup() {
    try {
      if (statement != null) {
        statement.close();
      }
    } catch (SQLException e) {
      _LOG.error("[Metron] JDBC statement close failed....", e);
    }
    try {
      if (connection != null) {
        connection.close();
      }
    }
    catch(SQLException e) {
      _LOG.error("[Metron] JDBC connection close failed....", e);
    }
  }
}
