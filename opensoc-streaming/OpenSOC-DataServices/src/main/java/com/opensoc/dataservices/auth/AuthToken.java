package com.opensoc.dataservices.auth;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthToken {

	private static final Logger logger = LoggerFactory.getLogger( AuthToken.class );
	
	public static String generateToken( final Properties configProps ) throws Exception
	{
		
		KeyStore ks = KeyStore.getInstance("JCEKS");
		String keystoreFile = configProps.getProperty( "keystoreFile" );
		logger.info( "keystoreFile: " + keystoreFile );
		
		String keystorePassword = configProps.getProperty( "keystorePassword" );
		logger.info( "keystorePassword: " + keystorePassword );
		
		String keystoreAlias = configProps.getProperty( "authTokenAlias" );
		logger.info( "keystoreAlias: " + keystoreAlias );
		
		FileInputStream fis = null;
		try {
			fis = new FileInputStream( keystoreFile );
			ks.load(fis, keystorePassword.toCharArray() );
		}
		catch( Exception e )
		{
			logger.error( "Error opening keyfile:", e );
			throw e;
		}
		finally {
			fis.close();
		}
		
		KeyStore.ProtectionParameter protParam =
		        new KeyStore.PasswordProtection(keystorePassword.toCharArray());
		KeyStore.SecretKeyEntry  secretKeyEntry = (KeyStore.SecretKeyEntry)ks.getEntry(keystoreAlias, protParam);
		
		SecretKey key = secretKeyEntry.getSecretKey();

		
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, key);		
		String tokenString = "OpenSOC_AuthToken:" + System.currentTimeMillis();
		
		byte[] encryptedData = cipher.doFinal(tokenString.getBytes());	
		
		String base64Token = new String( Base64.encodeBase64(encryptedData) );
		
		// System.out.println( "base64Token: " + base64Token );
		
		return base64Token;
		
	}
	
	public static boolean validateToken( final Properties configProps, String authToken ) throws Exception
	{
		KeyStore ks = KeyStore.getInstance("JCEKS");
		String keystoreFile = configProps.getProperty( "keystoreFile" );
		String keystorePassword = configProps.getProperty( "keystorePassword" );
		String keystoreAlias = configProps.getProperty( "authTokenAlias" );
		long tokenMaxAgeInMilliseconds = Long.parseLong( configProps.getProperty( "authTokenMaxAge", "600000" ));
		
		FileInputStream fis = null;
		try {
			fis = new FileInputStream( keystoreFile );
			ks.load(fis, keystorePassword.toCharArray() );
		}
		finally {
			if( fis != null) {
				fis.close();
			}
		}
		
		KeyStore.ProtectionParameter protParam =
		        new KeyStore.PasswordProtection(keystorePassword.toCharArray());
		KeyStore.SecretKeyEntry  secretKeyEntry = (KeyStore.SecretKeyEntry)ks.getEntry(keystoreAlias, protParam);
		
		SecretKey key = secretKeyEntry.getSecretKey();
		
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, key);		
		
		byte[] encryptedBytes = Base64.decodeBase64(authToken);
		
		byte[] unencryptedBytes = cipher.doFinal(encryptedBytes);
		String clearTextToken = new String( unencryptedBytes );
		
		System.out.println( "clearTextToken: " + clearTextToken );
		String[] tokenParts = clearTextToken.split( ":" );
		
		if( tokenParts[0].equals( "OpenSOC_AuthToken" ))
		{
			long now = System.currentTimeMillis();
			long tokenTime = Long.parseLong(tokenParts[1]);
			
			if( now > (tokenTime + tokenMaxAgeInMilliseconds ))
			{
				return false;
			}
			else
			{
				return true;
			}
		}
		else
		{
			return false;
		}
		
	}
	
	public static void main( String[] args ) throws Exception
	{
		
    	Options options = new Options();
    	
    	options.addOption( "keystoreFile", true, "Keystore File" );
    	options.addOption( "keystorePassword", true, "Keystore Password" );
    	options.addOption( "authTokenAlias", true, "");
    	
    	CommandLineParser parser = new GnuParser();
    	CommandLine cmd = parser.parse( options, args);
		
		
		try
		{
			KeyStore ks = KeyStore.getInstance("JCEKS");

			String keystorePassword = cmd.getOptionValue("keystorePassword");
			String keystoreFile = cmd.getOptionValue("keystoreFile");
			String authTokenAlias = cmd.getOptionValue("authTokenAlias");

			ks.load(null, keystorePassword.toCharArray());

			
			// generate a key and store it in the keystore...
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			SecretKey key = keyGen.generateKey();
			
			KeyStore.ProtectionParameter protParam =
			        new KeyStore.PasswordProtection(keystorePassword.toCharArray());
			
			
			KeyStore.SecretKeyEntry skEntry =
			        new KeyStore.SecretKeyEntry(key);
			
			ks.setEntry(authTokenAlias, skEntry, protParam);
			
			java.io.FileOutputStream fos = null;
		    try {
		        
		    	fos = new java.io.FileOutputStream(keystoreFile);
		        ks.store(fos, keystorePassword.toCharArray());
		    } 
		    finally {
		        
		    	if (fos != null) {
		            fos.close();
		        }
		    }
			
		    
		    System.out.println( "done" );
		    
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
	}
}
