package org.apache.metron.pcapservice.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.metron.pcapservice.PcapReceiverImplRestEasy;

public class JettyServiceRunner extends Application  {
	

	private static Set services = new HashSet(); 
		
	public  JettyServiceRunner() {     
		// initialize restful services   
		services.add(new PcapReceiverImplRestEasy());  
	}
	@Override
	public  Set getSingletons() {
		return services;
	}  
	public  static Set getServices() {  
		return services;
	} 
}