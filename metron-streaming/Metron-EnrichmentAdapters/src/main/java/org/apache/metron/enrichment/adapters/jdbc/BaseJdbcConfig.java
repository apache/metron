package org.apache.metron.enrichment.adapters.jdbc;

import java.io.Serializable;

public abstract class BaseJdbcConfig implements JdbcConfig, Serializable {

  protected String host;
  protected int port = -1;
  protected String username;
  protected String password;
  protected String table = "";

  @Override
  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }
}
