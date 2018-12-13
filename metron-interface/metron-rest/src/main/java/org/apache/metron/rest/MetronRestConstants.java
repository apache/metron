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
package org.apache.metron.rest;

import org.apache.metron.parsers.GrokParser;

public class MetronRestConstants {

  public static final String DEV_PROFILE = "dev";
  public static final String TEST_PROFILE = "test";
  public static final String LDAP_PROFILE = "ldap";
  public static final String DOCKER_PROFILE = "docker";
  public static final String KNOX_PROFILE = "knox";
  public static final String CSRF_ENABLE_PROFILE = "csrf-enable";

  public static final String GROK_TEMP_PATH_SPRING_PROPERTY = "grok.path.temp";
  public static final String GROK_CLASS_NAME = GrokParser.class.getName();
  public static final String GROK_PATH_KEY = "grokPath";

  public static final String STORM_UI_SPRING_PROPERTY = "storm.ui.url";
  public static final String SUPERVISOR_SUMMARY_URL = "/api/v1/supervisor/summary";
  public static final String TOPOLOGY_SUMMARY_URL = "/api/v1/topology/summary";
  public static final String TOPOLOGY_URL = "/api/v1/topology";
  public static final String ENRICHMENT_TOPOLOGY_NAME = "enrichment";
  public static final String BATCH_INDEXING_TOPOLOGY_NAME = "batch_indexing";
  public static final String RANDOM_ACCESS_INDEXING_TOPOLOGY_NAME = "random_access_indexing";
  public static final String PARSER_SCRIPT_PATH_SPRING_PROPERTY = "storm.parser.script.path";
  public static final String ENRICHMENT_SCRIPT_PATH_SPRING_PROPERTY = "storm.enrichment.script.path";
  public static final String BATCH_INDEXING_SCRIPT_PATH_SPRING_PROPERTY = "storm.indexing.batch.script.path";
  public static final String RANDOM_ACCESS_INDEXING_SCRIPT_PATH_SPRING_PROPERTY = "storm.indexing.randomaccess.script.path";
  public static final String PARSER_TOPOLOGY_OPTIONS_SPRING_PROPERTY = "storm.parser.topology.options";
  public static final String KAFKA_SECURITY_PROTOCOL_SPRING_PROPERTY = "kafka.security.protocol";

  public static final String ZK_URL_SPRING_PROPERTY = "zookeeper.url";
  public static final String ZK_CLIENT_SESSION_TIMEOUT = "zookeeper.client.timeout.session";
  public static final String ZK_CLIENT_CONNECTION_TIMEOUT = "zookeeper.client.timeout.connection";
  public static final String CURATOR_SLEEP_TIME = "curator.sleep.time";
  public static final String CURATOR_MAX_RETRIES = "curator.max.retries";

  public static final String KAFKA_BROKER_URL_SPRING_PROPERTY = "kafka.broker.url";
  public static final String KAFKA_TOPICS_ESCALATION_PROPERTY = "kafka.topics.escalation";

  public static final String METRON_ESCALATION_USER_FIELD = "metron_escalation_user";
  public static final String METRON_ESCALATION_TIMESTAMP_FIELD = "metron_escalation_timestamp";

  public static final String KERBEROS_ENABLED_SPRING_PROPERTY = "kerberos.enabled";
  public static final String KERBEROS_PRINCIPLE_SPRING_PROPERTY = "kerberos.principal";
  public static final String KERBEROS_KEYTAB_SPRING_PROPERTY = "kerberos.keytab";

  public static final String SEARCH_MAX_RESULTS = "search.max.results";
  public static final String SEARCH_MAX_GROUPS = "search.max.groups";
  public static final String SEARCH_FACET_FIELDS_SPRING_PROPERTY = "search.facet.fields";
  public static final String INDEX_DAO_IMPL = "index.dao.impl";
  public static final String INDEX_HBASE_TABLE_PROVIDER_IMPL = "index.hbase.provider";
  public static final String INDEX_WRITER_NAME = "index.writer.name";

  public static final String META_DAO_IMPL = "meta.dao.impl";
  public static final String META_DAO_SORT = "meta.dao.sort";

  public static final String SECURITY_ROLE_PREFIX = "ROLE_";
  public static final String SECURITY_ROLE_USER = "USER";
  public static final String SECURITY_ROLE_ADMIN = "ADMIN";

  public static final String USER_SETTINGS_HBASE_TABLE_SPRING_PROPERTY = "user.settings.table";
  public static final String USER_SETTINGS_HBASE_CF_SPRING_PROPERTY = "user.settings.cf";
  public static final String USER_JOB_LIMIT_SPRING_PROPERTY = "user.job.limit";

  public static final String LOGGING_SYSTEM_PROPERTY = "org.springframework.boot.logging.LoggingSystem";

  public static final String PCAP_BASE_PATH_SPRING_PROPERTY = "pcap.base.path";
  public static final String PCAP_BASE_INTERIM_RESULT_PATH_SPRING_PROPERTY = "pcap.base.interim.result.path";
  public static final String PCAP_FINAL_OUTPUT_PATH_SPRING_PROPERTY = "pcap.final.output.path";
  public static final String PCAP_PAGE_SIZE_SPRING_PROPERTY = "pcap.page.size";
  public static final String PCAP_PDML_SCRIPT_PATH_SPRING_PROPERTY = "pcap.pdml.script.path";
  public static final String PCAP_YARN_QUEUE_SPRING_PROPERTY = "pcap.yarn.queue";
  public static final String PCAP_FINALIZER_THREADPOOL_SIZE_SPRING_PROPERTY = "pcap.finalizer.threadpool.size";

  public static final String LDAP_PROVIDER_URL_SPRING_PROPERTY = "ldap.provider.url";
  public static final String LDAP_PROVIDER_USERDN_SPRING_PROPERTY = "ldap.provider.userdn";
  public static final String LDAP_PROVIDER_PASSWORD_SPRING_PROPERTY = "ldap.provider.password";

  public static final String KNOX_ROOT_SPRING_PROPERTY = "knox.root";
}
