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

#include "kafka.h"

#define POLL_TIMEOUT_MS 1000

/*
 * data structures required for the kafka client
 */
static rd_kafka_t** kaf_h;
static rd_kafka_topic_t** kaf_top_h;
static int num_conns;

/**
 * A callback executed for each global Kafka option.
 */
static void kaf_global_option(const char* key, const char* val, void* arg)
{
    rd_kafka_conf_t* conf = (rd_kafka_conf_t*)arg;
    rd_kafka_conf_res_t rc;
    char err[512];

    rc = rd_kafka_conf_set(conf, key, val, err, sizeof(err));
    if (RD_KAFKA_CONF_OK != rc) {
        LOG_WARN(USER1, "unable to set kafka global option: '%s' = '%s': %s\n", key, val, err);
    }
}

/**
 * A callback executed for topic-level Kafka option.
 */
static void kaf_topic_option(const char* key, const char* val, void* arg)
{
    rd_kafka_topic_conf_t* conf = (rd_kafka_topic_conf_t*)arg;
    rd_kafka_conf_res_t rc;
    char err[512];

    rc = rd_kafka_topic_conf_set(conf, key, val, err, sizeof(err));
    if (RD_KAFKA_CONF_OK != rc) {
        LOG_WARN(USER1, "unable to set kafka topic option: '%s' = '%s': %s\n", key, val, err);
    }
}

/**
 * Parses the configuration values from a configuration file.
 */
static void parse_kafka_config(char* file_path, const char* group,
    void (*option_cb)(const char* key, const char* val, void* arg), void* arg)
{

    gsize i;
    gchar* value;
    gchar** keys;
    gsize num_keys;
    GError* err = NULL;
    GError** errs = NULL;

    // load the configuration file
    GKeyFile* gkf = g_key_file_new();
    if (!g_key_file_load_from_file(gkf, file_path, G_KEY_FILE_NONE, &err)) {
        LOG_ERROR(USER1, "bad config: %s: %s\n", file_path, err->message);
    }

    // only grab keys within the specified group
    keys = g_key_file_get_keys(gkf, group, &num_keys, errs);
    if (keys) {

        // execute the callback for each key/value
        for (i = 0; i < num_keys; i++) {
            value = g_key_file_get_value(gkf, group, keys[i], errs);
            if (value) {
                LOG_DEBUG(USER1, "config[%s]: %s = %s\n", group, keys[i], value);
                option_cb(keys[i], value, arg);
            }
            else {
                LOG_INFO(USER1, "bad config: %s: %s = %s: %s\n", file_path, keys[i], value, errs[0]->message);
            }
        }
    }
    else {
        LOG_WARN(USER1, "bad config: %s: %s\n", file_path, errs[0]->message);
    }

    g_strfreev(keys);
    g_key_file_free(gkf);
}

/**
 * Initializes a pool of Kafka connections.
 */
void kaf_init(int num_of_conns)
{
    int i;
    char errstr[512];

    // the number of connections to maintain
    num_conns = num_of_conns;

    // create kafka resources for each consumer
    kaf_h = calloc(num_of_conns, sizeof(rd_kafka_t*));
    kaf_top_h = calloc(num_of_conns, sizeof(rd_kafka_topic_t*));

    for (i = 0; i < num_of_conns; i++) {

        // configure kafka connection; values parsed from kafka config file
        rd_kafka_conf_t* kaf_conf = rd_kafka_conf_new();
        if (NULL != app.kafka_config_path) {
            parse_kafka_config(app.kafka_config_path, "kafka-global", kaf_global_option, (void*)kaf_conf);
        }

        // create a new kafka connection
        kaf_h[i] = rd_kafka_new(RD_KAFKA_PRODUCER, kaf_conf, errstr, sizeof(errstr));
        if (!kaf_h[i]) {
            rte_exit(EXIT_FAILURE, "Cannot init kafka connection: %s", errstr);
        }

        // configure kafka topic; values parsed from kafka config file
        rd_kafka_topic_conf_t* topic_conf = rd_kafka_topic_conf_new();
        if (NULL != app.kafka_config_path) {
            parse_kafka_config(app.kafka_config_path, "kafka-topic", kaf_topic_option, (void*)topic_conf);
        }

        // connect to a kafka topic
        kaf_top_h[i] = rd_kafka_topic_new(kaf_h[i], app.kafka_topic, topic_conf);
        if (!kaf_top_h[i]) {
            rte_exit(EXIT_FAILURE, "Cannot init kafka topic: %s", app.kafka_topic);
        }
    }
}

/**
 * Closes the pool of Kafka connections.
 */
void kaf_close(void)
{
    int i;
    for (i = 0; i < num_conns; i++) {
        // wait for messages to be delivered
        while (rd_kafka_outq_len(kaf_h[i]) > 0) {
            LOG_INFO(USER1, "waiting for %d messages to clear on conn [%i/%i]",
                rd_kafka_outq_len(kaf_h[i]), i + 1, num_conns);
            rd_kafka_poll(kaf_h[i], POLL_TIMEOUT_MS);
        }

        rd_kafka_topic_destroy(kaf_top_h[i]);
        rd_kafka_destroy(kaf_h[i]);
    }
}

/**
 * The current time in microseconds.
 */
static uint64_t current_time(void)
{
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec * (uint64_t)1000000 + tv.tv_usec;
}

/**
 * Publish a set of packets to a kafka topic.
 */
int kaf_send(struct rte_mbuf* data, int pkt_count, int conn_id)
{
    // unassigned partition
    int partition = RD_KAFKA_PARTITION_UA;
    int i;
    int pkts_sent = 0;
    int drops;
    rd_kafka_message_t kaf_msgs[pkt_count];

    // TODO: ensure that librdkafka cleans this up for us
    uint64_t *now = malloc(sizeof(uint64_t));

    // the current time in microseconds from the epoch (in big-endian aka network
    // byte order) is added as a message key before being sent to kafka
    *now = htobe64(current_time());

    // find the topic connection based on the conn_id
    rd_kafka_topic_t* kaf_topic = kaf_top_h[conn_id];

    // create the batch message for kafka
    for (i = 0; i < pkt_count; i++) {
        kaf_msgs[i].err = 0;
        kaf_msgs[i].rkt = kaf_topic;
        kaf_msgs[i].partition = partition;
        kaf_msgs[i].payload = rte_ctrlmbuf_data(&data[i]);
        kaf_msgs[i].len = rte_ctrlmbuf_len(&data[i]);
        kaf_msgs[i].key = (void*) now;
        kaf_msgs[i].key_len = sizeof(uint64_t);
        kaf_msgs[i].offset = 0;
    }

    // hand all of the messages off to kafka
    pkts_sent = rd_kafka_produce_batch(kaf_topic, partition, RD_KAFKA_MSG_F_COPY, kaf_msgs, pkt_count);

    // did we drop packets?
    drops = pkt_count - pkts_sent;
    if (drops > 0) {
        for (i = 0; i < pkt_count; i++) {
            if (!kaf_msgs[i].err) {
                LOG_ERROR(USER1, "'%d' packets dropped, first error: %s", drops, (char*)kaf_msgs[i].payload);
            }
        }
    }

    return pkts_sent;
}
