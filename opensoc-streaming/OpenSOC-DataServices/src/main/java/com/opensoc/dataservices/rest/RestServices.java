package com.opensoc.dataservices.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestServices extends Application 
{
	private static final Logger logger = LoggerFactory.getLogger( RestServices.class );
	
	private static Set services = new HashSet();

	public RestServices() 
	{
		// initialize restful services
		services.add(new Index());
	}

	@Override
	public Set getSingletons() 
	{
		return services;
	}

	public static Set getServices() 
	{
		return services;
	}
}