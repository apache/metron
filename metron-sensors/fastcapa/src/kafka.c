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

/*
 * Passed to all callback functions to help identify the connection.
 */
struct opaque {
    int conn_id;
};

/*
 * Data structures required for the kafka client
 */
static rd_kafka_t **kaf_h;
static rd_kafka_topic_t **kaf_top_h;
static unsigned num_conns;
static FILE *stats_fd;
static app_stats *kaf_conn_stats;
static struct opaque *kaf_opaque;
static uint64_t *kaf_keys;

/*
 * A callback executed when an error occurs within the kafka client
 */
static void kaf_error_cb (
    rd_kafka_t *rk,
    int err,
    const char *reason,
    void* UNUSED(opaque))
{

    LOG_ERROR(USER1, "kafka client error; conn=%s, error=%s [%s] \n",
        rd_kafka_name(rk), rd_kafka_err2str(err), reason);
}

/*
 * A callback executed when a broker throttles the producer
 */
static void kaf_throttle_cb (
    rd_kafka_t *rk,
    const char *broker_name,
    int32_t broker_id,
    int throttle_time_ms,
    void* UNUSED(opaque))
{
    LOG_ERROR(USER1, "kafka client throttle event; conn=%s, time=%dms broker=%s broker_id=%"PRId32" \n",
        rd_kafka_name(rk), throttle_time_ms, broker_name, broker_id);
}

/*
 * A callback executed on a fixed frequency (defined by `statistics.interval.ms`)
 * that provides detailed performance statistics
 */
static int kaf_stats_cb(
    rd_kafka_t *rk,
    char *json,
    size_t UNUSED(json_len),
    void *opaque)
{
    int rc;
    struct opaque *data = (struct opaque*) opaque;
    int conn_id = data->conn_id;

    // update queue depth of this kafka connection
    kaf_conn_stats[conn_id].depth = rd_kafka_outq_len(rk);

    // write json to the stats file
    if(NULL != stats_fd) {
        rc = fprintf(stats_fd, "{ \"conn_id\": \"%u\", \"conn_name\": \"%s\", \"stats\": %s }\n", conn_id, rd_kafka_name(rk), json);
        if(rc < 0) {
            LOG_ERROR(USER1, "Unable to append to stats file \n");
            return rc;
        }
        fflush(stats_fd);
    }

    // 0 ensures the json pointer is immediately freed
    return 0;
}

/*
 * A callback that is called once for each message when it has been successfully
 * produced.
 */
static void kaf_message_delivered_cb (
    #ifdef DEBUG
    rd_kafka_t *rk,
    #else
    rd_kafka_t *UNUSED(rk),
    #endif
    const rd_kafka_message_t *rkmessage,
    void *opaque)
{
    struct opaque *data = (struct opaque*) opaque;
    int conn_id = data->conn_id;

    if(RD_KAFKA_RESP_ERR_NO_ERROR == rkmessage->err) {
        kaf_conn_stats[conn_id].out += 1;
    } else {
        kaf_conn_stats[conn_id].drops += 1;

        #ifdef DEBUG
        LOG_ERROR(USER1, "delivery failed: conn=%s, error=%s \n",
          rd_kafka_name(rk), rd_kafka_err2str(rkmessage->err));
        #endif
    }
}

/*
 * Opens the file used to persist the stats coming out of the kafka client
 */
static int open_stats_file(const char *filename)
{
    int rc;

    stats_fd = fopen(filename, "a");
    if(NULL == stats_fd) {
        LOG_ERROR(USER1, "Unable to open stats file: file=%s, error=%s \n", filename, strerror(errno));
        return -1;
    }

    // mark the file
    rc = fprintf(stats_fd, "{} \n");
    if(rc < 0) {
       LOG_ERROR(USER1, "Unable to append to stats file \n");
       return rc;
    }

    fflush(stats_fd);
    return 0;
}

/*
 * Closes the file used to persist the kafka client stats.
 */
static void close_stats_file(void)
{
    if(NULL != stats_fd) {
        fclose(stats_fd);
    }
}

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
static void parse_kafka_config(const char* file_path, const char* group,
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
                LOG_INFO(USER1, "config[%s]: %s = %s\n", group, keys[i], value);
                option_cb(keys[i], value, arg);
            }
            else {
                LOG_WARN(USER1, "bad config: %s: %s = %s: %s\n", file_path, keys[i], value, errs[0]->message);
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
void kaf_init(int num_of_conns, const char* kafka_topic, const char* kafka_config_path, const char* kafka_stats_path)
{
    int i;
    char errstr[512];

    // open the file to which the kafka stats are appended
    if(NULL != kafka_stats_path) {
        LOG_INFO(USER1, "Appending Kafka client stats to '%s' \n", kafka_stats_path);
        open_stats_file(kafka_stats_path);
    }

    // the number of connections to maintain
    num_conns = num_of_conns;

    // create kafka resources for each consumer
    kaf_h = calloc(num_of_conns, sizeof(rd_kafka_t*));
    kaf_top_h = calloc(num_of_conns, sizeof(rd_kafka_topic_t*));
    kaf_conn_stats = calloc(num_of_conns, sizeof(app_stats));
    kaf_opaque = calloc(num_of_conns, sizeof(struct opaque));
    kaf_keys = calloc(num_of_conns, sizeof(uint64_t));

    for (i = 0; i < num_of_conns; i++) {

        // passed to each callback function to identify the kafka connection
        kaf_opaque[i] = (struct opaque) { .conn_id = i };

        rd_kafka_conf_t* kaf_conf = rd_kafka_conf_new();
        rd_kafka_conf_set_opaque(kaf_conf, (void *) &kaf_opaque[i]);
        rd_kafka_conf_set_error_cb(kaf_conf, kaf_error_cb);
        rd_kafka_conf_set_throttle_cb(kaf_conf, kaf_throttle_cb);
        rd_kafka_conf_set_stats_cb(kaf_conf, kaf_stats_cb);
        rd_kafka_conf_set_dr_msg_cb(kaf_conf, kaf_message_delivered_cb);

        // configure kafka connection; values parsed from kafka config file
        if (NULL != kafka_config_path) {
            parse_kafka_config(kafka_config_path, "kafka-global", kaf_global_option, (void*)kaf_conf);
        }

        // create a new kafka connection
        kaf_h[i] = rd_kafka_new(RD_KAFKA_PRODUCER, kaf_conf, errstr, sizeof(errstr));
        if (!kaf_h[i]) {
            rte_exit(EXIT_FAILURE, "Cannot init kafka connection: %s", errstr);
        }

        // configure kafka topic; values parsed from kafka config file
        rd_kafka_topic_conf_t* topic_conf = rd_kafka_topic_conf_new();
        if (NULL != kafka_config_path) {
            parse_kafka_config(kafka_config_path, "kafka-topic", kaf_topic_option, (void*)topic_conf);
        }

        // connect to a kafka topic
        kaf_top_h[i] = rd_kafka_topic_new(kaf_h[i], kafka_topic, topic_conf);
        if (!kaf_top_h[i]) {
            rte_exit(EXIT_FAILURE, "Cannot init kafka topic: %s", kafka_topic);
        }
    }
}

/*
 * Executes polling across all of the kafka client connections.  Ensures that any queued
 * callbacks are served.
 */
void kaf_poll(void)
{
    unsigned i;
    for (i = 0; i < num_conns; i++) {
        rd_kafka_poll(kaf_h[i], POLL_TIMEOUT_MS);
    }
}

/**
 * Retrieves a summary of statistics across all of the kafka client connections.
 */
int kaf_stats(app_stats *stats)
{
    unsigned i;
    uint64_t in, out, depth, drops;

    in = out = depth = drops = 0;
    for (i = 0; i < num_conns; i++) {
        in += kaf_conn_stats[i].in;
        out += kaf_conn_stats[i].out;
        depth += kaf_conn_stats[i].depth;
        drops += kaf_conn_stats[i].drops;
    }

    stats->in = in;
    stats->out = out;
    stats->depth = depth;
    stats->drops = drops;

    return 0;
}

/**
 * Closes the pool of Kafka connections.
 */
void kaf_close(void)
{
    unsigned i;

    LOG_INFO(USER1, "Closing all Kafka connections \n");
    for (i = 0; i < num_conns; i++) {
       LOG_INFO(USER1, "'%u' message(s) queued on %s \n", rd_kafka_outq_len(kaf_h[i]), rd_kafka_name(kaf_h[i]));
    }

    for (i = 0; i < num_conns; i++) {

        // wait for messages to be delivered
        while (rd_kafka_outq_len(kaf_h[i]) > 0) {
            LOG_INFO(USER1, "Waiting for '%u' message(s) on %s \n", rd_kafka_outq_len(kaf_h[i]), rd_kafka_name(kaf_h[i]));
            rd_kafka_poll(kaf_h[i], POLL_TIMEOUT_MS);
        }

        LOG_INFO(USER1, "All messages cleared on %s \n", rd_kafka_name(kaf_h[i]));
        rd_kafka_flush(kaf_h[i], POLL_TIMEOUT_MS);
        rd_kafka_topic_destroy(kaf_top_h[i]);
        rd_kafka_destroy(kaf_h[i]);
    }

    free(kaf_conn_stats);
    free(kaf_opaque);
    free(kaf_keys);
    close_stats_file();
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
int kaf_send(struct rte_mbuf* pkts[], int pkt_count, int conn_id)
{
    // unassigned partition
    int partition = RD_KAFKA_PARTITION_UA;
    int i;
    int pkts_sent = 0;
    rd_kafka_message_t kaf_msgs[pkt_count];

    // find the topic connection based on the conn_id
    rd_kafka_topic_t* kaf_topic = kaf_top_h[conn_id];

    // current time in epoch microseconds from (big-endian aka network byte order)
    // is added as a message key before being sent to kafka
    kaf_keys[conn_id] = htobe64(current_time());

    // create the batch message for kafka
    for (i = 0; i < pkt_count; i++) {
        kaf_msgs[i].err = 0;
        kaf_msgs[i].rkt = kaf_topic;
        kaf_msgs[i].partition = partition;
        kaf_msgs[i].payload = rte_ctrlmbuf_data(pkts[i]);
        kaf_msgs[i].len = rte_ctrlmbuf_len(pkts[i]);
        kaf_msgs[i].key = (void*) &kaf_keys[conn_id];
        kaf_msgs[i].key_len = sizeof(uint64_t);
        kaf_msgs[i].offset = 0;
    }

    // hand all of the messages off to kafka
    pkts_sent = rd_kafka_produce_batch(kaf_topic, partition, 0, kaf_msgs, pkt_count);

    // update stats
    kaf_conn_stats[conn_id].in += pkt_count;
    kaf_conn_stats[conn_id].drops += (pkt_count - pkts_sent);

    return pkts_sent;
}
