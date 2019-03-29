package org.apache.metron.solr.client;

import com.google.common.base.Splitter;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;

import java.util.List;
import java.util.Map;

import static org.apache.metron.solr.SolrConstants.SOLR_ZOOKEEPER;

/**
 * Factory for creating a SolrClient.  The default implementation of SolrClient is CloudSolrClient.
 */
public class SolrClientFactory {

  /**
   * Creates a SolrClient.
   * @param globalConfig Global config
   * @return SolrClient
   */
  public static SolrClient create(Map<String, Object> globalConfig) {
    return new CloudSolrClient.Builder().withZkHost(getZkHosts(globalConfig)).build();
  }

  /**
   * Retrieves zookeeper hosts from the global config and formats them for CloudSolrClient instantiation.
   * @param globalConfig Global config
   * @return A list of properly formatted zookeeper servers
   */
  protected static List<String> getZkHosts(Map<String, Object> globalConfig) {
    return Splitter.on(',').trimResults()
            .splitToList((String) globalConfig.getOrDefault(SOLR_ZOOKEEPER, ""));
  }
}
