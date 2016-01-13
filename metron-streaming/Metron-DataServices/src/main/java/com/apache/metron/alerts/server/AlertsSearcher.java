package com.apache.metron.alerts.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class AlertsSearcher implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger( AlertsSearcher.class );
	
	@Inject
	private Properties configProps;
	
	// TODO: inject Searcher module for either ElasticSearch or Solr...
	// TODO: inject MetronServiceFactory here
	
	public final Map<String, AlertsFilterCacheEntry> alertsFilterCache = new HashMap<String, AlertsFilterCacheEntry>();
	MessageDigest md;
	
	public AlertsSearcher() throws NoSuchAlgorithmException
	{
		md = MessageDigest.getInstance("SHA-256");

	}
	
	@Override
	public void run() {

		try
		{
			logger.debug( "Doing Elastic Search search..." );
			
			long currentSearchTime = System.currentTimeMillis();
			long lastSearchTime = 0L;
			
			// look for a marker that tells us the last time we ran...
			String homeDir = configProps.getProperty("homeDir");
			if( homeDir.endsWith( "/" )) {
				homeDir = homeDir.substring(0, homeDir.length()-1);
			}
			
			logger.info( "using homeDir = " + homeDir );
			
			File searcherStateFile = new File( homeDir + "/searcherState.properties" );
			if( searcherStateFile.exists() )
			{
				logger.info( "found existing searcherState.properties file" );
				FileInputStream fis = null;
				try {
					fis = new FileInputStream( searcherStateFile );
					Properties searcherState = new Properties();
					searcherState.load(fis);
					lastSearchTime = Long.parseLong( searcherState.getProperty("lastSearchTime"));
				}
				catch( FileNotFoundException e ) {
					logger.error( "Error locating lastSearchTime value from state file", e );
					
				} catch (IOException e) {
					logger.error( "Error locating lastSearchTime value from state file", e );
				}
				finally
				{
					try {
						fis.close();
					} catch (IOException e) {
						logger.error( "Probably ignorable error closing file stream: ", e );
					}
				}
			}
			else
			{
				// nothing to do here.  We'll write out our lastSearchTime at the end
				logger.info( "No existing searcherState.properties found" );
			}
			
			// search for alerts newer than "lastSearchTime" 
			Settings settings = ImmutableSettings.settingsBuilder()
					.put("client.transport.sniff", true).build();
			        // .put("cluster.name", "elasticsearch").build();
	
			Client client = null;
			try
			{
				logger.info( "initializing elasticsearch client" );
				
				String elasticSearchHostName = configProps.getProperty( "elasticSearchHostName", "localhost" );
				int elasticSearchHostPort = Integer.parseInt(configProps.getProperty( "elasticSearchHostPort", "9300" ) );
				client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(elasticSearchHostName, elasticSearchHostPort));
					
				logger.info( "lastSearchTime: " + lastSearchTime );
				
				String alertsIndexes = configProps.getProperty( "alertsIndexes", "alerts" );
				String[] alertsIndexArray = alertsIndexes.split(",");
				
				String alertsTypes = configProps.getProperty( "alertsTypes", "" );
				String[] alertsTypesArray = alertsTypes.split( ",");
				
				String alertsQueryFieldName = configProps.getProperty( "alertQueryFieldName", "alert.source" );
				String alertsQueryFieldValue = configProps.getProperty( "alertsQueryFieldValue", "*" );
				
				logger.info( "alertsIndexes: " + alertsIndexes );
				
				String[] foo = new String[1];
				
				SearchResponse response = client.prepareSearch( alertsIndexArray )
				.setTypes( alertsTypesArray )
				.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				.addField("_source")		
				.setQuery( QueryBuilders.boolQuery().must(  QueryBuilders.wildcardQuery( alertsQueryFieldName, alertsQueryFieldValue ) )
													.must( QueryBuilders.rangeQuery("message.timestamp").from(lastSearchTime).to(System.currentTimeMillis()).includeLower(true).includeUpper(false)))
				.execute()
				.actionGet();
				
				SearchHits hits = response.getHits();
				logger.debug( "Total hits: " + hits.getTotalHits());

				
				// for all hits, put the alert onto the Kafka topic.
				for( SearchHit hit : hits )
				{
					// calculate hash for this hit...
					String sourceData = hit.getSourceAsString();
					String hash = new String( md.digest(sourceData.getBytes()));
					
					if( alertsFilterCache.containsKey(hash))
					{
						logger.warn( "We have already seen this Alert, so skipping..." );
						continue;
					}
					else
					{
						long timeNow = System.currentTimeMillis();
						AlertsFilterCacheEntry cacheEntry = new AlertsFilterCacheEntry( sourceData, timeNow );
						alertsFilterCache.put(hash, cacheEntry);
					}
					
					doSenderWork(hit);
				}
			
			}
			finally
			{			
				if( client != null )
				{
					client.close();
				}
			}		
			
			// record the time we just searched
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream( searcherStateFile );
				Properties searcherState = new Properties();
				searcherState.setProperty( "lastSearchTime", Long.toString(currentSearchTime));
				searcherState.store(fos, "");
				
			}
			catch(  FileNotFoundException e ) {
				logger.error( "Error saving lastSearchTime: ", e );
			} catch (IOException e) {
				logger.error( "Error saving lastSearchTime: ", e );
			}
			finally {
				
				try {
					fos.close();
				} 
				catch (IOException e) {
					logger.error( "Probably ignorable error closing file stream: ", e );
				}
			}
			
			logger.info( "Done with ElasticSearch search... " );
		}
		catch( Exception e )
		{
			logger.error( "Unexpected error while searching ElasticSearch index:", e );
		}
	}
	
	private void doSenderWork( SearchHit hit )
	{	
		String kafkaBrokerHostName = configProps.getProperty("kafkaBrokerHostName", "localhost" );
		String kafkaBrokerHostPort = configProps.getProperty("kafkaBrokerHostPort", "9092" );
		String kafkaTopicName = configProps.getProperty("kafkaTopicName", "test" );
		
		logger.debug( "kafkaBrokerHostName: " + kafkaBrokerHostName );
		logger.debug( "kafkaBrokerHostPort: " + kafkaBrokerHostPort );
		logger.debug( "kafkaTopicName: " + kafkaTopicName );
		
		String sourceData = hit.getSourceAsString();
		
		logger.debug( "Source Data: " + sourceData );
		Properties props = new Properties();
		 
		props.put("metadata.broker.list", kafkaBrokerHostName + ":" + kafkaBrokerHostPort );
		props.put("serializer.class", "kafka.serializer.StringEncoder");
		// props.put("partitioner.class", "example.producer.SimplePartitioner");
		props.put("request.required.acks", "1");
		 
		ProducerConfig config = new ProducerConfig(props);
		
		Producer<String, String> producer = new Producer<String, String>(config);
		
		KeyedMessage<String, String> data = new KeyedMessage<String, String>(kafkaTopicName, "", sourceData );
		 
		producer.send(data);		
	}
}