package com.apache.metron.parsing.parsers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import oi.thekraken.grok.api.exception.GrokException;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;

public class GrokAsaParser extends AbstractParser implements Serializable {

	private static final long serialVersionUID = 945353287115350798L;
	private transient  Grok  grok;
	Map<String, String> patternMap;
	private transient  Map<String, Grok> grokMap;
	private transient  InputStream pattern_url;

	public static final String PREFIX = "stream2file";
	public static final String SUFFIX = ".tmp";

	public static File stream2file(InputStream in) throws IOException {
		final File tempFile = File.createTempFile(PREFIX, SUFFIX);
		tempFile.deleteOnExit();
		try (FileOutputStream out = new FileOutputStream(tempFile)) {
			IOUtils.copy(in, out);
		}
		return tempFile;
	}

	public GrokAsaParser() throws Exception {
		// pattern_url = Resources.getResource("patterns/asa");

		pattern_url = getClass().getClassLoader().getResourceAsStream(
				"patterns/asa");

		File file = stream2file(pattern_url);
		grok = Grok.create(file.getPath());

		patternMap = getPatternMap();
		grokMap = getGrokMap();

		grok.compile("%{CISCO_TAGGED_SYSLOG}");
	}

	public GrokAsaParser(String filepath) throws Exception {

		grok = Grok.create(filepath);
		// grok.getNamedRegexCollection().put("ciscotag","CISCOFW302013_302014_302015_302016");
		grok.compile("%{CISCO_TAGGED_SYSLOG}");

	}

	public GrokAsaParser(String filepath, String pattern) throws Exception {

		grok = Grok.create(filepath);
		grok.compile("%{" + pattern + "}");
	}

	private Map<String, Object> getMap(String pattern, String text)
			throws GrokException {

		Grok g = grokMap.get(pattern);
		if (g != null) {
			Match gm = g.match(text);
			gm.captures();
			return gm.toMap();
		} else {
			return new HashMap<String, Object>();
		}

	}

	private Map<String, Grok> getGrokMap() throws GrokException, IOException {
		Map<String, Grok> map = new HashMap<String, Grok>();

		for (Map.Entry<String, String> entry : patternMap.entrySet()) {
			File file = stream2file(pattern_url);
			Grok grok = Grok.create(file.getPath());
			grok.compile("%{" + entry.getValue() + "}");

			map.put(entry.getValue(), grok);

		}

		return map;
	}

	private Map<String, String> getPatternMap() {
		Map<String, String> map = new HashMap<String, String>();

		map.put("ASA-2-106001", "CISCOFW106001");
		map.put("ASA-2-106006", "CISCOFW106006_106007_106010");
		map.put("ASA-2-106007", "CISCOFW106006_106007_106010");
		map.put("ASA-2-106010", "CISCOFW106006_106007_106010");
		map.put("ASA-3-106014", "CISCOFW106014");
		map.put("ASA-6-106015", "CISCOFW106015");
		map.put("ASA-1-106021", "CISCOFW106021");
		map.put("ASA-4-106023", "CISCOFW106023");
		map.put("ASA-5-106100", "CISCOFW106100");
		map.put("ASA-6-110002", "CISCOFW110002");
		map.put("ASA-6-302010", "CISCOFW302010");
		map.put("ASA-6-302013", "CISCOFW302013_302014_302015_302016");
		map.put("ASA-6-302014", "CISCOFW302013_302014_302015_302016");
		map.put("ASA-6-302015", "CISCOFW302013_302014_302015_302016");
		map.put("ASA-6-302016", "CISCOFW302013_302014_302015_302016");
		map.put("ASA-6-302020", "CISCOFW302020_302021");
		map.put("ASA-6-302021", "CISCOFW302020_302021");
		map.put("ASA-6-305011", "CISCOFW305011");
		map.put("ASA-3-313001", "CISCOFW313001_313004_313008");
		map.put("ASA-3-313004", "CISCOFW313001_313004_313008");
		map.put("ASA-3-313008", "CISCOFW313001_313004_313008");
		map.put("ASA-4-313005", "CISCOFW313005");
		map.put("ASA-4-402117", "CISCOFW402117");
		map.put("ASA-4-402119", "CISCOFW402119");
		map.put("ASA-4-419001", "CISCOFW419001");
		map.put("ASA-4-419002", "CISCOFW419002");
		map.put("ASA-4-500004", "CISCOFW500004");
		map.put("ASA-6-602303", "CISCOFW602303_602304");
		map.put("ASA-6-602304", "CISCOFW602303_602304");
		map.put("ASA-7-710001", "CISCOFW710001_710002_710003_710005_710006");
		map.put("ASA-7-710002", "CISCOFW710001_710002_710003_710005_710006");
		map.put("ASA-7-710003", "CISCOFW710001_710002_710003_710005_710006");
		map.put("ASA-7-710005", "CISCOFW710001_710002_710003_710005_710006");
		map.put("ASA-7-710006", "CISCOFW710001_710002_710003_710005_710006");
		map.put("ASA-6-713172", "CISCOFW713172");
		map.put("ASA-4-733100", "CISCOFW733100");
		map.put("ASA-6-305012", "CISCOFW305012");
		map.put("ASA-7-609001", "CISCOFW609001");
		map.put("ASA-7-609002", "CISCOFW609002");

		return map;
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
	
	@Override
	public void init() {
		// pattern_url = Resources.getResource("patterns/asa");

				pattern_url = getClass().getClassLoader().getResourceAsStream(
						"patterns/asa");

				File file = null;
				try {
					file = stream2file(pattern_url);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					grok = Grok.create(file.getPath());
				} catch (GrokException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				patternMap = getPatternMap();
				try {
					grokMap = getGrokMap();
				} catch (GrokException | IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				try {
					grok.compile("%{CISCO_TAGGED_SYSLOG}");
				} catch (GrokException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	}

	@Override
	public JSONObject parse(byte[] raw_message) {

		String toParse = "";
		JSONObject toReturn;

		try {

			toParse = new String(raw_message, "UTF-8");

			System.out.println("Received message: " + toParse);

			Match gm = grok.match(toParse);
			gm.captures();

			toReturn = new JSONObject();

			toReturn.putAll(gm.toMap());

			String str = toReturn.get("ciscotag").toString();
			String pattern = patternMap.get(str);

			Map<String, Object> response = getMap(pattern, toParse);

			toReturn.putAll(response);

			//System.out.println("*******I MAPPED: " + toReturn);

			toReturn.put("timestamp", convertToEpoch(toReturn.get("MONTH").toString(), toReturn
					.get("MONTHDAY").toString(), 
					toReturn.get("TIME").toString(),
					true));
			
			toReturn.remove("MONTHDAY");
			toReturn.remove("TIME");
			toReturn.remove("MINUTE");
			toReturn.remove("HOUR");
			toReturn.remove("YEAR");
			toReturn.remove("SECOND");
			
			toReturn.put("ip_src_addr", toReturn.remove("IPORHOST"));
			toReturn.put("original_string", toParse);

			return toReturn;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	
}