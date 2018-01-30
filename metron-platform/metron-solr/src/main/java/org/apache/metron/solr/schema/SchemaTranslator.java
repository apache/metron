package org.apache.metron.solr.schema;

import com.google.common.collect.ImmutableSet;
import org.apache.metron.common.utils.JSONUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SchemaTranslator {
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
          "-->\n";
  public static final String DYNAMIC_FIELD_CATCHALL = "<dynamicField name=\"*\" type=\"ignored\" multiValued=\"false\" docValues=\"true\"/>";

  public enum SolrFields {
    STRING("string", "solr.StrField", ImmutableSet.of("text", ""))
    ;
    String solrName;
    String solrType;
    Set<String> elasticsearchTypes;
    SolrFields(String solrName, String solrType, Set<String> elasticsearchTypes) {
      this.solrName = solrName;
      this.solrType = solrType;
      this.elasticsearchTypes = elasticsearchTypes;
    }

    public static SolrFields byElasticsearchType(String type) {
      for(SolrFields f : values()) {
        if(type.contains(type)) {
          return f;
        }
      }
      return null;
    }
  }

  public static String getSchemaFile(String templateFile) {
    return templateFile + ".schema";
  }

  public static void main(String... argv) throws IOException {
    String templateFile = argv[0];
    String schemaFile = getSchemaFile(templateFile);
    Map<String, Object> template = JSONUtils.INSTANCE.load(new File(templateFile), JSONUtils.MAP_SUPPLIER);
    System.out.println("Processing " + template.getOrDefault("template", "unknown template"));
    Map<String, Object> mappings = (Map<String, Object>) template.getOrDefault("mapping", new HashMap<>());
    if(mappings.size() != 1) {
      System.err.println("Unable to process mappings. We expect exactly 1 mapping, there are " + mappings.size() + " mappings specified")
    }

  }
}
