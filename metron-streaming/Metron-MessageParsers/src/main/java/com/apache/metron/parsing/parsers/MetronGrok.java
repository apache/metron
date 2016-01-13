package com.opensoc.parsing.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;

public class OpenSOCGrok implements Serializable {

	private static final long serialVersionUID = 2002441320075020721L;
	private static final Logger LOG = LoggerFactory.getLogger(OpenSOCGrok.class);
	  /**
	   * Named regex of the originalGrokPattern.
	   */
	  private String namedRegex;
	  /**
	   * Map of the named regex of the originalGrokPattern
	   * with id = namedregexid and value = namedregex.
	   */
	  private Map<String, String> namedRegexCollection;
	  /**
	   * Original {@code Grok} pattern (expl: %{IP}).
	   */
	  private String originalGrokPattern;
	  /**
	   * Pattern of the namedRegex.
	   */
	  private Pattern compiledNamedRegex;
	  /**
	   * {@code Grok} discovery.
	   */
	  private Map<String, String> grokPatternDefinition;

	  /** only use in grok discovery. */
	  private String savedPattern;

	  /**
	   * Create Empty {@code Grok}.
	   */
	  public static final OpenSOCGrok EMPTY = new OpenSOCGrok();

	  /**
	   * Create a new <i>empty</i>{@code Grok} object.
	   */
	  public OpenSOCGrok() {
	    originalGrokPattern = StringUtils.EMPTY;
	    namedRegex = StringUtils.EMPTY;
	    compiledNamedRegex = null;
	    grokPatternDefinition = new TreeMap<String, String>();
	    namedRegexCollection = new TreeMap<String, String>();
	    savedPattern = StringUtils.EMPTY;
	  }

	  public String getSaved_pattern() {
	    return savedPattern;
	  }

	  public void setSaved_pattern(String savedpattern) {
	    this.savedPattern = savedpattern;
	  }

	  /**
	   * Create a {@code Grok} instance with the given patterns file and
	   * a {@code Grok} pattern.
	   *
	   * @param grokPatternPath Path to the pattern file
	   * @param grokExpression  - <b>OPTIONAL</b> - Grok pattern to compile ex: %{APACHELOG}
	   * @return {@code Grok} instance
	   * @throws Exception
	   */
	  public static OpenSOCGrok create(String grokPatternPath, String grokExpression)
	      throws Exception {
	    if (StringUtils.isBlank(grokPatternPath)) {
	      throw new Exception("{grokPatternPath} should not be empty or null");
	    }
	    OpenSOCGrok g = new OpenSOCGrok();
	    g.addPatternFromFile(grokPatternPath);
	    if (StringUtils.isNotBlank(grokExpression)) {
	      g.compile(grokExpression);
	    }
	    return g;
	  }

	  /**
	   * Create a {@code Grok} instance with the given grok patterns file.
	   *
	   * @param  grokPatternPath : Path to the pattern file
	   * @return Grok
	   * @throws Exception
	   */
	  public static OpenSOCGrok create(String grokPatternPath) throws Exception {
	    return create(grokPatternPath, null);
	  }

	  /**
	   * Add custom pattern to grok in the runtime.
	   *
	   * @param name : Pattern Name
	   * @param pattern : Regular expression Or {@code Grok} pattern
	   * @throws Exception
	   **/
	  public void addPattern(String name, String pattern) throws Exception {
	    if (StringUtils.isBlank(name)) {
	      throw new Exception("Invalid Pattern name");
	    }
	    if (StringUtils.isBlank(name)) {
	      throw new Exception("Invalid Pattern");
	    }
	    grokPatternDefinition.put(name, pattern);
	  }

	  /**
	   * Copy the given Map of patterns (pattern name, regular expression) to {@code Grok},
	   * duplicate element will be override.
	   *
	   * @param cpy : Map to copy
	   * @throws Exception
	   **/
	  public void copyPatterns(Map<String, String> cpy) throws Exception {
	    if (cpy == null) {
	      throw new Exception("Invalid Patterns");
	    }

	    if (cpy.isEmpty()) {
	      throw new Exception("Invalid Patterns");
	    }
	    for (Map.Entry<String, String> entry : cpy.entrySet()) {
	      grokPatternDefinition.put(entry.getKey().toString(), entry.getValue().toString());
	    }
	  }

	  /**
	   * Get the current map of {@code Grok} pattern.
	   *
	   * @return Patterns (name, regular expression)
	   */
	  public Map<String, String> getPatterns() {
	    return grokPatternDefinition;
	  }

	  /**
	   * Get the named regex from the {@code Grok} pattern. <p></p>
	   * See {@link #compile(String)} for more detail.
	   * @return named regex
	   */
	  public String getNamedRegex() {
	    return namedRegex;
	  }

	  /**
	   * Add patterns to {@code Grok} from the given file.
	   *
	   * @param file : Path of the grok pattern
	   * @throws Exception
	   */
	  public void addPatternFromFile(String file) throws Exception {

	    File f = new File(file);
	    if (!f.exists()) {
	      throw new Exception("Pattern not found");
	    }

	    if (!f.canRead()) {
	      throw new Exception("Pattern cannot be read");
	    }

	    FileReader r = null;
	    try {
	      r = new FileReader(f);
	      addPatternFromReader(r);
	    } catch (FileNotFoundException e) {
	      throw new Exception(e.getMessage());
	    } catch (@SuppressWarnings("hiding") IOException e) {
	      throw new Exception(e.getMessage());
	    } finally {
	      try {
	        if (r != null) {
	          r.close();
	        }
	      } catch (IOException io) {
	        // TODO(anthony) : log the error
	      }
	    }
	  }

	  /**
	   * Add patterns to {@code Grok} from a Reader.
	   *
	   * @param r : Reader with {@code Grok} patterns
	   * @throws Exception
	   */
	  public void addPatternFromReader(Reader r) throws Exception {
	    BufferedReader br = new BufferedReader(r);
	    String line;
	    // We dont want \n and commented line
	    Pattern pattern = Pattern.compile("^([A-z0-9_]+)\\s+(.*)$");
	    try {
	      while ((line = br.readLine()) != null) {
	        Matcher m = pattern.matcher(line);
	        if (m.matches()) {
	          this.addPattern(m.group(1), m.group(2));
	        }
	      }
	      br.close();
	    } catch (IOException e) {
	      throw new Exception(e.getMessage());
	    } catch (Exception e) {
	      throw new Exception(e.getMessage());
	    }

	  }

	  /**
	   * Match the given <tt>log</tt> with the named regex.
	   * And return the json representation of the matched element
	   *
	   * @param log : log to match
	   * @return json representation og the log
	   */
	  public String capture(String log){
		  OpenSOCMatch match = match(log);
	    match.captures();
	    return match.toJson();
	  }

	  /**
	   * Match the given list of <tt>log</tt> with the named regex
	   * and return the list of json representation of the matched elements.
	   *
	   * @param logs : list of log
	   * @return list of json representation of the log
	   */
	  public List<String> captures(List<String> logs){
	    List<String> matched = new ArrayList<String>();
	    for (String log : logs) {
	    	OpenSOCMatch match = match(log);
	      match.captures();
	      matched.add(match.toJson());
	    }
	    return matched;
	  }

	  /**
	   * Match the given <tt>text</tt> with the named regex
	   * {@code Grok} will extract data from the string and get an extence of {@link Match}.
	   *
	   * @param text : Single line of log
	   * @return Grok Match
	   */
	  public OpenSOCMatch match(String text) {
	    if (compiledNamedRegex == null || StringUtils.isBlank(text)) {
	      return OpenSOCMatch.EMPTY;
	    }

	    Matcher m = compiledNamedRegex.matcher(text);
	    OpenSOCMatch match = new OpenSOCMatch();
	    if (m.find()) {
	      match.setSubject(text);
	      match.setGrok(this);
	      match.setMatch(m);
	      match.setStart(m.start(0));
	      match.setEnd(m.end(0));
	    }
	    return match;
	  }

	  /**
	   * Compile the {@code Grok} pattern to named regex pattern.
	   *
	   * @param pattern : Grok pattern (ex: %{IP})
	   * @throws Exception
	   */
	  public void compile(String pattern) throws Exception {

	    if (StringUtils.isBlank(pattern)) {
	      throw new Exception("{pattern} should not be empty or null");
	    }

	    namedRegex = pattern;
	    originalGrokPattern = pattern;
	    int index = 0;
	    /** flag for infinite recurtion */
	    int iterationLeft = 1000;
	    Boolean continueIteration = true;

	    // Replace %{foo} with the regex (mostly groupname regex)
	    // and then compile the regex
	    while (continueIteration) {
	      continueIteration = false;
	      if (iterationLeft <= 0) {
	        throw new Exception("Deep recursion pattern compilation of " + originalGrokPattern);
	      }
	      iterationLeft--;

	      Matcher m = GrokUtils.GROK_PATTERN.matcher(namedRegex);
	      // Match %{Foo:bar} -> pattern name and subname
	      // Match %{Foo=regex} -> add new regex definition
	      if (m.find()) {
	        continueIteration = true;
	        Map<String, String> group = m.namedGroups();
	        if (group.get("definition") != null) {
	          try {
	            addPattern(group.get("pattern"), group.get("definition"));
	            group.put("name", group.get("name") + "=" + group.get("definition"));
	          } catch (Exception e) {
	            // Log the exeception
	          }
	        }
	        namedRegexCollection.put("name" + index,
	            (group.get("subname") != null ? group.get("subname") : group.get("name")));
	        namedRegex =
	            StringUtils.replace(namedRegex, "%{" + group.get("name") + "}", "(?<name" + index + ">"
	                + grokPatternDefinition.get(group.get("pattern")) + ")");
	        // System.out.println(_expanded_pattern);
	        index++;
	      }
	    }

	    if (namedRegex.isEmpty()) {
	      throw new Exception("Pattern not fount");
	    }
	    // Compile the regex
	    compiledNamedRegex = Pattern.compile(namedRegex);
	  }

	 	  /**
	   * Original grok pattern used to compile to the named regex.
	   *
	   * @return String Original Grok pattern
	   */
	  public String getOriginalGrokPattern(){
	    return originalGrokPattern;
	  }

	  /**
	   * Get the named regex from the given id.
	   *
	   * @param id : named regex id
	   * @return String of the named regex
	   */
	  public String getNamedRegexCollectionById(String id) {
	    return namedRegexCollection.get(id);
	  }

	  /**
	   * Get the full collection of the named regex.
	   *
	   * @return named RegexCollection
	   */
	  public Map<String, String> getNamedRegexCollection() {
	    return namedRegexCollection;
	  }
	}