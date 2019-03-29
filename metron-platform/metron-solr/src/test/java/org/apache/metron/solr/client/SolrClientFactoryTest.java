package org.apache.metron.solr.client;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.metron.solr.SolrConstants.SOLR_ZOOKEEPER;
import static org.junit.Assert.assertEquals;

public class SolrClientFactoryTest {

  @Test
  public void testGetZkHostsSingle() {
    Map<String, Object> globalConfig = new HashMap<String, Object>() {{
      put(SOLR_ZOOKEEPER, "   zookeeper:2181   ");
    }};

    List<String> actual = SolrClientFactory.getZkHosts(globalConfig);
    List<String> expected = new ArrayList<>();
    expected.add("zookeeper:2181");
    assertEquals(expected, actual);
  }

  @Test
  public void testGetZkHostsMultiple() {
    Map<String, Object> globalConfig = new HashMap<String, Object>() {{
      put(SOLR_ZOOKEEPER, "   zookeeper:2181    ,   zookeeper2:2181    ");
    }};

    List<String> actual = SolrClientFactory.getZkHosts(globalConfig);
    List<String> expected = new ArrayList<>();
    expected.add("zookeeper:2181");
    expected.add("zookeeper2:2181");
    assertEquals(expected, actual);
  }
}
