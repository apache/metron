package org.apache.metron.parsers.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Various utilities for parsing and extracting dates
 * 
 * @author sball
 *
 */
public class DateUtils {

	public static List<SimpleDateFormat> DATE_FORMATS_CEF = new ArrayList<SimpleDateFormat>() {
		{
			add(new SimpleDateFormat("MMM dd HH:mm:ss.SSS zzz"));
			/*
			add(new SimpleDateFormat("MMM dd HH:mm:ss.SSS"));
			add(new SimpleDateFormat("MMM dd HH:mm:ss zzz"));
			add(new SimpleDateFormat("MMM dd HH:mm:ss"));
			add(new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS zzz"));
			add(new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS"));
			add(new SimpleDateFormat("MMM dd yyyy HH:mm:ss zzz"));
			add(new SimpleDateFormat("MMM dd yyyy HH:mm:ss"));
			*/
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

	public static Date parseMultiformat(String candidate, List<SimpleDateFormat> validPatterns) throws ParseException {

		for (SimpleDateFormat pattern : validPatterns) {
			try {
				return pattern.parse(candidate);
			} catch (ParseException e) {
				continue;
			}

		}
		throw new ParseException("Failed to parse any of the given date formats", 0);
	}
}
