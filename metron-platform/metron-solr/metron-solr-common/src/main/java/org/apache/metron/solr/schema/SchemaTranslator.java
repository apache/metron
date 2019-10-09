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
package org.apache.metron.solr.schema;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.nio.charset.StandardCharsets;
import org.apache.metron.common.utils.JSONUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class SchemaTranslator {

  public static final String TAB = "  ";
  public static final String PREAMBLE="<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
          "<!--\n" +
          " Licensed to the Apache Software Foundation (ASF) under one or more\n" +
          " contributor license agreements.  See the NOTICE file distributed with\n" +
          " this work for additional information regarding copyright ownership.\n" +
          " The ASF licenses this file to You under the Apache License, Version 2.0\n" +
          " (the \"License\"); you may not use this file except in compliance with\n" +
          " the License.  You may obtain a copy of the License at\n" +
          "\n" +
          "     http://www.apache.org/licenses/LICENSE-2.0\n" +
          "\n" +
          " Unless required by applicable law or agreed to in writing, software\n" +
          " distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
          " WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
          " See the License for the specific language governing permissions and\n" +
          " limitations under the License.\n" +
          "-->";
  public static final String VERSION_FIELD =
          "<field name=\"_version_\" type=\"" + SolrFields.LONG.solrType.getName() + "\" indexed=\"false\" stored=\"false\"/>";
  public static final String ROOT_FIELD =
          "<field name=\"_root_\" type=\"" + SolrFields.STRING.solrType.getName() + "\" indexed=\"true\" stored=\"false\" docValues=\"false\" />";
  public static final String DYNAMIC_FIELD_CATCHALL = "<dynamicField name=\"*\" type=\"ignored\" multiValued=\"false\" docValues=\"true\"/>";
  public static final String UNIQUE_KEY = "<uniqueKey>guid</uniqueKey>";
  public static final String PROPERTIES_KEY = "properties";
  public static final String DYNAMIC_TEMPLATES_KEY = "dynamic_templates";
  public static final String SCHEMA_FORMAT="<schema name=\"%s\" version=\"1.6\">";
  public static final String TEMPLATE_KEY = "template";

  public enum SolrFields {
    STRING( new FieldType("string", "solr.StrField").sortMissingLast()
          , ImmutableSet.of("text", "keyword")),
    BOOLEAN( new FieldType("boolean", "solr.BoolField").sortMissingLast()
           , ImmutableSet.of("boolean")),
    INTEGER( new FieldType("pint", "solr.IntPointField").docValues()
           , ImmutableSet.of("integer")),
    FLOAT( new FieldType("pfloat", "solr.FloatPointField").docValues()
           , ImmutableSet.of("float")),
    LONG( new FieldType("plong", "solr.LongPointField").docValues()
           , ImmutableSet.of("long")),
    DOUBLE( new FieldType("pdouble", "solr.DoublePointField").docValues()
           , ImmutableSet.of("double")),
    BINARY( new FieldType("bytes", "solr.BinaryField").docValues()
           , ImmutableSet.of("binary")),
    LOCATION( new FieldType("location", "solr.LatLonPointSpatialField").docValues()
            , ImmutableSet.of("geo_point")),
    IP(new FieldType("ip", "solr.StrField").sortMissingLast()
      , ImmutableSet.of("ip")),
    TIMESTAMP(new FieldType("timestamp", "solr.LongPointField").docValues()
             , ImmutableSet.of("date")),
    IGNORE( new FieldType("ignored", "solr.StrField").multiValued(), new HashSet<>())
    ;
    FieldType solrType;
    Set<String> elasticsearchTypes;

    SolrFields(FieldType solrType, Set<String> elasticsearchTypes) {
      this.solrType = solrType;
      this.elasticsearchTypes = elasticsearchTypes;
    }

    public String getTypeDeclaration() {
      return solrType.toString();
    }

    public static SolrFields byElasticsearchType(String type) {
      for(SolrFields f : values()) {
        if(f.elasticsearchTypes.contains(type)) {
          return f;
        }
      }
      return null;
    }


    public static void printTypes(PrintWriter pw) {
      for(SolrFields f : values()) {
        pw.println(TAB + f.getTypeDeclaration());
      }
    }
  }

  public static String normalizeField(String fieldName) {
    return fieldName.replace(':', '.');
  }

  public static void processProperties(PrintWriter pw, Map<String, Object> properties) {
    for(Map.Entry<String, Object> property : properties.entrySet()) {
      String fieldName = normalizeField(property.getKey());
      System.out.println("Processing property: " + fieldName);
      if(fieldName.equals("guid")) {
        pw.println(TAB + "<field name=\"guid\" type=\"" + SolrFields.STRING.solrType.getName()
                + "\" indexed=\"true\" stored=\"true\" required=\"true\" multiValued=\"false\" />");
      }
      else {
        String type = (String) ((Map<String, Object>) property.getValue()).get("type");
        SolrFields solrField = SolrFields.byElasticsearchType(type);
        if(solrField == null) {
          System.out.println("Skipping " + fieldName + " because I can't find solr type for " + type);
          continue;
        }
        pw.println(TAB + String.format("<field name=\"%s\" type=\"%s\" indexed=\"true\" stored=\"true\" />", fieldName, solrField.solrType.getName()));
      }
    }
  }

  public static void processDynamicMappings(PrintWriter pw, List<Map<String, Object>> properties) {
    for(Map<String, Object> dynamicProperty : properties) {
      for(Map.Entry<String, Object> dynamicFieldDef : dynamicProperty.entrySet()) {
        System.out.println("Processing dynamic property: " + dynamicFieldDef.getKey());
        Map<String, Object> def = (Map<String, Object>) dynamicFieldDef.getValue();
        String match = (String) def.get("match");
        if(match == null) {
          match = (String) def.get("path_match");
        }
        match = normalizeField(match);
        String type = (String)((Map<String, Object>)def.get("mapping")).get("type");
        SolrFields solrField = SolrFields.byElasticsearchType(type);
        if(solrField == null) {
          System.out.println("Skipping " + match + " because I can't find solr type for " + type);
          continue;
        }
        if(solrField == null) {
          throw new IllegalStateException("Unable to find associated solr type for " + type + " with dynamic property " + solrField);
        }
        pw.println(TAB + String.format("<dynamicField name=\"%s\" type=\"%s\" multiValued=\"false\" docValues=\"true\"/>", match, solrField.solrType.getName()));
      }
    }
  }

  public static void translate(PrintWriter pw, Map<String, Object> template) {
    pw.println(PREAMBLE);
    System.out.println("Processing " + template.getOrDefault(TEMPLATE_KEY, "unknown template"));
    Map<String, Object> mappings = (Map<String, Object>) template.getOrDefault("mappings", new HashMap<>());
    if (mappings.size() != 1) {
      System.err.println("Unable to process mappings. We expect exactly 1 mapping, there are " + mappings.size() + " mappings specified");
    }
    String docName = Iterables.getFirst(mappings.keySet(), null);
    pw.println(String.format(SCHEMA_FORMAT, docName));
    pw.println(TAB + VERSION_FIELD);
    pw.println(TAB + ROOT_FIELD);
    for (Map.Entry<String, Object> docTypeToMapping : mappings.entrySet()) {
      System.out.println("Processing " + docTypeToMapping.getKey() + " doc type");
      Map<String, Object> actualMappings = (Map<String, Object>) docTypeToMapping.getValue();
      Map<String, Object> properties = (Map<String, Object>) actualMappings.getOrDefault(PROPERTIES_KEY, new HashMap<>());
      processProperties(pw, properties);
      List<Map<String, Object>> dynamicMappings = (List<Map<String, Object>>) actualMappings.getOrDefault(DYNAMIC_TEMPLATES_KEY, new ArrayList<>());
      processDynamicMappings(pw, dynamicMappings);
      pw.println(TAB + DYNAMIC_FIELD_CATCHALL);
      pw.println(TAB + UNIQUE_KEY);
      SolrFields.printTypes(pw);
    }
    pw.println("</schema>");
    pw.flush();
  }

  public static void main(String... argv) throws IOException {
    String templateFile = argv[0];
    String schemaFile = argv[1];
    Map<String, Object> template = JSONUtils.INSTANCE.load(new File(templateFile), JSONUtils.MAP_SUPPLIER);
    try(PrintWriter pw = new PrintWriter(new File(schemaFile), StandardCharsets.UTF_8.name())) {
      translate(pw, template);
    }
  }
}
