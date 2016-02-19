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
package org.apache.metron.dataservices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import javax.servlet.DispatcherType;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.jasper.servlet.JspServlet;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import org.apache.metron.alerts.server.AlertsProcessingServer;
import org.apache.metron.dataservices.modules.guice.AlertsServerModule;
import org.apache.metron.dataservices.modules.guice.DefaultServletModule;
import org.apache.metron.dataservices.modules.guice.DefaultShiroWebModule;

public class Main {
	
	static int port = 9091;
	
	private static final String WEBROOT_INDEX = "/webroot/";
	
	private static final Logger logger = LoggerFactory.getLogger( Main.class );
	
    public static void main(String[] args) throws Exception {


    	Options options = new Options();
    	
    	options.addOption( "homeDir", true, "Home directory for the service" );
    	
    	CommandLineParser parser = new GnuParser();
    	CommandLine cmd = parser.parse( options, args);
    	
    	Properties configProps = new Properties();
    	
    	String homeDir = cmd.getOptionValue("homeDir");
    	
    	if( homeDir.endsWith( "/" ))
    	{
    		homeDir = homeDir.substring(0, homeDir.length()-1);
    	}


    	DOMConfigurator.configure( homeDir + "/log4j.xml" );
    	
    	logger.warn( "DataServices Server starting..." );
    	
    	
    	File configFile = new File( homeDir + "/config.properties" );
    	FileReader configFileReader = new FileReader( configFile );
    	try
    	{
    		configProps.load(configFileReader);
    		
    		Option[] cmdOptions = cmd.getOptions();
    		for( Option opt : cmdOptions )
    		{
    			String argName = opt.getOpt();
    			String argValue = opt.getValue();
	
    			configProps.put(argName, argValue);
    		}
    		
    	}
    	finally
    	{
    		if( configFileReader != null )
    		{
    			configFileReader.close();
    		}
    	}
    	
        WebAppContext context = new WebAppContext();
    	
    	Injector injector = Guice.createInjector(   new DefaultServletModule(configProps), 
    												new AlertsServerModule(configProps),
    												new DefaultShiroWebModule(configProps, context.getServletContext()), 
    												new AbstractModule() {
			
														@Override
														protected void configure() {
															binder().requireExplicitBindings();
															bind(GuiceFilter.class);
															bind( GuiceResteasyBootstrapServletContextListener.class );
															bind( EnvironmentLoaderListener.class );
										
														}
													}
    											);

    	
        injector.getAllBindings();
        injector.createChildInjector().getAllBindings();

        Server server = new Server(port);
        
        /***************************************************
         *************** enable SSL ************************
         ***************************************************/
        
        // HTTP Configuration
        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme("https");
        http_config.setSecurePort(8443);
        http_config.setOutputBufferSize(32768);
        http_config.setRequestHeaderSize(8192);
        http_config.setResponseHeaderSize(8192);
        http_config.setSendServerVersion(true);
        http_config.setSendDateHeader(false);
        // httpConfig.addCustomizer(new ForwardedRequestCustomizer())
        // SSL Context Factory
        SslContextFactory sslContextFactory = new SslContextFactory();
        
        String sslKeystorePath = configProps.getProperty( "sslKeystorePath", "/keystore" );
        logger.debug( "sslKeystorePath: " + sslKeystorePath );
        sslContextFactory.setKeyStorePath( homeDir + sslKeystorePath );
        
        String sslKeystorePassword = configProps.getProperty( "sslKeystorePassword" );
        sslContextFactory.setKeyStorePassword(sslKeystorePassword);
        
        String sslKeyManagerPassword = configProps.getProperty( "sslKeyManagerPassword" );
        if( sslKeyManagerPassword != null && !sslKeyManagerPassword.isEmpty() )
        {
        	sslContextFactory.setKeyManagerPassword(sslKeyManagerPassword);
        }
        
        String sslTruststorePath = configProps.getProperty( "sslTruststorePath" );
        if( sslTruststorePath != null && !sslTruststorePath.isEmpty() )
        {
        	sslContextFactory.setTrustStorePath( homeDir + sslTruststorePath );
        }
        
        String sslTruststorePassword = configProps.getProperty( "sslTruststorePassword" );
        if( sslTruststorePassword != null && !sslTruststorePassword.isEmpty())
        {
        	sslContextFactory.setTrustStorePassword( sslTruststorePassword );
        }
        
        sslContextFactory.setExcludeCipherSuites(
                "SSL_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");

        // SSL HTTP Configuration
        HttpConfiguration https_config = new HttpConfiguration(http_config);
        https_config.addCustomizer(new SecureRequestCustomizer());

        // SSL Connector
        ServerConnector sslConnector = new ServerConnector(server,
            new SslConnectionFactory(sslContextFactory,"http/1.1"),
            new HttpConnectionFactory(https_config));
        sslConnector.setPort(8443);
        server.addConnector(sslConnector);        
        

        FilterHolder guiceFilter = new FilterHolder(injector.getInstance(GuiceFilter.class));
        
        
        /** For JSP support.  Used only for testing and debugging for now.  This came come out
         * once the real consumers for this service are in place
         */
        URL indexUri = Main.class.getResource(WEBROOT_INDEX);
        if (indexUri == null)
        {
            throw new FileNotFoundException("Unable to find resource " + WEBROOT_INDEX);
        }

        // Points to wherever /webroot/ (the resource) is
        URI baseUri = indexUri.toURI();        
        
        // Establish Scratch directory for the servlet context (used by JSP compilation)
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File scratchDir = new File(tempDir.toString(),"embedded-jetty-jsp");

        if (!scratchDir.exists())
        {
            if (!scratchDir.mkdirs())
            {
                throw new IOException("Unable to create scratch directory: " + scratchDir);
            }
        }        
        
        // Set JSP to use Standard JavaC always
        System.setProperty("org.apache.jasper.compiler.disablejsr199","false");	        
        
        context.setAttribute("javax.servlet.context.tempdir",scratchDir);
        context.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
        
        //Ensure the jsp engine is initialized correctly
        JettyJasperInitializer sci = new JettyJasperInitializer();
        ServletContainerInitializersStarter sciStarter = new ServletContainerInitializersStarter(context);
        ContainerInitializer initializer = new ContainerInitializer(sci, null);
        List<ContainerInitializer> initializers = new ArrayList<ContainerInitializer>();
        initializers.add(initializer);

        context.setAttribute("org.eclipse.jetty.containerInitializers", initializers);
        context.addBean(sciStarter, true);        
        
        // Set Classloader of Context to be sane (needed for JSTL)
        // JSP requires a non-System classloader, this simply wraps the
        // embedded System classloader in a way that makes it suitable
        // for JSP to use
        // new URL( "file:///home/prhodes/.m2/repository/javax/servlet/jsp/javax.servlet.jsp-api/2.3.1/javax.servlet.jsp-api-2.3.1.jar" ) 
        ClassLoader jspClassLoader = new URLClassLoader(new URL[] {}, Thread.currentThread().getContextClassLoader());
        context.setClassLoader(jspClassLoader);

        // Add JSP Servlet (must be named "jsp")
        ServletHolder holderJsp = new ServletHolder("jsp",JspServlet.class);
        holderJsp.setInitOrder(0);
        holderJsp.setInitParameter("logVerbosityLevel","DEBUG");
        holderJsp.setInitParameter("fork","false");
        holderJsp.setInitParameter("xpoweredBy","false");
        holderJsp.setInitParameter("compilerTargetVM","1.7");
        holderJsp.setInitParameter("compilerSourceVM","1.7");
        holderJsp.setInitParameter("keepgenerated","true");
        context.addServlet(holderJsp,"*.jsp");
        //context.addServlet(holderJsp,"*.jspf");
        //context.addServlet(holderJsp,"*.jspx");

        // Add Default Servlet (must be named "default")
        ServletHolder holderDefault = new ServletHolder("default",DefaultServlet.class);
        holderDefault.setInitParameter("resourceBase",baseUri.toASCIIString());
        holderDefault.setInitParameter("dirAllowed","true");
        context.addServlet(holderDefault,"/");         
        
        /** end "for JSP support */
        

        context.setResourceBase(baseUri.toASCIIString());
        
        context.setInitParameter("resteasy.guice.modules", "org.apache.metron.dataservices.modules.guice.RestEasyModule");
        context.setInitParameter("resteasy.servlet.mapping.prefix", "/rest");
        
        context.addEventListener(injector.getInstance(GuiceResteasyBootstrapServletContextListener.class));
        context.addFilter(guiceFilter, "/*", EnumSet.allOf(DispatcherType.class));
        
        server.setHandler(context);
        server.start();
        
        AlertsProcessingServer alertsServer = injector.getInstance(AlertsProcessingServer.class);
        
        alertsServer.startProcessing();
        
        server.join();
    }
}
