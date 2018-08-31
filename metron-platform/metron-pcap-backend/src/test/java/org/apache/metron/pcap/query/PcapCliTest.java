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

import static org.apache.metron.pcap.config.PcapGlobalDefaults.BASE_INPUT_PATH_DEFAULT;
import static org.apache.metron.pcap.config.PcapGlobalDefaults.BASE_INTERIM_RESULT_PATH_DEFAULT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.MRJobConfig;
import org.apache.metron.common.Constants;
import org.apache.metron.common.system.Clock;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.job.Finalizer;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.config.FixedPcapConfig;
import org.apache.metron.pcap.config.PcapConfig.PrefixStrategy;
import org.apache.metron.pcap.config.PcapOptions;
import org.apache.metron.pcap.mr.PcapJob;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PcapCliTest {

  @Mock
  private PcapJob jobRunner;
  @Mock
  private Clock clock;
  private String execDir;
  private PrefixStrategy prefixStrategy;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    execDir = System.getProperty("user.dir");
    prefixStrategy = clock -> "random_prefix";
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

    HashMap<String, String> query = new HashMap<String, String>() {{
      put(Constants.Fields.SRC_ADDR.getName(), "192.168.1.1");
      put(Constants.Fields.DST_ADDR.getName(), "192.168.1.2");
      put(Constants.Fields.SRC_PORT.getName(), "8081");
      put(Constants.Fields.DST_PORT.getName(), "8082");
      put(Constants.Fields.PROTOCOL.getName(), "6");
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), "false");
      put(PcapHelper.PacketFields.PACKET_FILTER.getName(), "`casey`");
    }};
    FixedPcapConfig config = new FixedPcapConfig(prefixStrategy);
    PcapOptions.BASE_PATH.put(config, BASE_INPUT_PATH_DEFAULT);
    PcapOptions.BASE_INTERIM_RESULT_PATH.put(config, BASE_INTERIM_RESULT_PATH_DEFAULT);
    PcapOptions.FIELDS.put(config, query);
    PcapOptions.NUM_REDUCERS.put(config, 10);
    PcapOptions.START_TIME_MS.put(config, 500L);

    when(jobRunner.submit(isA(Finalizer.class), argThat(mapContaining(config)))).thenReturn(jobRunner);

    PcapCli cli = new PcapCli(jobRunner, prefixStrategy);
    assertThat("Expect no errors on run", cli.run(args), equalTo(0));
    verify(jobRunner).get();
  }

  /**
   * Check that "map" entries exist in the tested map "item". Note, will not work for complex
   * Objects where equals() does not compare contents favorably. e.g. Configurator() did not work.
   */
  private <K, V> Matcher<Map<K, V>> mapContaining(Map<K, V> map) {
    return new TypeSafeMatcher<Map<K, V>>() {
      @Override
      protected boolean matchesSafely(Map<K, V> item) {
        for(K key: map.keySet()) {
          if (key.equals(PcapOptions.HADOOP_CONF.getKey())) {
            Configuration itemConfiguration = (Configuration) item.get(PcapOptions.HADOOP_CONF.getKey());
            Map<String, Object> mapConfiguration = (Map<String, Object>) map.get(PcapOptions.HADOOP_CONF.getKey());
            for(String setting: mapConfiguration.keySet()) {
              if (!mapConfiguration.get(setting).equals(itemConfiguration.get(setting, ""))) {
                return false;
              }
            }
          } else {
            V itemValue = item.get(key);
            V mapValue = map.get(key);
            if (itemValue != null ? !itemValue.equals(mapValue) : mapValue != null) {
              return false;
            }
          }
        }
        return true;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Should contain items: ");
        for (Entry<K, V> entry : map.entrySet()) {
          StringBuilder sb = new StringBuilder();
          sb.append("key=");
          sb.append(entry.getKey());
          sb.append(",value=");
          sb.append(entry.getValue());
          description.appendText(sb.toString());
        }
      }
    };
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
            "-records_per_file", "1000",
            "-finalizer_threads", "10"
    };
    Map<String, String> query = new HashMap<String, String>() {{
      put(Constants.Fields.SRC_ADDR.getName(), "192.168.1.1");
      put(Constants.Fields.DST_ADDR.getName(), "192.168.1.2");
      put(Constants.Fields.SRC_PORT.getName(), "8081");
      put(Constants.Fields.DST_PORT.getName(), "8082");
      put(Constants.Fields.PROTOCOL.getName(), "6");
      put(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), "true");
    }};
    FixedPcapConfig config = new FixedPcapConfig(prefixStrategy);
    PcapOptions.BASE_PATH.put(config, "/base/path");
    PcapOptions.BASE_INTERIM_RESULT_PATH.put(config, "/base/output/path");
    PcapOptions.FIELDS.put(config, query);
    PcapOptions.NUM_REDUCERS.put(config, 10);
    PcapOptions.START_TIME_MS.put(config, 500L);
    PcapOptions.END_TIME_MS.put(config, 1000L);
    PcapOptions.NUM_RECORDS_PER_FILE.put(config, 1000);
    PcapOptions.PRINT_JOB_STATUS.put(config, true);
    PcapOptions.FINALIZER_THREADPOOL_SIZE.put(config, "10");

    when(jobRunner.submit(isA(Finalizer.class), argThat(mapContaining(config)))).thenReturn(jobRunner);

    PcapCli cli = new PcapCli(jobRunner, prefixStrategy);
    assertThat("Expect no errors on run", cli.run(args), equalTo(0));
    verify(jobRunner).get();
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
            "-records_per_file", "1000",
            "-yq", "pcap",
            "-finalizer_threads", "10"
    };
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

    FixedPcapConfig config = new FixedPcapConfig(prefixStrategy);
    PcapOptions.BASE_PATH.put(config, "/base/path");
    PcapOptions.BASE_INTERIM_RESULT_PATH.put(config, "/base/output/path");
    PcapOptions.FIELDS.put(config, query);
    PcapOptions.NUM_REDUCERS.put(config, 10);
    PcapOptions.START_TIME_MS.put(config, startAsNanos / 1000000L); // needed bc defaults in config
    PcapOptions.END_TIME_MS.put(config, endAsNanos / 1000000L);  // needed bc defaults in config
    PcapOptions.NUM_RECORDS_PER_FILE.put(config, 1000);
    PcapOptions.PRINT_JOB_STATUS.put(config, true);
    PcapOptions.HADOOP_CONF.put(config, new HashMap<String, Object>() {{
      put(MRJobConfig.QUEUE_NAME, "pcap");
    }});
    PcapOptions.FINALIZER_THREADPOOL_SIZE.put(config, "10");

    when(jobRunner.submit(isA(Finalizer.class), argThat(mapContaining(config)))).thenReturn(jobRunner);

    PcapCli cli = new PcapCli(jobRunner, prefixStrategy);
    assertThat("Expect no errors on run", cli.run(args), equalTo(0));
    verify(jobRunner).get();
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

    String query = "some query string";
    FixedPcapConfig config = new FixedPcapConfig(prefixStrategy);
    PcapOptions.BASE_PATH.put(config, BASE_INPUT_PATH_DEFAULT);
    PcapOptions.BASE_INTERIM_RESULT_PATH.put(config, BASE_INTERIM_RESULT_PATH_DEFAULT);
    PcapOptions.FIELDS.put(config, query);
    PcapOptions.NUM_REDUCERS.put(config, 10);
    PcapOptions.START_TIME_MS.put(config, 500L);
    PcapOptions.FINALIZER_THREADPOOL_SIZE.put(config, "1");

    when(jobRunner.submit(isA(Finalizer.class), argThat(mapContaining(config)))).thenReturn(jobRunner);

    PcapCli cli = new PcapCli(jobRunner, prefixStrategy);
    assertThat("Expect no errors on run", cli.run(args), equalTo(0));
    verify(jobRunner).get();
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
            "-records_per_file", "1000",
            "-finalizer_threads", "10"
    };

    String query = "some query string";
    FixedPcapConfig config = new FixedPcapConfig(prefixStrategy);
    PcapOptions.BASE_PATH.put(config, "/base/path");
    PcapOptions.BASE_INTERIM_RESULT_PATH.put(config, "/base/output/path");
    PcapOptions.FIELDS.put(config, query);
    PcapOptions.NUM_REDUCERS.put(config, 10);
    PcapOptions.START_TIME_MS.put(config, 500L); // needed bc defaults in config
    PcapOptions.END_TIME_MS.put(config, 1000L);  // needed bc defaults in config
    PcapOptions.NUM_RECORDS_PER_FILE.put(config, 1000);
    PcapOptions.PRINT_JOB_STATUS.put(config, true);
    PcapOptions.FINALIZER_THREADPOOL_SIZE.put(config, "10");

    when(jobRunner.submit(isA(Finalizer.class), argThat(mapContaining(config)))).thenReturn(jobRunner);

    PcapCli cli = new PcapCli(jobRunner, prefixStrategy);
    assertThat("Expect no errors on run", cli.run(args), equalTo(0));
    verify(jobRunner).get();
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

      PcapCli cli = new PcapCli(jobRunner, clock -> "random_prefix");
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
