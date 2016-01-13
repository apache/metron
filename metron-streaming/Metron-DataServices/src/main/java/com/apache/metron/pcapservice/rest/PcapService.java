package com.apache.metron.pcapservice.rest;

import java.io.IOException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import com.apache.metron.helpers.services.PcapServiceCli;


public class PcapService {

	public static void main(String[] args) throws IOException {

		PcapServiceCli cli = new PcapServiceCli(args);
		cli.parse();
		
		Server server = new Server(cli.getPort());
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		ServletHolder h = new ServletHolder(new HttpServletDispatcher());
		h.setInitParameter("javax.ws.rs.Application", "com.apache.metron.pcapservice.rest.JettyServiceRunner");
		context.addServlet(h, "/*");
		server.setHandler(context);
		try {
			server.start();
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}