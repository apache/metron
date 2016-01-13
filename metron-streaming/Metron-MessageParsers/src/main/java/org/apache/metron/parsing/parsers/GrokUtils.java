package com.apache.metron.parsing.parsers;
import java.io.Serializable;

import com.google.code.regexp.Pattern;

public class GrokUtils implements Serializable {

	private static final long serialVersionUID = 7465176887422419286L;
	/**
	   * Extract Grok patter like %{FOO} to FOO, Also Grok pattern with semantic.
	   */
	  public static final Pattern GROK_PATTERN = Pattern.compile(
	      "%\\{" +
	      "(?<name>" +
	        "(?<pattern>[A-z0-9]+)" +
	          "(?::(?<subname>[A-z0-9_:;\\/\\s\\.]+))?" +
	          ")" +
	          "(?:=(?<definition>" +
	            "(?:" +
	            "(?:[^{}]+|\\.+)+" +
	            ")+" +
	            ")" +
	      ")?" +
	      "\\}");

	}