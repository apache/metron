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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.log4j.Logger;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;
import org.apache.metron.pcap.filter.query.QueryPcapFilter;
import org.apache.metron.pcap.mr.PcapJob;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

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
  private static ThreadLocal<Configuration> CONFIGURATION = new ThreadLocal<Configuration>() {
    /**
     * Returns the current thread's "initial value" for this
     * thread-local variable.  This method will be invoked the first
     * time a thread accesses the variable with the {@link #get}
     * method, unless the thread previously invoked the {@link #set}
     * method, in which case the {@code initialValue} method will not
     * be invoked for the thread.  Normally, this method is invoked at
     * most once per thread, but it may be invoked again in case of
     * subsequent invocations of {@link #remove} followed by {@link #get}.
     * <p>
     * <p>This implementation simply returns {@code null}; if the
     * programmer desires thread-local variables to have an initial
     * value other than {@code null}, {@code ThreadLocal} must be
     * subclassed, and this method overridden.  Typically, an
     * anonymous inner class will be used.
     *
     * @return the initial value for this thread-local
     */
    @Override
    protected Configuration initialValue() {
      return new Configuration();
    }
  };
  PcapJob queryUtil = new PcapJob();

  protected PcapJob getQueryUtil() {
    return queryUtil;
  }

  private static boolean isValidPort(String port) {
    if( port != null && !port.equals("") ) {
      try {
        Integer.parseInt(port);
        return true;
      }
      catch(Exception e) {
        return false;
      }
    }
    return false;
  }

  /**
   * Enable filtering PCAP results by query filter string and start/end packet TS
   *
   * @param query Filter results based on this query
   * @param startTime Only return packets originating after this start time
   * @param endTime Only return packets originating before this end time
   * @param servlet_response
   * @return REST response
   * @throws IOException
   */
  @GET
  @Path("/pcapGetter/getPcapsByQuery")
  public Response getPcapsByIdentifiers(
          @QueryParam ("query") String query,
          @DefaultValue("-1") @QueryParam ("startTime")long startTime,
          @DefaultValue("-1") @QueryParam ("endTime")long endTime,
          @Context HttpServletResponse servlet_response)

          throws IOException {
    PcapsResponse response = new PcapsResponse();
    try {
      if (startTime < 0) {
        startTime = 0L;
      }
      if (endTime < 0) {
        endTime = System.currentTimeMillis();
      }
      if(query == null) {
        return Response.serverError().status(Response.Status.NO_CONTENT)
                .entity("Query is null").build();
      }
      //convert to nanoseconds since the epoch
      startTime = TimestampConverters.MILLISECONDS.toNanoseconds(startTime);
      endTime = TimestampConverters.MILLISECONDS.toNanoseconds(endTime);
      if(LOGGER.isDebugEnabled()) {
        LOGGER.debug("Query received: " + query);
      }
      response.setPcaps(getQueryUtil().query(new org.apache.hadoop.fs.Path(ConfigurationUtil.getPcapOutputPath())
              , new org.apache.hadoop.fs.Path(ConfigurationUtil.getTempQueryOutputPath())
              , startTime
              , endTime
              , query
              , CONFIGURATION.get()
              , FileSystem.get(CONFIGURATION.get())
              , new QueryPcapFilter.Configurator()
              )
      );

    } catch (Exception e) {
      LOGGER.error("Exception occurred while fetching Pcaps by identifiers :",
              e);
      throw new WebApplicationException("Unable to fetch Pcaps via MR job", e);
    }

    // return http status '200 OK' along with the complete pcaps response file,
    // and headers
    return Response
            .ok(response.getPcaps(), MediaType.APPLICATION_OCTET_STREAM)
            .status(200).build();
  }

  /**
   * Enable filtering PCAP results by fixed properties and start/end packet TS
   *
   * @param srcIp filter value
   * @param dstIp filter value
   * @param protocol filter value
   * @param srcPort filter value
   * @param dstPort filter value
   * @param startTime filter value
   * @param endTime filter value
   * @param includeReverseTraffic Indicates if filter should check swapped src/dest addresses and IPs
   * @param servlet_response
   * @return REST response
   * @throws IOException
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

    if (!isValidPort(srcPort)) {
      return Response.serverError().status(Response.Status.NO_CONTENT)
              .entity("'srcPort' must not be null, empty or a non-integer").build();
    }

    if (!isValidPort(dstPort)) {
      return Response.serverError().status(Response.Status.NO_CONTENT)
              .entity("'dstPort' must not be null, empty or a non-integer").build();
    }

    final boolean includeReverseTrafficF = includeReverseTraffic;
    PcapsResponse response = new PcapsResponse();
    try {
      if(startTime < 0) {
        startTime = 0L;
      }
      if(endTime < 0) {
        endTime = System.currentTimeMillis();
      }

      //convert to nanoseconds since the epoch
      startTime = TimestampConverters.MILLISECONDS.toNanoseconds(startTime);
      endTime = TimestampConverters.MILLISECONDS.toNanoseconds(endTime);
      EnumMap<Constants.Fields, String> query = new EnumMap<Constants.Fields, String>(Constants.Fields.class) {{
                                      if(srcIp != null) {
                                        put(Constants.Fields.SRC_ADDR, srcIp);
                                      }
                                      if(dstIp != null) {
                                        put(Constants.Fields.DST_ADDR, dstIp);
                                      }
                                      if(srcPort != null) {
                                        put(Constants.Fields.SRC_PORT, srcPort);
                                      }
                                      if(dstPort != null) {
                                        put(Constants.Fields.DST_PORT, dstPort);
                                      }
                                      if(protocol != null) {
                                        put(Constants.Fields.PROTOCOL, protocol);
                                      }
                                      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC, "" + includeReverseTrafficF);
                                    }};
      if(LOGGER.isDebugEnabled()) {
        LOGGER.debug("Query received: " + Joiner.on(",").join(query.entrySet()));
      }
      response.setPcaps(getQueryUtil().query(new org.apache.hadoop.fs.Path(ConfigurationUtil.getPcapOutputPath())
                                    , new org.apache.hadoop.fs.Path(ConfigurationUtil.getTempQueryOutputPath())
                                    , startTime
                                    , endTime
                                    , query
                                    , CONFIGURATION.get()
                                    , FileSystem.get(CONFIGURATION.get())
                                    , new FixedPcapFilter.Configurator()
                                    )
                     );

    } catch (Exception e) {
      LOGGER.error("Exception occurred while fetching Pcaps by identifiers :",
              e);
      throw new WebApplicationException("Unable to fetch Pcaps via MR job", e);
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
