package com.apache.metron.parsing.parsers;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.code.regexp.Matcher;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MetronMatch implements Serializable {

	private static final long serialVersionUID = -1129245286587945311L;
	private String subject; // texte
	  private Map<String, Object> capture;
	  private MetronGarbage garbage;
	  private MetronGrok grok;
	  private Matcher match;
	  private int start;
	  private int end;

	  /**
	   * For thread safety
	   */
	  private static ThreadLocal<MetronMatch> matchHolder = new ThreadLocal<MetronMatch>() {
		  @Override
		  protected MetronMatch initialValue() {
			  return new MetronMatch();
		  }
	  };

	  /**
	   *Create a new {@code Match} object.
	   */
	  public MetronMatch() {
	    subject = "Nothing";
	    grok = null;
	    match = null;
	    capture = new TreeMap<String, Object>();
	    garbage = new MetronGarbage();
	    start = 0;
	    end = 0;
	  }

	  /**
	   * Create Empty grok matcher
	   */
	  public static final MetronMatch EMPTY = new MetronMatch();

	  public void setGrok(MetronGrok grok){
	    if (grok != null) {
	      this.grok = grok;
	    }
	  }

	  public Matcher getMatch() {
	    return match;
	  }

	  public void setMatch(Matcher match) {
	    this.match = match;
	  }

	  public int getStart() {
	    return start;
	  }

	  public void setStart(int start) {
	    this.start = start;
	  }

	  public int getEnd() {
	    return end;
	  }

	  public void setEnd(int end) {
	    this.end = end;
	  }

	  /**
	   * Singleton.
	   *
	   * @return instance of Match
	   */
	  public static MetronMatch getInstance() {
		 return matchHolder.get();
	  }

	  /**
	   *  Set the single line of log to parse.
	   *
	   * @param text : single line of log
	   */
	  public void setSubject(String text) {
	    if (text == null) {
	      return;
	    }
	    if (text.isEmpty()) {
	      return;
	    }
	    subject = text;
	  }

	  /**
	   * Retrurn the single line of log.
	   *
	   * @return the single line of log
	   */
	  public String getSubject() {
	    return subject;
	  }

	  /**
	   * Match to the <tt>subject</tt> the <tt>regex</tt> and save the matched element into a map.
	   *
	   */
	  public void captures() {
	    if (match == null) {
	      return;
	    }
	    capture.clear();

	    // _capture.put("LINE", this.line);
	    // _capture.put("LENGTH", this.line.length() +"");

	    Map<String, String> mappedw = this.match.namedGroups();
	    Iterator<Entry<String, String>> it = mappedw.entrySet().iterator();
	    while (it.hasNext()) {

	      @SuppressWarnings("rawtypes")
	      Map.Entry pairs = (Map.Entry) it.next();
	      String key = null;
	      Object value = null;
	      if (this.grok.getNamedRegexCollectionById(pairs.getKey().toString()) == null) {
	        key = pairs.getKey().toString();
	      } else if (!this.grok.getNamedRegexCollectionById(pairs.getKey().toString()).isEmpty()) {
	        key = this.grok.getNamedRegexCollectionById(pairs.getKey().toString());
	      }
	      if (pairs.getValue() != null) {
	        value = pairs.getValue().toString();
	        
	        KeyValue keyValue = MetronConverter.convert(key, value);
	        
	        //get validated key
	        key = keyValue.getKey();
	        
	        //resolve value
	        if (keyValue.getValue() instanceof String) {
	        	 value = cleanString((String)keyValue.getValue());
	        } else {
	        	value = keyValue.getValue();
	        }
	        
	        //set if grok failure
	        if (keyValue.hasGrokFailure()) {
	        	capture.put(key + "_grokfailure", keyValue.getGrokFailure());
	        }
	      }

	      capture.put(key, value);
	      it.remove(); // avoids a ConcurrentModificationException
	    }
	  }


	  /**
	   * remove from the string the quote and double quote.
	   *
	   * @param string to pure: "my/text"
	   * @return unquoted string: my/text
	   */
	  private String cleanString(String value) {
	    if (value == null) {
	      return value;
	    }
	    if (value.isEmpty()) {
	      return value;
	    }
	    char[] tmp = value.toCharArray();
	    if ((tmp[0] == '"' && tmp[value.length() - 1] == '"')
	        || (tmp[0] == '\'' && tmp[value.length() - 1] == '\'')) {
	      value = value.substring(1, value.length() - 1);
	    }
	    return value;
	  }


	  /**
	   * Get the json representation of the matched element.
	   * <p>
	   * example:
	   * map [ {IP: 127.0.0.1}, {status:200}]
	   * will return
	   * {"IP":"127.0.0.1", "status":200}
	   * </p>
	   * If pretty is set to true, json will return prettyprint json string.
	   *
	   * @return Json of the matched element in the text
	   */
	  public String toJson(Boolean pretty) {
	    if (capture == null) {
	      return "{}";
	    }
	    if (capture.isEmpty()) {
	      return "{}";
	    }

	    this.cleanMap();
	    Gson gs;
	    if (pretty) {
	     gs = new GsonBuilder().setPrettyPrinting().create();
	    } else {
	      gs = new Gson();
	    }
	    return gs.toJson(/* cleanMap( */capture/* ) */);
	  }

	  /**
	   * Get the json representation of the matched element.
	   * <p>
	   * example:
	   * map [ {IP: 127.0.0.1}, {status:200}]
	   * will return
	   * {"IP":"127.0.0.1", "status":200}
	   * </p>
	   *
	   * @return Json of the matched element in the text
	   */
	  public String toJson() {
	    return toJson(false);
	  }

	  /**
	   * Get the map representation of the matched element in the text.
	   *
	   * @return map object from the matched element in the text
	   */
	  public Map<String, Object> toMap() {
	    this.cleanMap();
	    return capture;
	  }

	  /**
	   * Remove and rename the unwanted elelents in the matched map.
	   */
	  private void cleanMap() {
	    garbage.rename(capture);
	    garbage.remove(capture);
	  }

	  /**
	   * Util fct.
	   *
	   * @return boolean
	   */
	  public Boolean isNull() {
	    if (this.match == null) {
	      return true;
	    }
	    return false;
	  }

	  /**
	   * Util fct.
	   *
	   * @param s
	   * @return boolean
	   */
	  private boolean isInteger(String s) {
	    try {
	      Integer.parseInt(s);
	    } catch (NumberFormatException e) {
	      return false;
	    }
	    return true;
	  }
	  
	}