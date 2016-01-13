package com.apache.metron.dataservices.auth;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ServerResponse;

import com.google.inject.Inject;

@AuthTokenFilter
@Provider
public class RestSecurityInterceptor implements javax.ws.rs.container.ContainerRequestFilter {

	private static final ServerResponse ACCESS_DENIED = new ServerResponse("Access denied for this resource", 401, new Headers<Object>());;
	
	@Inject
	private Properties configProps;
	
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		
		// get our token...		
		Map<String, Cookie> cookies = requestContext.getCookies();
		
		Cookie authTokenCookie = cookies.get( "authToken" );
		if( authTokenCookie == null )
		{
			requestContext.abortWith(ACCESS_DENIED );
			return;			
		}
		
		String authToken = authTokenCookie.getValue();
		try {
			
			if( ! AuthToken.validateToken(configProps, authToken) )
			{
				requestContext.abortWith(ACCESS_DENIED );
				return;	
			}
		} 
		catch (Exception e) {

			e.printStackTrace();
			requestContext.abortWith(ACCESS_DENIED );
			return;
		}
	
		// if the token is good, just return...
		
	}
}