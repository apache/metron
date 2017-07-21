/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.pcap;

import static java.lang.Long.toUnsignedString;
import static org.hamcrest.CoreMatchers.equalTo;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.pcap.mr.PcapJob;
import org.junit.Assert;
import org.junit.Test;

public class PcapJobTest {

  @Test
  public void partition_gives_value_in_range() throws Exception {
    long start = 1473897600000000000L;
    long end = TimestampConverters.MILLISECONDS.toNanoseconds(1473995927455L);
    Configuration conf = new Configuration();
    conf.set(PcapJob.START_TS_CONF, toUnsignedString(start));
    conf.set(PcapJob.END_TS_CONF, toUnsignedString(end));
    conf.set(PcapJob.WIDTH_CONF, "" + PcapJob.findWidth(start, end, 10));
    PcapJob.PcapPartitioner partitioner = new PcapJob.PcapPartitioner();
    partitioner.setConf(conf);
    Assert.assertThat("Partition not in range",
        partitioner.getPartition(new LongWritable(1473978789181189000L), new BytesWritable(), 10),
        equalTo(8));
  }

}
