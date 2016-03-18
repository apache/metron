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
package org.apache.metron.pcapservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import org.apache.metron.pcap.PcapUtils;

@Path("/")
public class PcapReceiverImplRestEasy {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = Logger
			.getLogger(PcapReceiverImplRestEasy.class);

	/** The Constant HEADER_CONTENT_DISPOSITION_NAME. */
	private static final String HEADER_CONTENT_DISPOSITION_NAME = "Content-Disposition";

	/** The Constant HEADER_CONTENT_DISPOSITION_VALUE. */
	private static final String HEADER_CONTENT_DISPOSITION_VALUE = "attachment; filename=\"managed-threat.pcap\"";

	/** partial response key header name. */
	private static final String HEADER_PARTIAL_RESPONE_KEY = "lastRowKey";

	@GET
	@Path("pcapGetter/getPcapsByKeys")
	public Response getPcapsByKeys(
			@QueryParam("keys") List<String> keys,
			@QueryParam("lastRowKey") String lastRowKey,
			@DefaultValue("-1") @QueryParam("startTime") long startTime,
			@DefaultValue("-1") @QueryParam("endTime") long endTime,
			@QueryParam("includeDuplicateLastRow") boolean includeDuplicateLastRow,
			@QueryParam("includeReverseTraffic") boolean includeReverseTraffic,
			@QueryParam("maxResponseSize") String maxResponseSize,
			@Context HttpServletResponse response) throws IOException {
		PcapsResponse pcapResponse = null;

		if (keys == null || keys.size() == 0)
			return Response.serverError().status(Response.Status.NO_CONTENT)
					.entity("'keys' must not be null or empty").build();

		try {
			IPcapGetter pcapGetter = PcapGetterHBaseImpl.getInstance();
			pcapResponse = pcapGetter.getPcaps(parseKeys(keys), lastRowKey,
					startTime, endTime, includeReverseTraffic,
					includeDuplicateLastRow,
					ConfigurationUtil.validateMaxResultSize(maxResponseSize));
			LOGGER.info("pcaps response in REST layer ="
					+ pcapResponse.toString());

			// return http status '204 No Content' if the pcaps response size is
			// 0
			if (pcapResponse == null || pcapResponse.getResponseSize() == 0) {

				return Response.status(Response.Status.NO_CONTENT).build();
			}

			// return http status '206 Partial Content', the partial response
			// file and
			// 'lastRowKey' header , if the pcaps response status is 'PARTIAL'

			response.setHeader(HEADER_CONTENT_DISPOSITION_NAME,
					HEADER_CONTENT_DISPOSITION_VALUE);

			if (pcapResponse.getStatus() == PcapsResponse.Status.PARTIAL) {

				response.setHeader(HEADER_PARTIAL_RESPONE_KEY,
						pcapResponse.getLastRowKey());

				return Response
						.ok(pcapResponse.getPcaps(),
								MediaType.APPLICATION_OCTET_STREAM).status(206)
						.build();

			}

		} catch (IOException e) {
			LOGGER.error(
					"Exception occurred while fetching Pcaps for the keys :"
							+ keys.toString(), e);
			throw e;
		}

		// return http status '200 OK' along with the complete pcaps response
		// file,
		// and headers
		// return new ResponseEntity<byte[]>(pcapResponse.getPcaps(), headers,
		// HttpStatus.OK);

		return Response
				.ok(pcapResponse.getPcaps(), MediaType.APPLICATION_OCTET_STREAM)
				.status(200).build();

	}
	
	
	@GET
	@Path("/pcapGetter/getPcapsByKeyRange")

	  public Response getPcapsByKeyRange(
	      @QueryParam("startKey") String startKey,
	      @QueryParam("endKey")String endKey,
	      @QueryParam("maxResponseSize") String maxResponseSize,
	      @DefaultValue("-1") @QueryParam("startTime")long startTime,
	      @DefaultValue("-1") @QueryParam("endTime") long endTime, 
	      @Context HttpServletResponse servlet_response) throws IOException {

		if (startKey == null || startKey.equals(""))
			return Response.serverError().status(Response.Status.NO_CONTENT)
					.entity("'start key' must not be null or empty").build();
		
		if (startKey == null || startKey.equals(""))
			return Response.serverError().status(Response.Status.NO_CONTENT)
					.entity("'end key' must not be null or empty").build();
		
		
	    byte[] response = null;
	    try {
	      IPcapScanner pcapScanner = PcapScannerHBaseImpl.getInstance();
	      response = pcapScanner.getPcaps(startKey, endKey,
	          ConfigurationUtil.validateMaxResultSize(maxResponseSize), startTime,
	          endTime);
	      if (response == null || response.length == 0) {
	    	  
	    	  return Response.status(Response.Status.NO_CONTENT).build();
	        
	      }
	      servlet_response.setHeader(HEADER_CONTENT_DISPOSITION_NAME,
					HEADER_CONTENT_DISPOSITION_VALUE);

	    } catch (IOException e) {
	      LOGGER.error(
	          "Exception occurred while fetching Pcaps for the key range : startKey="
	              + startKey + ", endKey=" + endKey, e);
	      throw e;
	    }
	    // return http status '200 OK' along with the complete pcaps response file,
	    // and headers
	    
		return Response
				.ok(response, MediaType.APPLICATION_OCTET_STREAM)
				.status(200).build();
	  }

	  /*
	   * (non-Javadoc)
	   * 
	   * @see
	   * com.cisco.opensoc.hbase.client.IPcapReceiver#getPcapsByIdentifiers(java.lang
	   * .String, java.lang.String, java.lang.String, java.lang.String,
	   * java.lang.String, long, long, boolean,
	   * javax.servlet.http.HttpServletResponse)
	   */
	  
	@GET
	@Path("/pcapGetter/getPcapsByIdentifiers")

	  public Response getPcapsByIdentifiers(
	      @QueryParam ("srcIp") String srcIp, 
	      @QueryParam ("dstIp") String dstIp,
	      @QueryParam ("protocol") String protocol, 
	      @QueryParam ("srcPort") String srcPort,
	      @QueryParam ("dstPort") String dstPort,
	      @DefaultValue("-1") @QueryParam ("startTime")long startTime,
	      @DefaultValue("-1") @QueryParam ("endTime")long endTime,
	      @DefaultValue("false") @QueryParam ("includeReverseTraffic") boolean includeReverseTraffic,
	      @Context HttpServletResponse servlet_response)
	      
	      throws IOException {
		
		if (srcIp == null || srcIp.equals(""))
			return Response.serverError().status(Response.Status.NO_CONTENT)
					.entity("'srcIp' must not be null or empty").build();
		
		if (dstIp == null || dstIp.equals(""))
			return Response.serverError().status(Response.Status.NO_CONTENT)
					.entity("'dstIp' must not be null or empty").build();
		
		if (protocol == null || protocol.equals(""))
			return Response.serverError().status(Response.Status.NO_CONTENT)
					.entity("'protocol' must not be null or empty").build();
		
		if (srcPort == null || srcPort.equals(""))
			return Response.serverError().status(Response.Status.NO_CONTENT)
					.entity("'srcPort' must not be null or empty").build();
		
		if (dstPort == null || dstPort.equals(""))
			return Response.serverError().status(Response.Status.NO_CONTENT)
					.entity("'dstPort' must not be null or empty").build();
		
	
	    PcapsResponse response = null;
	    try {
	      String sessionKey = PcapUtils.getPartialSessionKey(srcIp, dstIp, protocol,
	          srcPort, dstPort);
	      LOGGER.info("sessionKey =" + sessionKey);
	      IPcapGetter pcapGetter = PcapGetterHBaseImpl.getInstance();
	      response = pcapGetter.getPcaps(Arrays.asList(sessionKey), null,
	          startTime, endTime, includeReverseTraffic, false,
	          ConfigurationUtil.getDefaultResultSize());
	      if (response == null || response.getResponseSize() == 0) {
	         return Response.status(Response.Status.NO_CONTENT).build();
	      }
	      servlet_response.setHeader(HEADER_CONTENT_DISPOSITION_NAME,
					HEADER_CONTENT_DISPOSITION_VALUE);

	    } catch (IOException e) {
	      LOGGER.error("Exception occurred while fetching Pcaps by identifiers :",
	          e);
	      throw e;
	    }
	    // return http status '200 OK' along with the complete pcaps response file,
	    // and headers
	    return Response
				.ok(response.getPcaps(), MediaType.APPLICATION_OCTET_STREAM)
				.status(200).build();
	  }
	/**
	 * This method parses the each value in the List using delimiter ',' and
	 * builds a new List;.
	 * 
	 * @param keys
	 *            list of keys to be parsed
	 * @return list of keys
	 */
	@VisibleForTesting
	List<String> parseKeys(List<String> keys) {
		// Assert.notEmpty(keys);
		List<String> parsedKeys = new ArrayList<String>();
		for (String key : keys) {
			parsedKeys.addAll(Arrays.asList(StringUtils.split(
					StringUtils.trim(key), ",")));
		}
		return parsedKeys;
	}
}
