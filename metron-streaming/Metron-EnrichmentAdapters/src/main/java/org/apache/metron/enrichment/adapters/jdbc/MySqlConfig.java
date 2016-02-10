package org.apache.metron.enrichment.adapters.jdbc;

public class MySqlConfig extends BaseJdbcConfig {

  @Override
  public String getClassName() {
    return "com.mysql.jdbc.Driver";
  }

  @Override
  public String getJdbcUrl() {
    StringBuilder url = new StringBuilder();
    url.append("jdbc:mysql://").append(host);
    if (port > 0) {
      url.append(":").append(port);
    }
    url.append("/").append(table);
    url.append("?user=").append(username);
    url.append("&password=").append(password);
    return url.toString();
  }
}
