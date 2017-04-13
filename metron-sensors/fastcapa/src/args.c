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

#include "args.h"

typedef int bool;
#define true 1
#define false 0
#define valid(s) (s == NULL ? false : strlen(s) > 1)

/*
 * Print usage information to the user.
 */
static void print_usage(void)
{
    printf("fastcapa [EAL options] -- [APP options]\n"
           "  -p PORT_MASK     bitmask of ports to bind                     [%s]\n"
           "  -b BURST_SIZE    max packets to retrieve at a time            [%d]\n"
           "  -r NB_RX_DESC    num of descriptors for receive ring          [%d]\n"
           "  -x TX_RING_SIZE  size of tx rings (must be a power of 2)      [%d]\n"
           "  -q NB_RX_QUEUE   num of receive queues for each device        [%d]\n"
           "  -t KAFKA_TOPIC   name of the kafka topic                      [%s]\n"
           "  -c KAFKA_CONF    file containing configs for kafka client         \n"
           "  -s KAFKA_STATS   append kafka client stats to a file              \n"
           "  -h               print this help message                          \n",
        STR(DEFAULT_PORT_MASK), 
        DEFAULT_BURST_SIZE, 
        DEFAULT_NB_RX_DESC,
        DEFAULT_TX_RING_SIZE,
        DEFAULT_NB_RX_QUEUE,
        STR(DEFAULT_KAFKA_TOPIC));
}

/*
 * Parse the 'portmask' command line argument.
 */
static int parse_portmask(const char* portmask)
{
    char* end = NULL;
    unsigned long pm;

    // parse hexadecimal string
    pm = strtoul(portmask, &end, 16);

    if ((portmask[0] == '\0') || (end == NULL) || (*end != '\0')) {
        return -1;
    }
    else if (pm == 0) {
        return -1;
    }
    else {
        return pm;
    }
}

/*
 * Check if a file exists
 */
static bool file_exists(const char* filepath)
{
    struct stat buf;
    return (stat(filepath, &buf) == 0);
}

/**
 * Parse the command line arguments passed to the application.
 */
int parse_args(int argc, char** argv)
{
    int opt;
    char** argvopt;
    int option_index;
    int nb_workers;
    static struct option lgopts[] = {
        { NULL, 0, 0, 0 }
    };

    // initialize args
    memset(&app, 0, sizeof(struct app_params));

    // parse arguments to this application
    argvopt = argv;
    while ((opt = getopt_long(argc, argvopt, "hp:b:t:c:r:q:s:x:", lgopts, &option_index)) != EOF) {
        switch (opt) {

            // help
            case 'h':
                print_usage();
                return -1;

            // burst size
            case 'b':
                app.burst_size = atoi(optarg);
                printf("[ -b BURST_SIZE ] defined as %d \n", app.burst_size);

                if(app.burst_size < 1 || app.burst_size > MAX_BURST_SIZE) {
                    fprintf(stderr, "Invalid burst size; burst=%u must be in [1, %u]. \n", app.burst_size, MAX_BURST_SIZE);
                    print_usage();
                    return -1;
                }
                break;

            // number of receive descriptors
            case 'r':
                app.nb_rx_desc = atoi(optarg);
                printf("[ -r NB_RX_DESC ] defined as %d \n", app.nb_rx_desc);

                if (app.nb_rx_desc < 1) {
                    fprintf(stderr, "Invalid num of receive descriptors: '%s' \n", optarg);
                    print_usage();
                    return -1;
                }
                break;

            // size of each transmit ring
            case 'x':
                app.tx_ring_size = atoi(optarg);
                printf("[ -x TX_RING_SIZE ] defined as %d \n", app.tx_ring_size);

                // must be a power of 2 and not 0
                if (app.tx_ring_size == 0 || (app.tx_ring_size & (app.tx_ring_size - 1)) != 0) {
                    fprintf(stderr, "Invalid tx ring size (must be power of 2): '%s' \n", optarg);
                    print_usage();
                    return -1;
                }
                break;

            // number of receive queues for each device
            case 'q':
                app.nb_rx_queue = atoi(optarg);
                printf("[ -q NB_RX_QUEUE ] defined as %d \n", app.nb_rx_queue);

                if (app.nb_rx_queue < 1) {
                    fprintf(stderr, "Invalid num of receive queues: '%s' \n", optarg);
                    print_usage();
                    return -1;
                }
                break;

          // port mask
          case 'p':
              app.enabled_port_mask = parse_portmask(optarg);
              printf("[ -p PORT_MASK ] defined as %d \n", app.enabled_port_mask);

              if (app.enabled_port_mask == 0) {
                  fprintf(stderr, "Invalid portmask: '%s'\n", optarg);
                  print_usage();
                  return -1;
              }
              break;

          // kafka topic
          case 't':
              app.kafka_topic = strdup(optarg);
              printf("[ -t KAFKA_TOPIC ] defined as %s \n", app.kafka_topic);

              if (!valid(app.kafka_topic)) {
                  printf("Invalid kafka topic: '%s'\n", optarg);
                  print_usage();
                  return -1;
              }
              break;

          // kafka config path
          case 'c':
              app.kafka_config_path = strdup(optarg);
              printf("[ -c KAFKA_CONFIG ] defined as %s \n", app.kafka_config_path);

              if (!valid(app.kafka_config_path) || !file_exists(app.kafka_config_path)) {
                  fprintf(stderr, "Invalid kafka config: '%s'\n", optarg);
                  print_usage();
                  return -1;
              }
              break;

          // kafka stats path
          case 's':
              app.kafka_stats_path = strdup(optarg);
              printf("[ -s KAFKA_STATS ] defined as %s \n", app.kafka_stats_path);
              break;

          default:
              print_usage();
              return -1;
          }
    }

    // default PORT_MASK
    if (app.enabled_port_mask == 0) {
        printf("[ -p PORT_MASK ] undefined; defaulting to %s \n", STR(DEFAULT_PORT_MASK));
        app.enabled_port_mask = DEFAULT_PORT_MASK;
    }

    // default KAFKA_TOPIC
    if (!valid(app.kafka_topic)) {
        printf("[ -t KAFKA_TOPIC ] undefined; defaulting to %s \n", STR(DEFAULT_KAFKA_TOPIC));
        app.kafka_topic = STR(DEFAULT_KAFKA_TOPIC);
    }

    // default BURST_SIZE 
    if (app.burst_size == 0) {
        printf("[ -b BURST_SIZE ] undefined; defaulting to %d \n", DEFAULT_BURST_SIZE);
        app.burst_size = DEFAULT_BURST_SIZE;
    }

    // default NB_RX_DESC
    if (app.nb_rx_desc == 0) {
        printf("[ -r NB_RX_DESC ] undefined; defaulting to %d \n", DEFAULT_NB_RX_DESC);
        app.nb_rx_desc = DEFAULT_NB_RX_DESC;
    }

    // default NB_RX_QUEUE
    if (app.nb_rx_queue == 0) {
        printf("[ -q NB_RX_QUEUE ] undefined; defaulting to %d \n", DEFAULT_NB_RX_QUEUE);
        app.nb_rx_queue = DEFAULT_NB_RX_QUEUE;
    }

    // default TX_RING_SIZE
    if (app.tx_ring_size == 0) {
        printf("[ -x TX_RING_SIZE ] undefined; defaulting to %u \n", DEFAULT_TX_RING_SIZE);
        app.tx_ring_size = DEFAULT_TX_RING_SIZE;
    }

    // check number of ethernet devices
    if (rte_eth_dev_count() == 0) {
         rte_exit(EXIT_FAILURE, "No ethernet ports detected.\n");
     }

    // check number of workers
    nb_workers = rte_lcore_count() - 1;
    if (nb_workers < 1) {
        rte_exit(EXIT_FAILURE, "Minimum 2 logical cores required. \n");
    }

    // need at least 1 worker for each receive queue
    if(nb_workers < app.nb_rx_queue) {
        rte_exit(EXIT_FAILURE, "Minimum 1 worker per receive queue; workers=%u rx_queues=%u. \n", 
            nb_workers, app.nb_rx_queue);
    }

    // reset getopt lib
    optind = 0;

    return 0;
}

