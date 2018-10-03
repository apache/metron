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

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoField.*;

public class SyslogUtils {

    public static long parseTimestampToEpochMillis(String logTimestamp, Clock deviceClock) throws ParseException {
        ZoneId deviceTimeZone = deviceClock.getZone();

        // RFC3164 (standard syslog timestamp; no year)
        // MMM ppd HH:mm:ss
        // Oct  9 2015 13:42:11
        if (Pattern.matches("[A-Z][a-z]{2}(?:(?:\\s{2}\\d)|(?:\\s\\d{2}))\\s\\d{2}:\\d{2}:\\d{2}", logTimestamp)) {
            DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("MMM ppd HH:mm:ss").withZone(deviceTimeZone);

            TemporalAccessor inputDate = inputFormat.parse(logTimestamp);
            int inputMonth = inputDate.get(MONTH_OF_YEAR);
            int inputDay = inputDate.get(DAY_OF_MONTH);
            int inputHour = inputDate.get(HOUR_OF_DAY);
            int inputMinute = inputDate.get(MINUTE_OF_HOUR);
            int inputSecond = inputDate.get(SECOND_OF_MINUTE);

            ZonedDateTime currentDate = ZonedDateTime.now(deviceClock);
            int currentYear = currentDate.getYear();

            ZonedDateTime inputDateWithCurrentYear =
                    ZonedDateTime.of(currentYear, inputMonth, inputDay, inputHour, inputMinute, inputSecond, 0, deviceTimeZone);

            // Since no year is provided, one must be derived. Assume that any date more than 4 days in the future is in the past.
            if(inputDateWithCurrentYear.isAfter(currentDate.plusDays(4L))) {
                ZonedDateTime inputDateWithPreviousYear =
                        ZonedDateTime.of(currentYear-1, inputMonth, inputDay, inputHour, inputMinute, inputSecond, 0, deviceTimeZone);
                return inputDateWithPreviousYear.toInstant().toEpochMilli();
            }
            else
                return inputDateWithCurrentYear.toInstant().toEpochMilli();
        }

        // CISCO timestamp (standard syslog + year)
        // MMM dd yyyy HH:mm:ss
        // Oct 09 2015 13:42:11
        else if (Pattern.matches("[A-Z][a-z]{2}\\s\\d{2}\\s\\d{4}\\s\\d{2}:\\d{2}:\\d{2}", logTimestamp))
            return convertToEpochMillis(logTimestamp, DateTimeFormatter.ofPattern("MMM dd yyyy HH:mm:ss").withZone(deviceTimeZone));

        // RFC5424 (ISO timestamp)
        // 2015-10-09T13:42:11.52Z or 2015-10-09T13:42:11.52-04:00
        else if (Pattern.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})", logTimestamp))
            return convertToEpochMillis(logTimestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        else
            throw new ParseException(String.format("Unsupported date format: '%s'", logTimestamp));
    }

    private static long convertToEpochMillis(String logTimestamp, DateTimeFormatter logTimeFormat) {
        ZonedDateTime timestamp = ZonedDateTime.parse(logTimestamp, logTimeFormat);
        return timestamp.toInstant().toEpochMilli();
    }

    public static String getSeverityFromPriority(int priority) {
        int severity = priority & 0x07;
        switch (severity) {
            case 0: return "emerg";
            case 1: return "alert";
            case 2: return "crit";
            case 3: return "err";
            case 4: return "warn";
            case 5: return "notice";
            case 6: return "info";
            case 7: return "debug";
            default: return "unknown";
        }
    }

    public static String getFacilityFromPriority(int priority) {
        int facility = priority >> 3;
        switch (facility) {
            case 0: return "kern";
            case 1: return "user";
            case 2: return "mail";
            case 3: return "daemon";
            case 4: return "auth";
            case 5: return "syslog";
            case 6: return "lpr";
            case 7: return "news";
            case 8: return "uucp";
            //case 9
            case 10: return "authpriv";
            case 11: return "ftp";
            //case 12
            //case 13
            //case 14
            case 15: return "cron";
            case 16: return "local0";
            case 17: return "local1";
            case 18: return "local2";
            case 19: return "local3";
            case 20: return "local4";
            case 21: return "local5";
            case 22: return "local6";
            case 23: return "local7";
            default: return "unknown";
        }
    }
}
