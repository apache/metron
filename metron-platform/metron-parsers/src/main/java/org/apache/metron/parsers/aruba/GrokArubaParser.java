package org.apache.metron.parsers.aruba;

import org.apache.metron.parsers.GrokParser;

public class GrokArubaParser extends GrokParser {

	private static final long serialVersionUID = 3975493065728576059L;

	public GrokArubaParser(String grokHdfsPath, String patternLabel) {
		super(grokHdfsPath, patternLabel);
	}

}