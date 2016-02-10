package org.apache.metron.enrichment.adapters.geo;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.metron.enrichment.adapters.jdbc.JdbcAdapter;
import org.json.simple.JSONObject;

import java.net.InetAddress;
import java.sql.ResultSet;

public class GeoAdapter extends JdbcAdapter {

  private InetAddressValidator ipvalidator = new InetAddressValidator();

  @SuppressWarnings("unchecked")
  @Override
  public JSONObject enrich(String value) {
    JSONObject enriched = new JSONObject();
    try {
      InetAddress addr = InetAddress.getByName(value);
      if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()
              || addr.isSiteLocalAddress() || addr.isMulticastAddress()
              || !ipvalidator.isValidInet4Address(value)) {
        return new JSONObject();
      }
      String locidQuery = "select IPTOLOCID(\"" + value
              + "\") as ANS";
      ResultSet resultSet = statement.executeQuery(locidQuery);
      String locid = null;
      if (resultSet.next()) {
        locid = resultSet.getString("ANS");
      }
      resultSet.close();
      if (locid == null) return new JSONObject();
      String geoQuery = "select * from location where locID = " + locid;
      resultSet = statement.executeQuery(geoQuery);
      if (resultSet.next()) {
        enriched.put("locID", resultSet.getString("locID"));
        enriched.put("country", resultSet.getString("country"));
        enriched.put("city", resultSet.getString("city"));
        enriched.put("postalCode", resultSet.getString("postalCode"));
        enriched.put("latitude", resultSet.getString("latitude"));
        enriched.put("longitude", resultSet.getString("longitude"));
        enriched.put("dmaCode", resultSet.getString("dmaCode"));
        enriched.put("location_point", enriched.get("longitude") + "," + enriched.get("latitude"));
      }
      resultSet.close();
    } catch (Exception e) {
      e.printStackTrace();
      _LOG.error("Enrichment failure: " + e);
      return new JSONObject();
    }
    return enriched;
  }
}
