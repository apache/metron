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
package org.apache.metron.pcap.query;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.Constants;
import org.apache.metron.common.hadoop.SequenceFileIterable;
import org.apache.metron.common.system.Clock;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;
import org.apache.metron.pcap.filter.query.QueryPcapFilter;
import org.apache.metron.pcap.mr.PcapJob;
import org.apache.metron.pcap.writer.ResultsWriter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class PcapCliTest {

  @Mock
  private PcapJob jobRunner;
  @Mock
  private ResultsWriter resultsWriter;
  @Mock
  private Clock clock;
  private String execDir;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    doCallRealMethod().when(jobRunner).writeResults(anyObject(), anyObject(), anyObject(), anyInt(), anyObject());
    execDir = System.getProperty("user.dir");
  }

  @Test
  public void runs_fixed_pcap_filter_job_with_default_argument_list() throws Exception {
    String[] args = {
            "fixed",
            "-start_time", "500",
            "-ip_src_addr", "192.168.1.1",
            "-ip_dst_addr", "192.168.1.2",
            "-ip_src_port", "8081",
            "-ip_dst_port", "8082",
            "-protocol", "6",
            "-packet_filter", "`casey`"
    };
    List<byte[]> pcaps = Arrays.asList(new byte[][]{asBytes("abc"), asBytes("def"), asBytes("ghi")});
    Iterator iterator = pcaps.iterator();
    SequenceFileIterable iterable = mock(SequenceFileIterable.class);
    when(iterable.iterator()).thenReturn(iterator);

    Path base_path = new Path(CliParser.BASE_PATH_DEFAULT);
    Path base_output_path = new Path(CliParser.BASE_OUTPUT_PATH_DEFAULT);
    HashMap<String, String> query = new HashMap<String, String>() {{
      put(Constants.Fields.SRC_ADDR.getName(), "192.168.1.1");
      put(Constants.Fields.DST_ADDR.getName(), "192.168.1.2");
      put(Constants.Fields.SRC_PORT.getName(), "8081");
      put(Constants.Fields.DST_PORT.getName(), "8082");
      put(Constants.Fields.PROTOCOL.getName(), "6");
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), "false");
      put(PcapHelper.PacketFields.PACKET_FILTER.getName(), "`casey`");
    }};

    when(jobRunner.query(eq(base_path), eq(base_output_path), anyLong(), anyLong(), anyInt(), eq(query), isA(Configuration.class), isA(FileSystem.class), isA(FixedPcapFilter.Configurator.class))).thenReturn(iterable);

    PcapCli cli = new PcapCli(jobRunner, resultsWriter, clock -> "random_prefix");
    assertThat("Expect no errors on run", cli.run(args), equalTo(0));
    Mockito.verify(resultsWriter).write(isA(Configuration.class), eq(pcaps), eq("file:" + execDir + "/pcap-data-random_prefix+0001.pcap"));
  }

  @Test
  public void runs_fixed_pcap_filter_job_with_full_argument_list_and_default_dateformat() throws Exception {
    String[] args = {
            "fixed",
            "-start_time", "500",
            "-end_time", "1000",
            "-base_path", "/base/path",
            "-base_output_path", "/base/output/path",
            "-ip_src_addr", "192.168.1.1",
            "-ip_dst_addr", "192.168.1.2",
            "-ip_src_port", "8081",
            "-ip_dst_port", "8082",
            "-protocol", "6",
            "-include_reverse",
            "-num_reducers", "10",
            "-records_per_file", "1000"
    };
    List<byte[]> pcaps = Arrays.asList(new byte[][]{asBytes("abc"), asBytes("def"), asBytes("ghi")});
    Iterator iterator = pcaps.iterator();
    SequenceFileIterable iterable = mock(SequenceFileIterable.class);
    when(iterable.iterator()).thenReturn(iterator);

    Path base_path = new Path("/base/path");
    Path base_output_path = new Path("/base/output/path");
    Map<String, String> query = new HashMap<String, String>() {{
      put(Constants.Fields.SRC_ADDR.getName(), "192.168.1.1");
      put(Constants.Fields.DST_ADDR.getName(), "192.168.1.2");
      put(Constants.Fields.SRC_PORT.getName(), "8081");
      put(Constants.Fields.DST_PORT.getName(), "8082");
      put(Constants.Fields.PROTOCOL.getName(), "6");
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), "true");
    }};

    when(jobRunner.query(eq(base_path), eq(base_output_path), anyLong(), anyLong(), anyInt(), eq(query), isA(Configuration.class), isA(FileSystem.class), isA(FixedPcapFilter.Configurator.class))).thenReturn(iterable);

    PcapCli cli = new PcapCli(jobRunner, resultsWriter, clock -> "random_prefix");
    assertThat("Expect no errors on run", cli.run(args), equalTo(0));
    Mockito.verify(resultsWriter).write(isA(Configuration.class), eq(pcaps), eq("file:" + execDir + "/pcap-data-random_prefix+0001.pcap"));
  }

  @Test
  public void runs_fixed_pcap_filter_job_with_full_argument_list() throws Exception {
    String[] args = {
            "fixed",
            "-start_time", "2016-06-13-18:35.00",
            "-end_time", "2016-06-15-18:35.00",
            "-date_format", "yyyy-MM-dd-HH:mm.ss",
            "-base_path", "/base/path",
            "-base_output_path", "/base/output/path",
            "-ip_src_addr", "192.168.1.1",
            "-ip_dst_addr", "192.168.1.2",
            "-ip_src_port", "8081",
            "-ip_dst_port", "8082",
            "-protocol", "6",
            "-include_reverse",
            "-num_reducers", "10",
            "-records_per_file", "1000"
    };
    List<byte[]> pcaps = Arrays.asList(new byte[][]{asBytes("abc"), asBytes("def"), asBytes("ghi")});
    Iterator iterator = pcaps.iterator();
    SequenceFileIterable iterable = mock(SequenceFileIterable.class);
    when(iterable.iterator()).thenReturn(iterator);

    Path base_path = new Path("/base/path");
    Path base_output_path = new Path("/base/output/path");
    Map<String, String> query = new HashMap<String, String>() {{
      put(Constants.Fields.SRC_ADDR.getName(), "192.168.1.1");
      put(Constants.Fields.DST_ADDR.getName(), "192.168.1.2");
      put(Constants.Fields.SRC_PORT.getName(), "8081");
      put(Constants.Fields.DST_PORT.getName(), "8082");
      put(Constants.Fields.PROTOCOL.getName(), "6");
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), "true");
    }};

    long startAsNanos = asNanos("2016-06-13-18:35.00", "yyyy-MM-dd-HH:mm.ss");
    long endAsNanos = asNanos("2016-06-15-18:35.00", "yyyy-MM-dd-HH:mm.ss");
    when(jobRunner.query(eq(base_path), eq(base_output_path), eq(startAsNanos), eq(endAsNanos), anyInt(), eq(query), isA(Configuration.class), isA(FileSystem.class), isA(FixedPcapFilter.Configurator.class))).thenReturn(iterable);

    PcapCli cli = new PcapCli(jobRunner, resultsWriter, clock -> "random_prefix");
    assertThat("Expect no errors on run", cli.run(args), equalTo(0));
    Mockito.verify(resultsWriter).write(isA(Configuration.class), eq(pcaps), eq("file:" + execDir + "/pcap-data-random_prefix+0001.pcap"));
  }

  private long asNanos(String inDate, String format) throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    Date date = sdf.parse(inDate);
    return TimestampConverters.MILLISECONDS.toNanoseconds(date.getTime());
  }

  private byte[] asBytes(String val) {
    return val.getBytes(StandardCharsets.UTF_8);
  }

  @Test
  public void runs_query_pcap_filter_job_with_default_argument_list() throws Exception {
    String[] args = {
            "query",
            "-start_time", "500",
            "-query", "some query string"
    };
    List<byte[]> pcaps = Arrays.asList(new byte[][]{asBytes("abc"), asBytes("def"), asBytes("ghi")});
    Iterator iterator = pcaps.iterator();
    SequenceFileIterable iterable = mock(SequenceFileIterable.class);
    when(iterable.iterator()).thenReturn(iterator);

    Path base_path = new Path(CliParser.BASE_PATH_DEFAULT);
    Path base_output_path = new Path(CliParser.BASE_OUTPUT_PATH_DEFAULT);
    String query = "some query string";

    when(jobRunner.query(eq(base_path), eq(base_output_path), anyLong(), anyLong(), anyInt(), eq(query), isA(Configuration.class), isA(FileSystem.class), isA(QueryPcapFilter.Configurator.class))).thenReturn(iterable);

    PcapCli cli = new PcapCli(jobRunner, resultsWriter, clock -> "random_prefix");
    assertThat("Expect no errors on run", cli.run(args), equalTo(0));
    Mockito.verify(resultsWriter).write(isA(Configuration.class), eq(pcaps), eq("file:" + execDir + "/pcap-data-random_prefix+0001.pcap"));
  }

  @Test
  public void runs_query_pcap_filter_job_with_full_argument_list() throws Exception {
    String[] args = {
            "query",
            "-start_time", "500",
            "-end_time", "1000",
            "-num_reducers", "10",
            "-base_path", "/base/path",
            "-base_output_path", "/base/output/path",
            "-query", "some query string",
            "-records_per_file", "1000"
    };
    List<byte[]> pcaps = Arrays.asList(new byte[][]{asBytes("abc"), asBytes("def"), asBytes("ghi")});
    Iterator iterator = pcaps.iterator();
    SequenceFileIterable iterable = mock(SequenceFileIterable.class);
    when(iterable.iterator()).thenReturn(iterator);

    Path base_path = new Path("/base/path");
    Path base_output_path = new Path("/base/output/path");
    String query = "some query string";

    when(jobRunner.query(eq(base_path), eq(base_output_path), anyLong(), anyLong(), anyInt(), eq(query), isA(Configuration.class), isA(FileSystem.class), isA(QueryPcapFilter.Configurator.class))).thenReturn(iterable);

    PcapCli cli = new PcapCli(jobRunner, resultsWriter, clock -> "random_prefix");
    assertThat("Expect no errors on run", cli.run(args), equalTo(0));
    Mockito.verify(resultsWriter).write(isA(Configuration.class), eq(pcaps), eq("file:" + execDir + "/pcap-data-random_prefix+0001.pcap"));
  }

  // INVALID OPTION CHECKS

  @Test
  public void invalid_fixed_filter_arg_prints_help() throws Exception {
    String[] args = {
            "fixed",
            "-start_time", "500",
            "-end_time", "1000",
            "-num_reducers", "10",
            "-base_path", "/base/path",
            "-base_output_path", "/base/output/path",
            "-query", "THIS IS AN ERROR"
    };
    assertCliError(args, "Fixed", "Unrecognized option: -query");
  }

  /**
   *
   * @param args PcapJob args
   * @param type Fixed|Query
   * @param optMsg Expected error message
   */
  public void assertCliError(String[] args, String type, String optMsg) {
    PrintStream originalOutStream = System.out;
    PrintStream originalErrOutStream = System.err;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      PrintStream outStream = new PrintStream(new BufferedOutputStream(bos));
      System.setOut(outStream);

      ByteArrayOutputStream ebos = new ByteArrayOutputStream();
      PrintStream errOutStream = new PrintStream(new BufferedOutputStream(ebos));
      System.setErr(errOutStream);

      PcapCli cli = new PcapCli(jobRunner, resultsWriter, clock -> "random_prefix");
      assertThat("Expect errors on run", cli.run(args), equalTo(-1));
      assertThat("Expect missing required option error: " + ebos.toString(), ebos.toString().contains(optMsg), equalTo(true));
      assertThat("Expect usage to be printed: " + bos.toString(), bos.toString().contains("usage: " + type + " filter options"), equalTo(true));
    } finally {
      System.setOut(originalOutStream);
      System.setErr(originalErrOutStream);
    }
  }

  @Test
  public void invalid_query_filter_arg_prints_help() throws Exception {
    String[] args = {
            "query",
            "-start_time", "500",
            "-end_time", "1000",
            "-num_reducers", "10",
            "-base_path", "/base/path",
            "-base_output_path", "/base/output/path",
            "-ip_src_addr", "THIS IS AN ERROR"
    };
    assertCliError(args, "Query", "");
  }

  @Test
  public void missing_start_time_arg_prints_error_and_help() throws Exception {
    String[] args = {
            "fixed",
            "-ip_src_addr", "192.168.1.1",
            "-ip_dst_addr", "192.168.1.2",
            "-ip_src_port", "8081",
            "-ip_dst_port", "8082",
            "-protocol", "6",
            "-num_reducers", "10"
    };
    assertCliError(args, "Fixed", "Missing required option: st");
  }

}
