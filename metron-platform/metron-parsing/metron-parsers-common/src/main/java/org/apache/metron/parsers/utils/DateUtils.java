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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * Various utilities for parsing and extracting dates
 * 
 */
public class DateUtils {

	// Per IBM LEEF guide at https://www.ibm.com/support/knowledgecenter/SS42VS_DSM/c_LEEF_Format_Guide_intro.html
	public static List<SimpleDateFormat> DATE_FORMATS_LEEF = new ArrayList<SimpleDateFormat>() {
		{
			add(new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS zzz"));
			add(new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS"));
			add(new SimpleDateFormat("MMM dd yyyy HH:mm:ss"));
		}
	};

	public static List<SimpleDateFormat> DATE_FORMATS_CEF = new ArrayList<SimpleDateFormat>() {
		{
			// as per CEF Spec
			add(new SimpleDateFormat("MMM dd HH:mm:ss.SSS zzz"));
			add(new SimpleDateFormat("MMM dd HH:mm:ss.SSS"));
			add(new SimpleDateFormat("MMM dd HH:mm:ss zzz"));
			add(new SimpleDateFormat("MMM dd HH:mm:ss"));
			add(new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS zzz"));
			add(new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS"));
			add(new SimpleDateFormat("MMM dd yyyy HH:mm:ss zzz"));
			add(new SimpleDateFormat("MMM dd yyyy HH:mm:ss"));
			// found in the wild
			add(new SimpleDateFormat("dd MMMM yyyy HH:mm:ss"));
		}
	};

	public static List<SimpleDateFormat> DATE_FORMATS_SYSLOG = new ArrayList<SimpleDateFormat>() {
		{
			// As specified in https://tools.ietf.org/html/rfc5424
			add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

			// common format per rsyslog defaults e.g. Mar 21 14:05:02
			add(new SimpleDateFormat("MMM dd HH:mm:ss"));
			add(new SimpleDateFormat("MMM dd yyyy HH:mm:ss"));

			// additional formats found in the wild
			add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
			add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"));
			add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"));

		}
	};

	Pattern NUMERIC = Pattern.compile("\\b\\d+\\b");

	/**
	 * Parse the data according to a sequence of possible parse patterns.
	 * 
	 * If the given date is entirely numeric, it is assumed to be a unix
	 * timestamp.
	 * 
	 * If the year is not specified in the date string, use the current year.
	 * Assume that any date more than 4 days in the future is in the past as per
	 * SyslogUtils
	 * 
	 * @param candidate
	 *            The possible date.
	 * @param validPatterns
	 *            A list of SimpleDateFormat instances to try parsing with.
	 * @return A java.util.Date based on the parse result
	 * @throws ParseException
	 */
	public static long parseMultiformat(String candidate, List<SimpleDateFormat> validPatterns) throws ParseException {
		if (StringUtils.isNumeric(candidate)) {
			return Long.valueOf(candidate);
		} else {
			for (SimpleDateFormat pattern : validPatterns) {
				try {
					Calendar cal = Calendar.getInstance();
					cal.setTime(pattern.parse(candidate));
					Calendar current = Calendar.getInstance();
					if (cal.get(Calendar.YEAR) == 1970) {
						cal.set(Calendar.YEAR, current.get(Calendar.YEAR));
					}
					current.add(Calendar.DAY_OF_MONTH, 4);
					if (cal.after(current)) {
						cal.add(Calendar.YEAR, -1);
					}
					return cal.getTimeInMillis();
				} catch (ParseException e) {
					continue;
				}
			}
			throw new ParseException("Failed to parse any of the given date formats", 0);
		}
	}
}
