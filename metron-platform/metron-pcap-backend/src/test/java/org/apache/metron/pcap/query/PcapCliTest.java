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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.Constants;
import org.apache.metron.common.system.Clock;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;
import org.apache.metron.pcap.filter.query.QueryPcapFilter;
import org.apache.metron.pcap.mr.PcapJob;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

public class PcapCliTest {

  @Mock
  private PcapJob jobRunner;
  @Mock
  private ResultsWriter resultsWriter;
  @Mock
  private Clock clock;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
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
            "-protocol", "6"
    };
    List<byte[]> pcaps = Arrays.asList(new byte[][]{asBytes("abc"), asBytes("def"), asBytes("ghi")});

    Path base_path = new Path(CliConfig.BASE_PATH_DEFAULT);
    Path base_output_path = new Path(CliConfig.BASE_OUTPUT_PATH_DEFAULT);
    EnumMap<Constants.Fields, String> query = new EnumMap<Constants.Fields, String>(Constants.Fields.class) {{
      put(Constants.Fields.SRC_ADDR, "192.168.1.1");
      put(Constants.Fields.DST_ADDR, "192.168.1.2");
      put(Constants.Fields.SRC_PORT, "8081");
      put(Constants.Fields.DST_PORT, "8082");
      put(Constants.Fields.PROTOCOL, "6");
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC, "false");
    }};

    when(jobRunner.query(eq(base_path), eq(base_output_path), anyLong(), anyLong(), eq(query), isA(Configuration.class), isA(FileSystem.class), isA(FixedPcapFilter.Configurator.class))).thenReturn(pcaps);
    when(clock.currentTimeFormatted("yyyyMMddHHmmssSSSZ")).thenReturn("20160615183527162+0000");

    PcapCli cli = new PcapCli(jobRunner, resultsWriter, clock);
    assertThat("Expect no errors on run", cli.run(args), equalTo(0));
    Mockito.verify(resultsWriter).write(pcaps, "pcap-data-20160615183527162+0000.pcap");
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
            "-include_reverse"
    };
    List<byte[]> pcaps = Arrays.asList(new byte[][]{asBytes("abc"), asBytes("def"), asBytes("ghi")});

    Path base_path = new Path("/base/path");
    Path base_output_path = new Path("/base/output/path");
    EnumMap<Constants.Fields, String> query = new EnumMap<Constants.Fields, String>(Constants.Fields.class) {{
      put(Constants.Fields.SRC_ADDR, "192.168.1.1");
      put(Constants.Fields.DST_ADDR, "192.168.1.2");
      put(Constants.Fields.SRC_PORT, "8081");
      put(Constants.Fields.DST_PORT, "8082");
      put(Constants.Fields.PROTOCOL, "6");
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC, "true");
    }};

    when(jobRunner.query(eq(base_path), eq(base_output_path), anyLong(), anyLong(), eq(query), isA(Configuration.class), isA(FileSystem.class), isA(FixedPcapFilter.Configurator.class))).thenReturn(pcaps);
    when(clock.currentTimeFormatted("yyyyMMddHHmmssSSSZ")).thenReturn("20160615183527162+0000");

    PcapCli cli = new PcapCli(jobRunner, resultsWriter, clock);
    assertThat("Expect no errors on run", cli.run(args), equalTo(0));
    Mockito.verify(resultsWriter).write(pcaps, "pcap-data-20160615183527162+0000.pcap");
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
            "-include_reverse"
    };
    List<byte[]> pcaps = Arrays.asList(new byte[][]{asBytes("abc"), asBytes("def"), asBytes("ghi")});

    Path base_path = new Path("/base/path");
    Path base_output_path = new Path("/base/output/path");
    EnumMap<Constants.Fields, String> query = new EnumMap<Constants.Fields, String>(Constants.Fields.class) {{
      put(Constants.Fields.SRC_ADDR, "192.168.1.1");
      put(Constants.Fields.DST_ADDR, "192.168.1.2");
      put(Constants.Fields.SRC_PORT, "8081");
      put(Constants.Fields.DST_PORT, "8082");
      put(Constants.Fields.PROTOCOL, "6");
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC, "true");
    }};

    long startAsNanos = asNanos("2016-06-13-18:35.00", "yyyy-MM-dd-HH:mm.ss");
    long endAsNanos = asNanos("2016-06-15-18:35.00", "yyyy-MM-dd-HH:mm.ss");
    when(jobRunner.query(eq(base_path), eq(base_output_path), eq(startAsNanos), eq(endAsNanos), eq(query), isA(Configuration.class), isA(FileSystem.class), isA(FixedPcapFilter.Configurator.class))).thenReturn(pcaps);
    when(clock.currentTimeFormatted("yyyyMMddHHmmssSSSZ")).thenReturn("20160615183527162+0000");

    PcapCli cli = new PcapCli(jobRunner, resultsWriter, clock);
    assertThat("Expect no errors on run", cli.run(args), equalTo(0));
    Mockito.verify(resultsWriter).write(pcaps, "pcap-data-20160615183527162+0000.pcap");
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

    Path base_path = new Path(CliConfig.BASE_PATH_DEFAULT);
    Path base_output_path = new Path(CliConfig.BASE_OUTPUT_PATH_DEFAULT);
    String query = "some query string";

    when(jobRunner.query(eq(base_path), eq(base_output_path), anyLong(), anyLong(), eq(query), isA(Configuration.class), isA(FileSystem.class), isA(QueryPcapFilter.Configurator.class))).thenReturn(pcaps);
    when(clock.currentTimeFormatted("yyyyMMddHHmmssSSSZ")).thenReturn("20160615183527162+0000");

    PcapCli cli = new PcapCli(jobRunner, resultsWriter, clock);
    assertThat("Expect no errors on run", cli.run(args), equalTo(0));
    Mockito.verify(resultsWriter).write(pcaps, "pcap-data-20160615183527162+0000.pcap");
  }

  @Test
  public void runs_query_pcap_filter_job_with_full_argument_list() throws Exception {
    String[] args = {
            "query",
            "-start_time", "500",
            "-end_time", "1000",
            "-base_path", "/base/path",
            "-base_output_path", "/base/output/path",
            "-query", "some query string"
    };
    List<byte[]> pcaps = Arrays.asList(new byte[][]{asBytes("abc"), asBytes("def"), asBytes("ghi")});

    Path base_path = new Path("/base/path");
    Path base_output_path = new Path("/base/output/path");
    String query = "some query string";

    when(jobRunner.query(eq(base_path), eq(base_output_path), anyLong(), anyLong(), eq(query), isA(Configuration.class), isA(FileSystem.class), isA(QueryPcapFilter.Configurator.class))).thenReturn(pcaps);
    when(clock.currentTimeFormatted("yyyyMMddHHmmssSSSZ")).thenReturn("20160615183527162+0000");

    PcapCli cli = new PcapCli(jobRunner, resultsWriter, clock);
    assertThat("Expect no errors on run", cli.run(args), equalTo(0));
    Mockito.verify(resultsWriter).write(pcaps, "pcap-data-20160615183527162+0000.pcap");
  }

  @Test
  public void invalid_fixed_filter_arg_prints_help() throws Exception {
    PrintStream originalOutStream = System.out;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      PrintStream testStream = new PrintStream(new BufferedOutputStream(bos));
      System.setOut(testStream);
      String[] args = {
              "fixed",
              "-start_time", "500",
              "-end_time", "1000",
              "-base_path", "/base/path",
              "-base_output_path", "/base/output/path",
              "-query", "THIS IS AN ERROR"
      };

      PcapCli cli = new PcapCli(jobRunner, resultsWriter, clock);
      assertThat("Expect errors on run", cli.run(args), equalTo(-1));
      assertThat(bos.toString(), bos.toString().contains("usage: Fixed filter options"), equalTo(true));
    } finally {
      System.setOut(originalOutStream);
    }
  }

  @Test
  public void invalid_query_filter_arg_prints_help() throws Exception {
    PrintStream originalOutStream = System.out;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      PrintStream outStream = new PrintStream(new BufferedOutputStream(bos));
      System.setOut(outStream);
      String[] args = {
              "query",
              "-start_time", "500",
              "-end_time", "1000",
              "-base_path", "/base/path",
              "-base_output_path", "/base/output/path",
              "-ip_src_addr", "THIS IS AN ERROR"
      };

      PcapCli cli = new PcapCli(jobRunner, resultsWriter, clock);
      assertThat("Expect errors on run", cli.run(args), equalTo(-1));
      assertThat(bos.toString(), bos.toString().contains("usage: Query filter options"), equalTo(true));
    } finally {
      System.setOut(originalOutStream);
    }
  }

}
