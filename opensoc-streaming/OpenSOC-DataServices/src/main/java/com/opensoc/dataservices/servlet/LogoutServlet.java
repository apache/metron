package com.opensoc.dataservices.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogoutServlet extends HttpServlet 
{
	private static final Logger logger = LoggerFactory.getLogger( LogoutServlet.class );
	
	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		doPost( req, resp );
	}
	
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		logger.info( "Doing logout here..." );
		
		Subject currentUser = SecurityUtils.getSubject();

		currentUser.logout();
		
		Cookie authCookie = new Cookie("authToken", "Logout" );
		authCookie.setMaxAge( 0 );
		resp.addCookie(authCookie);
		
		resp.sendRedirect( "/login.jsp" );
	}	
}