package org.apache.metron.parsers.airmagnet;

import org.apache.metron.parsers.GrokParser;

public class GrokAirMagnetParser extends GrokParser {

	private static final long serialVersionUID = 1161648624118924155L;

	public GrokAirMagnetParser(String grokHdfsPath, String patternLabel) {
		super(grokHdfsPath, patternLabel);
	}

}
