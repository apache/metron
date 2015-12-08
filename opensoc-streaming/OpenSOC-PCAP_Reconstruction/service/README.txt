'rest' module contains the web layer configuration for REST API which communicates with HBase and fetches pcaps.
Spring frameworks 'org.springframework.web.servlet.DispatcherServlet' is configured to intercept all requests (/*) and the 
application context is built using the configuration file 'ipcap-config.xml'.

REST APIs :
1. http://{hostname:port}//cisco-rest/pcapGetter/getPcapsByKeys?<query params here. Refer documentation below>
2. http://mon.hw.com:8090/cisco-rest-0.0.5-SNAPSHOT/pcapGetter/getPcapsByKeyRange?<query params here. Refer documentation below>
3. http://mon.hw.com:8090/cisco-rest-0.0.5-SNAPSHOT/pcapGetter/getPcapsByIdentifiers?<query params here. Refer documentation below>

Refer the wiki documentation for further details : https://hwcsco.atlassian.net/wiki/pages/viewpage.action?pageId=5242892	
