package org.apache.metron.enrichment.adapters.host;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by rmerriman on 2/2/16.
 */
public class HostFromJSONListAdapter extends AbstractHostAdapter {

  Map<String, JSONObject> _known_hosts = new HashMap<>();

  public HostFromJSONListAdapter(String jsonList) {
    JSONArray jsonArray = (JSONArray) JSONValue.parse(jsonList);
    Iterator jsonArrayIterator = jsonArray.iterator();
    while(jsonArrayIterator.hasNext()) {
      JSONObject jsonObject = (JSONObject) jsonArrayIterator.next();
      String host = (String) jsonObject.remove("ip");
      _known_hosts.put(host, jsonObject);
    }
  }

  @Override
  public boolean initializeAdapter()
  {

    if(_known_hosts.size() > 0)
      return true;
    else
      return false;
  }

  @Override
  public void logAccess(String value) {

  }

  @SuppressWarnings("unchecked")
  @Override
  public JSONObject enrich(String metadata) {


    if(!_known_hosts.containsKey(metadata))
      return new JSONObject();

    JSONObject enrichment = new JSONObject();
    enrichment.put("known_info", _known_hosts.get(metadata));
    return enrichment;
  }
}
