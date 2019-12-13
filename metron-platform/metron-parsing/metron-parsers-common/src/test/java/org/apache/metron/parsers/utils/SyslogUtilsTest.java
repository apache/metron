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
package org.apache.metron.parsers.utils;

import org.apache.metron.parsers.ParseException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class SyslogUtilsTest {

    @Test
    public void testRfc3164Timestamp() throws ParseException {
        String originalTimestamp = "Oct  9 13:42:11";

        // Fixed clock is behind parsed timestamp, but by less than 4 days. Expect year of clock.
        ZonedDateTime fixedInstant =
                ZonedDateTime.of(2016, 10, 8, 18, 30, 30, 0, ZoneOffset.UTC);
        Clock fixedClock = Clock.fixed(fixedInstant.toInstant(), fixedInstant.getZone());

        assertEquals(SyslogUtils.parseTimestampToEpochMillis(originalTimestamp, fixedClock), 1476020531000L);
    }

    @Test
    public void testRfc3164TimestampBackDate() throws ParseException {
        String originalTimestamp = "Oct  9 13:42:11";

        // Fixed clock is behind parsed timestamp by more than 4 days. Expect year of clock - 1.
        ZonedDateTime fixedInstant =
                ZonedDateTime.of(2016, 10, 1, 18, 30, 30, 0, ZoneOffset.UTC);
        Clock fixedClock = Clock.fixed(fixedInstant.toInstant(), fixedInstant.getZone());

        assertEquals(SyslogUtils.parseTimestampToEpochMillis(originalTimestamp, fixedClock), 1444398131000L);
    }

    @Test
    public void testCiscoTimestamp() throws ParseException {
        String originalTimestamp = "Oct 09 2015 13:42:11";
        assertEquals(getParsedEpochMillis(originalTimestamp), 1444398131000L);
    }

    @Test
    public void testRfc5424TimestampUTC() throws ParseException {
        String originalTimestamp = "2015-10-09T13:42:11.52Z";
        assertEquals(getParsedEpochMillis(originalTimestamp), 1444398131520L);
    }

    @Test
    public void testRfc5424TimestampWithOffset() throws ParseException {
        String originalTimestamp = "2015-10-09T08:42:11.52-05:00";
        assertEquals(getParsedEpochMillis(originalTimestamp), 1444398131520L);
    }

    private long getParsedEpochMillis(String originalTimestamp) throws ParseException {
        return SyslogUtils.parseTimestampToEpochMillis(originalTimestamp, Clock.systemUTC());
    }
}
