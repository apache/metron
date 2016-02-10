package org.apache.metron.enrichment.adapters.jdbc;

public interface JdbcConfig {

  public String getClassName();
  public String getJdbcUrl();
  public String getHost();

}
