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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SyslogUtils {

    public static long convertToEpochMillis(String logTimestamp, String logTimeFormat) {
        ZonedDateTime timestamp = ZonedDateTime.parse(logTimestamp, DateTimeFormatter.ofPattern(logTimeFormat).withZone(ZoneOffset.UTC));
        return timestamp.toEpochSecond() * 1000;
    }

    public static long parseTimestampToEpochMillis(String logTimestamp) {
        if (logTimestamp.length() < 20) {
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            int year = now.getYear();
            if (now.getDayOfYear() == 1 && !now.getMonth().toString().substring(0,3).equals(logTimestamp.substring(0,3).toUpperCase()))
                year--;
            logTimestamp = logTimestamp + " " + year;
            return convertToEpochMillis(logTimestamp, "MMM ppd HH:mm:ss yyyy");
        }
        else
            return convertToEpochMillis(logTimestamp, "MMM dd yyyy HH:mm:ss");
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
