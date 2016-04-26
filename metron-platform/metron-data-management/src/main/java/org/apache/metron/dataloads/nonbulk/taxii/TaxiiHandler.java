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

package org.apache.metron.dataloads.nonbulk.taxii;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.mitre.taxii.client.HttpClient;
import org.mitre.taxii.messages.xml11.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class TaxiiHandler extends TimerTask {
    private static final Logger LOG = Logger.getLogger(TaxiiHandler.class);

    private static ThreadLocal<TaxiiXmlFactory> xmlFactory = new ThreadLocal<TaxiiXmlFactory>() {
        @Override
        protected TaxiiXmlFactory initialValue() {
            return new TaxiiXmlFactory();
        }
    };
    private static ThreadLocal<ObjectFactory> messageFactory = new ThreadLocal<ObjectFactory>() {
        @Override
        protected ObjectFactory initialValue() {
            return new ObjectFactory();
        }
    };

    private HttpClient taxiiClient;
    private URL endpoint;
    private Extractor extractor;
    private String hbaseTable;
    private String columnFamily;
    private Map<String, HTableInterface> connectionCache = new HashMap<>();
    private HttpClientContext context;
    private String collection;
    private String subscriptionId;
    private EnrichmentConverter converter = new EnrichmentConverter();
    private Date beginTime;
    private Configuration config;
    private boolean inProgress = false;
    private Set<String> allowedIndicatorTypes;
    public TaxiiHandler( TaxiiConnectionConfig connectionConfig
                       , Extractor extractor
                       , Configuration config
                       ) throws Exception
    {
        LOG.info("Loading configuration: " + connectionConfig);
        this.allowedIndicatorTypes = connectionConfig.getAllowedIndicatorTypes();
        this.extractor = extractor;
        this.collection = connectionConfig.getCollection();
        this.subscriptionId = connectionConfig.getSubscriptionId();
        hbaseTable = connectionConfig.getTable();
        columnFamily = connectionConfig.getColumnFamily();
        this.beginTime = connectionConfig.getBeginTime();
        this.config = config;
        initializeClient(connectionConfig);
        LOG.info("Configured, starting polling " + endpoint + " for " + collection);
    }

    protected synchronized HTableInterface getTable(String table) throws IOException {
        HTableInterface ret = connectionCache.get(table);
        if(ret == null) {
            ret = createHTable(table);
            connectionCache.put(table, ret);
        }
        return ret;
    }

    protected synchronized HTableInterface createHTable(String tableInfo) throws IOException {
        return new HTable(config, tableInfo);
    }
    /**
     * The action to be performed by this timer task.
     */
    @Override
    public void run() {
        if(inProgress) {
            return;
        }
        Date ts = new Date();
        LOG.info("Polling..." + new SimpleDateFormat().format(ts));
        try {
            inProgress = true;
            // Prepare the message to send.
            String sessionID = MessageHelper.generateMessageId();
            PollRequest request = messageFactory.get().createPollRequest()
                    .withMessageId(sessionID)
                    .withCollectionName(collection);
            if (subscriptionId != null) {
                request = request.withSubscriptionID(subscriptionId);
            } else {
                request = request.withPollParameters(messageFactory.get().createPollParametersType());
            }
            if (beginTime != null) {
                Calendar gc = GregorianCalendar.getInstance();
                gc.setTime(beginTime);
                XMLGregorianCalendar gTime = null;
                try {
                    gTime = DatatypeFactory.newInstance().newXMLGregorianCalendar((GregorianCalendar) gc).normalize();
                } catch (DatatypeConfigurationException e) {
                    LOG.error("Unable to set the begin time", e);
                }
                gTime.setFractionalSecond(null);
                LOG.info("Begin Time: " + gTime);
                request.setExclusiveBeginTimestamp(gTime);
            }

            try {
                PollResponse response = call(request, PollResponse.class);
                LOG.info("Got Poll Response with " + response.getContentBlocks().size() + " blocks");
                int numProcessed = 0;
                long avgTimeMS = 0;
                long timeStartedBlock = System.currentTimeMillis();
                for (ContentBlock block : response.getContentBlocks()) {
                    AnyMixedContentType content = block.getContent();
                    for (Object o : content.getContent()) {
                        numProcessed++;
                        long timeS = System.currentTimeMillis();
                        String xml = null;
                        if (o instanceof Element) {
                            Element element = (Element) o;
                            xml = getStringFromDocument(element.getOwnerDocument());
                            if(LOG.isDebugEnabled() && Math.random() < 0.01) {
                                LOG.debug("Random Stix doc: " + xml);
                            }
                            for (LookupKV<EnrichmentKey, EnrichmentValue> kv : extractor.extract(xml)) {
                                if(allowedIndicatorTypes.isEmpty()
                                || allowedIndicatorTypes.contains(kv.getKey().type)
                                  )
                                {
                                    kv.getValue().getMetadata().put("source_type", "taxii");
                                    kv.getValue().getMetadata().put("taxii_url", endpoint.toString());
                                    kv.getValue().getMetadata().put("taxii_collection", collection);
                                    Put p = converter.toPut(columnFamily, kv.getKey(), kv.getValue());
                                    HTableInterface table = getTable(hbaseTable);
                                    table.put(p);
                                    LOG.info("Found Threat Intel: " + kv.getKey() + " => " + kv.getValue());
                                }
                            }
                        }
                        avgTimeMS += System.currentTimeMillis() - timeS;
                    }
                    if( (numProcessed + 1) % 100 == 0) {
                        LOG.info("Processed " + numProcessed + " in " + (System.currentTimeMillis() - timeStartedBlock) + " ms, avg time: " + avgTimeMS / content.getContent().size());
                        timeStartedBlock = System.currentTimeMillis();
                        avgTimeMS = 0;
                        numProcessed = 0;
                    }
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                throw new RuntimeException("Unable to make request", e);
            }
        }
        finally {
            inProgress = false;
            beginTime = ts;
        }
    }
    public String getStringFromDocument(Document doc)
    {
        try
        {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            return writer.toString();
        }
        catch(TransformerException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }
    private <RESPONSE_T> RESPONSE_T call( Object request, Class<RESPONSE_T> responseClazz) throws URISyntaxException, JAXBException, IOException {
        return call(taxiiClient, endpoint.toURI(), request, context, responseClazz);
    }

    private void initializeClient(TaxiiConnectionConfig config) throws Exception {
        LOG.info("Initializing client..");
        if(context == null) {
            context = createContext(config.getEndpoint(), config.getUsername(), config.getPassword(), config.getPort());
        }
        URL endpoint = config.getEndpoint();
        if(config.getType() == ConnectionType.DISCOVER) {
            LOG.info("Discovering endpoint");
            endpoint = discoverPollingClient(config.getProxy(), endpoint, config.getUsername(), config.getPassword(), context, collection).pollEndpoint;
            this.endpoint = endpoint;
            LOG.info("Discovered endpoint as " + endpoint);
        }
        taxiiClient = buildClient(config.getProxy(), config.getUsername(), config.getPassword());
    }

    private static class DiscoveryResults {
        URL pollEndpoint;
        URL collectionManagementEndpoint;
        List<String> collections = new ArrayList<>();
    }
    private static DiscoveryResults discoverPollingClient(URL proxy, URL endpoint, String username, String password, HttpClientContext context, String defaultCollection) throws Exception {

        DiscoveryResults results = new DiscoveryResults();
        {
            HttpClient discoverClient = buildClient(proxy, username, password);
            String sessionID = MessageHelper.generateMessageId();
            // Prepare the message to send.
            DiscoveryRequest request = messageFactory.get().createDiscoveryRequest()
                    .withMessageId(sessionID);
            DiscoveryResponse response = call(discoverClient, endpoint.toURI(), request, context, DiscoveryResponse.class);
            for (ServiceInstanceType serviceInstance : response.getServiceInstances()) {
                if (serviceInstance.isAvailable() && serviceInstance.getServiceType() == ServiceTypeEnum.POLL) {
                    results.pollEndpoint = new URL(serviceInstance.getAddress());
                }
                else if(serviceInstance.isAvailable() && serviceInstance.getServiceType() == ServiceTypeEnum.COLLECTION_MANAGEMENT) {
                    results.collectionManagementEndpoint= new URL(serviceInstance.getAddress());
                }
            }
            if (results.pollEndpoint == null) {
                throw new RuntimeException("Unable to discover a poll TAXII feed");
            }
        }
        if(defaultCollection == null)
        //get collections
        {
            HttpClient discoverClient = buildClient(proxy, username, password);
            String sessionID = MessageHelper.generateMessageId();
            CollectionInformationRequest request = messageFactory.get().createCollectionInformationRequest()
                                                                 .withMessageId(sessionID);
            CollectionInformationResponse response = call(discoverClient, results.collectionManagementEndpoint.toURI(), request, context, CollectionInformationResponse.class);
            LOG.info("Unable to find the default collection; available collections are:");
            for(CollectionRecordType c : response.getCollections()) {
                LOG.info(c.getCollectionName());
                results.collections.add(c.getCollectionName());
            }
            System.exit(0);
        }
        return results;
    }

    private static HttpClientContext createContext(URL endpoint, String username, String password, int port) {
        HttpClientContext context = null;
        HttpHost target = new HttpHost(endpoint.getHost(), port, endpoint.getProtocol());
        if (username != null && password != null) {

            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(target.getHostName(), target.getPort()),
                    new UsernamePasswordCredentials(username, password));

            // http://hc.apache.org/httpcomponents-client-ga/tutorial/html/authentication.html
            AuthCache authCache = new BasicAuthCache();
            authCache.put(target, new BasicScheme());

            // Add AuthCache to the execution context
            context = HttpClientContext.create();
            context.setCredentialsProvider(credsProvider);
            context.setAuthCache(authCache);
        } else {
            context = null;
        }
        return context;
    }


    public static <RESPONSE_T, REQUEST_T> RESPONSE_T call( HttpClient taxiiClient
            , URI endpoint
            , REQUEST_T request
            , HttpClientContext context
            , Class<RESPONSE_T> responseClazz
    ) throws JAXBException, IOException {
        //TaxiiXml taxiiXml = xmlFactory.get().createTaxiiXml();
        //String req = taxiiXml.marshalToString(request, true);
        // Call the service
        Object responseObj =  taxiiClient.callTaxiiService(endpoint, request, context);
        LOG.info("Request made : " + request.getClass().getCanonicalName() + " => " + responseObj.getClass().getCanonicalName() + " (expected " + responseClazz.getCanonicalName() + ")");
        //String resp = taxiiXml.marshalToString(responseObj, true);
        try {
            return responseClazz.cast(responseObj);
        }
        catch(ClassCastException cce) {
            TaxiiXml taxiiXml = xmlFactory.get().createTaxiiXml();
            String resp = taxiiXml.marshalToString(responseObj, true);
            String msg = "Didn't return the response we expected: " + responseObj.getClass() + " \n" + resp;
            LOG.error(msg, cce);
            throw new RuntimeException(msg, cce);
        }
    }
    private static HttpClient buildClient(URL proxy, String username, String password) throws Exception
    {
        HttpClient client = new HttpClient(); // Start with a default TAXII HTTP client.

        // Create an Apache HttpClientBuilder to be customized by the command line arguments.
        HttpClientBuilder builder = HttpClientBuilder.create().useSystemProperties();

        // Proxy
        if (proxy != null) {
            HttpHost proxyHost = new HttpHost(proxy.getHost(), proxy.getPort(), proxy.getProtocol());
            builder.setProxy(proxyHost);
        }

        // Basic authentication. User & Password
        if (username != null ^ password != null) {
            throw new Exception("'username' and 'password' arguments are required to appear together.");
        }


        // from:  http://stackoverflow.com/questions/19517538/ignoring-ssl-certificate-in-apache-httpclient-4-3
        SSLContextBuilder ssbldr = new SSLContextBuilder();
        ssbldr.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(ssbldr.build(),SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);


        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new PlainConnectionSocketFactory())
                .register("https", sslsf)
                .build();


        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        cm.setMaxTotal(20);//max connection

        System.setProperty("jsse.enableSNIExtension", "false"); //""
        CloseableHttpClient httpClient = builder
                .setSSLSocketFactory(sslsf)
                .setConnectionManager(cm)
                .build();

        client.setHttpclient(httpClient);
        return client;
    }
    public static void main(String... argv) throws Exception {
        URL endpoint = new URL("http://hailataxii.com/taxii-discovery-service");
        String username = "guest";
        String password = "guest";
        TaxiiConnectionConfig config = new TaxiiConnectionConfig();
        config = config.withConnectionType(ConnectionType.DISCOVER)
                       .withEndpoint(endpoint)
                       .withUsername(username)
                       .withCollection("guest.Abuse_ch")
                       .withPassword(password);
        //TaxiiHandler handler = new TaxiiHandler(config, null);
        //handler.run();
        //discoverPollingClient(null, endpoint, username, password, context);
    }
}
