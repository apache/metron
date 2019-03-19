/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.metron.parsers.regex;

import com.google.common.base.CaseFormat;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

//@formatter:off
/**
 * General purpose class to parse unstructured text message into a json object. This class parses
 * the message as per supplied parser config as part of sensor config. Sensor parser config example:
 *
 * <pre>
 * <code>
 * "convertCamelCaseToUnderScore": true,
 * "recordTypeRegex": "(?&lt;process&gt;(?&lt;=\\s)\\b(kernel|syslog)\\b(?=\\[|:))",
 * "messageHeaderRegex": "(?&lt;syslogpriority&gt;(?&lt;=^&lt;)\\d{1,4}(?=&gt;)).*?(?&lt;timestamp>(?&lt;=&gt;)[A-Za-z]{3}\\s{1,2}\\d{1,2}\\s\\d{1,2}:\\d{1,2}:\\d{1,2}(?=\\s)).*?(?&lt;syslogHost&gt;(?&lt;=\\s).*?(?=\\s))",
 * "fields": [
 * {
 * "recordType": "kernel",
 * "regex": ".*(?&lt;eventInfo&gt;(?&lt;=\\]|\\w\\:).*?(?=$))"
 * },
 * {
 * "recordType": "syslog",
 * "regex": ".*(?&lt;processid&gt;(?&lt;=PID\\s=\\s).*?(?=\\sLine)).*(?&lt;filePath&gt;(?&lt;=64\\s)\/([A-Za-z0-9_-]+\/)+(?=\\w))(?&lt;fileName&gt;.*?(?=\")).*(?&lt;eventInfo&gt;(?&lt;=\").*?(?=$))"
 * }
 * ]
 * </code>
 * </pre>
 *
 * Note: messageHeaderRegex could be specified as lists also e.g.
 *
 * <pre>
 * <code>
 * "messageHeaderRegex": [
 * "regular expression 1",
 * "regular expression 2"
 * ]
 * </code>
 * </pre>
 *
 * Where <strong>regular expression 1</strong> are valid regular expressions and may have named
 * groups, which would be extracted into fields. This list will be evaluated in order until a
 * matching regular expression is found.<br>
 * <br>
 *
 * <strong>Configuration fields explanation</strong>
 *
 * <pre>
 * recordTypeRegex : used to specify a regular expression to distinctly identify a record type.
 * messageHeaderRegex :  used to specify a regular expression to extract fields from a message part which is common across all the messages.
 * e.g. rhel logs looks like
 * <code>
 * <7>Jun 26 16:18:01 hostName kernel: SELinux: initialized (dev tmpfs, type tmpfs), uses transition SIDs
 * </code>
 * <br>
 * </pre>
 *
 * Here message structure (<7>Jun 26 16:18:01 hostName kernel) is common across all messages.
 * Hence messageHeaderRegex could be used to extract fields from this part.
 *
 * fields : json list of objects containing recordType and regex. regex could be a further list e.g.
 *
 * <pre>
 * <code>
 * "regex":  [ "record type specific regular expression 1",
 *             "record type specific regular expression 2"]
 *
 * </code>
 * </pre>
 *
 * <strong>Limitation</strong> <br>
 * Currently the named groups in java regular expressions have a limitation. Only following
 * characters could be used to name a named group. A capturing group can also be assigned a "name",
 * a named-capturing group, and then be back-referenced later by the "name". Group names are
 * composed of the following characters. The first character must be a letter.
 *
 * <pre>
 * <code>
 * The uppercase letters 'A' through 'Z' ('\u0041' through '\u005a'),
 * The lowercase letters 'a' through 'z' ('\u0061' through '\u007a'),
 * The digits '0' through '9' ('\u0030' through '\u0039'),
 * </code>
 * </pre>
 *
 * This means that an _ (underscore), cannot be used as part of a named group name. E.g. this is an
 * invalid regular expression <code>.*(?&lt;event_info&gt;(?&lt;=\\]|\\w\\:).*?(?=$))</code>
 *
 * However, this limitation can be easily overcome by adding a parser configuration setting.
 *
 * <code>
 *  "convertCamelCaseToUnderScore": true,
 * <code>
 * If above property is added to the sensor parser configuration, in parserConfig object, this parser will automatically convert all the camel case property names to underscore seperated.
 * For example, following conversions will automatically happen:
 *
 * <code>
 * ipSrcAddr -> ip_src_addr
 * ipDstAddr -> ip_dst_addr
 * ipSrcPort -> ip_src_port
 * <code>
 * etc.
 */
//@formatter:on
public class RegularExpressionsParser extends BasicParser {

    protected static final Logger LOG =
        LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private List<Map<String, Object>> fields;
    private Map<String, Object> parserConfig;
    private final Pattern namedGroupPattern = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");
    Pattern capitalLettersPattern = Pattern.compile("^.*[A-Z]+.*$");
    private Pattern recordTypePattern;
    private final Set<String> recordTypePatternNamedGroups = new HashSet<>();
    private final Map<String, Map<Pattern, Set<String>>> recordTypePatternMap =
        new LinkedHashMap<>();
    private final Map<Pattern, Set<String>> messageHeaderPatternsMap = new LinkedHashMap<>();

    /**
     * Parses an unstructured text message into a json object based upon the regular expression
     * configuration supplied.
     *
     * @param rawMessage incoming unstructured raw text.
     * @return List of json parsed json objects. In this case list will have a single element only.
     */
    @Override
    public List<JSONObject> parse(byte[] rawMessage) {
        String originalMessage = null;
        try {
            originalMessage = new String(rawMessage, UTF_8).trim();
            LOG.debug(" raw message. {}", originalMessage);
            if (originalMessage.isEmpty()) {
                LOG.warn("Message is empty.");
                return Arrays.asList(new JSONObject());
            }
        } catch (Exception e) {
            LOG.error("[Metron] Could not read raw message. {} " + originalMessage, e);
            throw new RuntimeException(e.getMessage(), e);
        }

        JSONObject parsedJson = new JSONObject();
        if (messageHeaderPatternsMap.size() > 0) {
            parsedJson.putAll(extractHeaderFields(originalMessage));
        }
        parsedJson.putAll(parse(originalMessage));
        parsedJson.put(Constants.Fields.ORIGINAL.getName(), originalMessage);
        /**
         * Populate the output json with default timestamp.
         */
        parsedJson.put(Constants.Fields.TIMESTAMP.getName(), System.currentTimeMillis());
        applyFieldTransformations(parsedJson);
        return Arrays.asList(parsedJson);
    }

    private void applyFieldTransformations(JSONObject parsedJson) {
        if (getParserConfig().get(ParserConfigConstants.CONVERT_CAMELCASE_TO_UNDERSCORE.getName())
            != null && (Boolean) getParserConfig()
            .get(ParserConfigConstants.CONVERT_CAMELCASE_TO_UNDERSCORE.getName())) {
            convertCamelCaseToUnderScore(parsedJson);
        }

    }

    // @formatter:off
  /**
   * This method is called during the parser initialization. It parses the parser
   * configuration and configures the parser accordingly. It then initializes
   * instance variables.
   *
   * @param parserConfig ParserConfig(Map<String, Object>) supplied to the sensor.
   * @see org.apache.metron.parsers.interfaces.Configurable#configure(java.util.Map)<br>
   *      <br>
   */
  // @formatter:on
  @Override
  public void configure(Map<String, Object> parserConfig) {
      setParserConfig(parserConfig);
      setFields((List<Map<String, Object>>) getParserConfig()
          .get(ParserConfigConstants.FIELDS.getName()));
      String recordTypeRegex =
          (String) getParserConfig().get(ParserConfigConstants.RECORD_TYPE_REGEX.getName());

      if (StringUtils.isBlank(recordTypeRegex)) {
          LOG.error("Invalid config :recordTypeRegex is missing in parserConfig");
          throw new IllegalStateException(
              "Invalid config :recordTypeRegex is missing in parserConfig");
      }

      setRecordTypePattern(recordTypeRegex);
      recordTypePatternNamedGroups.addAll(getNamedGroups(recordTypeRegex));
      List<Map<String, Object>> fields =
          (List<Map<String, Object>>) getParserConfig().get(ParserConfigConstants.FIELDS.getName());

      try {
          configureRecordTypePatterns(fields);
          configureMessageHeaderPattern();
      } catch (PatternSyntaxException e) {
          LOG.error("Invalid config : {} ", e.getMessage());
          throw new IllegalStateException("Invalid config : " + e.getMessage());
      }

      validateConfig();
  }

    private void configureMessageHeaderPattern() {
        if (getParserConfig().get(ParserConfigConstants.MESSAGE_HEADER.getName()) != null) {
            if (getParserConfig()
                .get(ParserConfigConstants.MESSAGE_HEADER.getName()) instanceof List) {
                List<String> messageHeaderPatternList = (List<String>) getParserConfig()
                    .get(ParserConfigConstants.MESSAGE_HEADER.getName());
                for (String messageHeaderPatternStr : messageHeaderPatternList) {
                    messageHeaderPatternsMap.put(Pattern.compile(messageHeaderPatternStr),
                        getNamedGroups(messageHeaderPatternStr));
                }
            } else if (getParserConfig()
                .get(ParserConfigConstants.MESSAGE_HEADER.getName()) instanceof String) {
                String messageHeaderPatternStr =
                    (String) getParserConfig().get(ParserConfigConstants.MESSAGE_HEADER.getName());
                if (StringUtils.isNotBlank(messageHeaderPatternStr)) {
                    messageHeaderPatternsMap.put(Pattern.compile(messageHeaderPatternStr),
                        getNamedGroups(messageHeaderPatternStr));
                }
            }
        }
    }

    private void configureRecordTypePatterns(List<Map<String, Object>> fields) {

        for (Map<String, Object> field : fields) {
            if (field.get(ParserConfigConstants.RECORD_TYPE.getName()) != null
                && field.get(ParserConfigConstants.REGEX.getName()) != null) {
                String recordType =
                    ((String) field.get(ParserConfigConstants.RECORD_TYPE.getName())).toLowerCase();
                recordTypePatternMap.put(recordType, new LinkedHashMap<>());
                if (field.get(ParserConfigConstants.REGEX.getName()) instanceof List) {
                    List<String> regexList =
                        (List<String>) field.get(ParserConfigConstants.REGEX.getName());
                    regexList.forEach(s -> {
                        recordTypePatternMap.get(recordType)
                            .put(Pattern.compile(s), getNamedGroups(s));
                    });
                } else if (field.get(ParserConfigConstants.REGEX.getName()) instanceof String) {
                    recordTypePatternMap.get(recordType).put(
                        Pattern.compile((String) field.get(ParserConfigConstants.REGEX.getName())),
                        getNamedGroups((String) field.get(ParserConfigConstants.REGEX.getName())));
                }
            }
        }
    }

    private void setRecordTypePattern(String recordTypeRegex) {
        if (recordTypeRegex != null) {
            recordTypePattern = Pattern.compile(recordTypeRegex);
        }
    }

    private JSONObject parse(String originalMessage) {
        JSONObject parsedJson = new JSONObject();
        Optional<String> recordIdentifier = getField(recordTypePattern, originalMessage);
        if (recordIdentifier.isPresent()) {
            extractNamedGroups(parsedJson, recordIdentifier.get(), originalMessage);
        }
        /*
         * Extract fields(named groups) from record type regular expression
         */
        Matcher matcher = recordTypePattern.matcher(originalMessage);
        if (matcher.find()) {
            for (String namedGroup : recordTypePatternNamedGroups) {
                if (matcher.group(namedGroup) != null) {
                    parsedJson.put(namedGroup, matcher.group(namedGroup).trim());
                }
            }
        }
        return parsedJson;
    }

    private void extractNamedGroups(Map<String, Object> json, String recordType,
        String originalMessage) {
        Map<Pattern, Set<String>> patternMap = recordTypePatternMap.get(recordType.toLowerCase());
        if (patternMap != null) {
            for (Map.Entry<Pattern, Set<String>> entry : patternMap.entrySet()) {
                Pattern pattern = entry.getKey();
                Set<String> namedGroups = entry.getValue();
                if (pattern != null && namedGroups != null && namedGroups.size() > 0) {
                    Matcher m = pattern.matcher(originalMessage);
                    if (m.matches()) {
                        LOG.debug("RecordType : {} Trying regex : {} for message : {} ", recordType,
                            pattern.toString(), originalMessage);
                        for (String namedGroup : namedGroups) {
                            if (m.group(namedGroup) != null) {
                                json.put(namedGroup, m.group(namedGroup).trim());
                            }
                        }
                        break;
                    }
                }
            }
        } else {
            LOG.warn("No pattern found for record type : {}", recordType);
        }
    }

    public Optional<String> getField(Pattern pattern, String originalMessage) {
        Matcher matcher = pattern.matcher(originalMessage);
        while (matcher.find()) {
            return Optional.of(matcher.group());
        }
        return Optional.empty();
    }

    private Set<String> getNamedGroups(String regex) {
        Set<String> namedGroups = new TreeSet<>();
        Matcher matcher = namedGroupPattern.matcher(regex);
        while (matcher.find()) {
            namedGroups.add(matcher.group(1));
        }
        return namedGroups;
    }

    private Map<String, Object> extractHeaderFields(String originalMessage) {
        Map<String, Object> messageHeaderJson = new JSONObject();
        for (Map.Entry<Pattern, Set<String>> syslogPatternEntry : messageHeaderPatternsMap
            .entrySet()) {
            Matcher m = syslogPatternEntry.getKey().matcher(originalMessage);
            if (m.find()) {
                for (String namedGroup : syslogPatternEntry.getValue()) {
                    if (StringUtils.isNotBlank(m.group(namedGroup))) {
                        messageHeaderJson.put(namedGroup, m.group(namedGroup).trim());
                    }
                }
                break;
            }
        }
        return messageHeaderJson;
    }

    @Override
    public void init() {
        LOG.info("RegularExpressions parser initialised.");
    }

    public void validateConfig() {
        if (getFields() == null) {
            LOG.error("Invalid config :  fields is missing in parserConfig");
            throw new IllegalStateException("Invalid config :fields is missing in parserConfig");
        }
    }

    private void convertCamelCaseToUnderScore(Map<String, Object> json) {
        Map<String, String> oldKeyNewKeyMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            if (capitalLettersPattern.matcher(entry.getKey()).matches()) {
                oldKeyNewKeyMap.put(entry.getKey(),
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entry.getKey()));
            }
        }
        oldKeyNewKeyMap.forEach((oldKey, newKey) -> json.put(newKey, json.remove(oldKey)));
    }

    public List<Map<String, Object>> getFields() {
        return fields;
    }

    public void setFields(List<Map<String, Object>> fields) {
        this.fields = fields;
    }

    public Map<String, Object> getParserConfig() {
        return parserConfig;
    }

    public void setParserConfig(Map<String, Object> parserConfig) {
        this.parserConfig = parserConfig;
    }

    enum ParserConfigConstants {
        //@formatter:off
    RECORD_TYPE("recordType"),
    RECORD_TYPE_REGEX("recordTypeRegex"),
    REGEX("regex"),
    FIELDS("fields"),
    MESSAGE_HEADER("messageHeaderRegex"),
    CONVERT_CAMELCASE_TO_UNDERSCORE("convertCamelCaseToUnderScore");
    //@formatter:on
    private final String name;
        private static Map<String, ParserConfigConstants> nameToField;

        ParserConfigConstants(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        static {
            nameToField = new HashMap<>();
            for (ParserConfigConstants f : ParserConfigConstants.values()) {
                nameToField.put(f.getName(), f);
            }
        }

        public static ParserConfigConstants fromString(String fieldName) {
            return nameToField.get(fieldName);
        }
    }
}
