package org.apache.metron.parsing.parsers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import oi.thekraken.grok.api.exception.GrokException;

public class BasicFireEyeParser extends AbstractParser implements Serializable {

	private static final long serialVersionUID = 6328907550159134550L;
	//String tsRegex = "(.*)([a-z][A-Z]+)\\s+(\\d+)\\s+(\\d+\\:\\d+\\:\\d+)\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)(.*)$";
	String tsRegex ="([a-zA-Z]{3})\\s+(\\d+)\\s+(\\d+\\:\\d+\\:\\d+)\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)";
	
	
	Pattern tsPattern = Pattern.compile(tsRegex);
	// private transient static MetronGrok grok;
	// private transient static InputStream pattern_url;

	public BasicFireEyeParser() throws Exception {
		// pattern_url = getClass().getClassLoader().getResourceAsStream(
		// "patterns/fireeye");
		//
		// File file = ParserUtils.stream2file(pattern_url);
		// grok = MetronGrok.create(file.getPath());
		//
		// grok.compile("%{FIREEYE_BASE}");
	}

	@Override
	public JSONObject parse(byte[] raw_message) {
		String toParse = "";

		try {

			toParse = new String(raw_message, "UTF-8");

			// String[] mTokens = toParse.split(" ");

			String positveIntPattern = "<[1-9][0-9]*>";
			Pattern p = Pattern.compile(positveIntPattern);
			Matcher m = p.matcher(toParse);

			String delimiter = "";

			while (m.find()) {
				delimiter = m.group();

			}

			if (!StringUtils.isBlank(delimiter)) {
				String[] tokens = toParse.split(delimiter);

				if (tokens.length > 1)
					toParse = delimiter + tokens[1];

			}

			JSONObject toReturn = parseMessage(toParse);

			toReturn.put("timestamp", getTimeStamp(toParse,delimiter));

			return toReturn;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public static Long convertToEpoch(String m, String d, String ts,
			boolean adjust_timezone) throws ParseException {
		d = d.trim();

		if (d.length() <= 2)
			d = "0" + d;

		Date date = new SimpleDateFormat("MMM", Locale.ENGLISH).parse(m);
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		String month = String.valueOf(cal.get(Calendar.MONTH));
		int year = Calendar.getInstance().get(Calendar.YEAR);

		if (month.length() <= 2)
			month = "0" + month;

		String coglomerated_ts = year + "-" + month + "-" + d + " " + ts;

		System.out.println(coglomerated_ts);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		if (adjust_timezone)
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

		date = sdf.parse(coglomerated_ts);
		long timeInMillisSinceEpoch = date.getTime();

		return timeInMillisSinceEpoch;
	}

	private long getTimeStamp(String toParse,String delimiter) throws ParseException {
		
		long ts = 0;
		String month = null;
		String day = null;
		String time = null;
		Matcher tsMatcher = tsPattern.matcher(toParse);
		if (tsMatcher.find()) {
			month = tsMatcher.group(1);
			day = tsMatcher.group(2);
			time = tsMatcher.group(3);
	
				} else {
			_LOG.warn("Unable to find timestamp in message: " + toParse);
			ts = convertToEpoch(month, day, time, true);
		}

			return ts;
	
	}

	private JSONObject parseMessage(String toParse) {

		// System.out.println("Received message: " + toParse);

		// MetronMatch gm = grok.match(toParse);
		// gm.captures();

		JSONObject toReturn = new JSONObject();
		//toParse = toParse.replaceAll("  ", " ");
		String[] mTokens = toParse.split("\\s+");
	 //mTokens = toParse.split(" ");

		// toReturn.putAll(gm.toMap());

		String id = mTokens[4];

		// We are not parsing the fedata for multi part message as we cannot
		// determine how we can split the message and how many multi part
		// messages can there be.
		// The message itself will be stored in the response.

		String[] tokens = id.split("\\.");
		if (tokens.length == 2) {

			String[] array = Arrays.copyOfRange(mTokens, 1, mTokens.length - 1);
			String syslog = Joiner.on(" ").join(array);

			Multimap<String, String> multiMap = formatMain(syslog);

			for (String key : multiMap.keySet()) {

				String value = Joiner.on(",").join(multiMap.get(key));
				toReturn.put(key, value.trim());
			}

		}

		toReturn.put("original_string", toParse);

		String ip_src_addr = (String) toReturn.get("dvc");
		String ip_src_port = (String) toReturn.get("src_port");
		String ip_dst_addr = (String) toReturn.get("dst_ip");
		String ip_dst_port = (String) toReturn.get("dst_port");

		if (ip_src_addr != null)
			toReturn.put("ip_src_addr", ip_src_addr);
		if (ip_src_port != null)
			toReturn.put("ip_src_port", ip_src_port);
		if (ip_dst_addr != null)
			toReturn.put("ip_dst_addr", ip_dst_addr);
		if (ip_dst_port != null)
			toReturn.put("ip_dst_port", ip_dst_port);

		System.out.println(toReturn);

		return toReturn;
	}

	private Multimap<String, String> formatMain(String in) {
		Multimap<String, String> multiMap = ArrayListMultimap.create();
		String input = in.replaceAll("cn3", "dst_port")
				.replaceAll("cs5", "cncHost").replaceAll("proto", "protocol")
				.replaceAll("rt=", "timestamp=").replaceAll("cs1", "malware")
				.replaceAll("dst=", "dst_ip=")
				.replaceAll("shost", "src_hostname")
				.replaceAll("dmac", "dst_mac").replaceAll("smac", "src_mac")
				.replaceAll("spt", "src_port")
				.replaceAll("\\bsrc\\b", "src_ip");
		String[] tokens = input.split("\\|");

		if (tokens.length > 0) {
			String message = tokens[tokens.length - 1];

			String pattern = "([\\w\\d]+)=([^=]*)(?=\\s*\\w+=|\\s*$) ";
			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(message);

			while (m.find()) {
				String[] str = m.group().split("=");
				multiMap.put(str[0], str[1]);

			}

		}
		return multiMap;
	}

	

}