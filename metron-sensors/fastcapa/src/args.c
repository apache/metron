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

/*
 * Print usage information to the user.
 */
static void print_usage(void)
{
    printf("fastcapa [EAL options] -- [APP options]\n"
           "  -p PORT_MASK        bitmask of ports to bind                     [%s]\n"
           "  -b RX_BURST_SIZE    burst size of receive worker                 [%d]\n"
           "  -w TX_BURST_SIZE    burst size of transmit worker                [%d]\n"
           "  -d NB_RX_DESC       num of descriptors for receive ring          [%d]\n"
           "  -x TX_RING_SIZE     size of tx rings (must be a power of 2)      [%d]\n"
           "  -q NB_RX_QUEUE      num of receive queues for each device        [%d]\n"
           "  -t KAFKA_TOPIC      name of the kafka topic                      [%s]\n"
           "  -c KAFKA_CONF       file containing configs for kafka client         \n"
           "  -s KAFKA_STATS      append kafka client stats to a file              \n"
           "  -h                  print this help message                          \n",
        STR(DEFAULT_PORT_MASK),
        DEFAULT_RX_BURST_SIZE,
        DEFAULT_TX_BURST_SIZE,
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

/*
 * Parse the command line arguments passed to the application; the arguments
 * which no not go directly to DPDK's EAL.
 */
static int parse_app_args(int argc, char** argv, app_params* p)
{
    int opt;
    char** argvopt;
    int option_index;
    unsigned int nb_workers;
    static struct option lgopts[] = {
        { NULL, 0, 0, 0 }
    };

    // set default args
    p->enabled_port_mask = parse_portmask(STR(DEFAULT_PORT_MASK));
    p->kafka_topic = STR(DEFAULT_KAFKA_TOPIC);
    p->rx_burst_size = DEFAULT_RX_BURST_SIZE;
    p->tx_burst_size = DEFAULT_TX_BURST_SIZE;
    p->nb_rx_desc = DEFAULT_NB_RX_DESC;
    p->nb_rx_queue = DEFAULT_NB_RX_QUEUE;
    p->tx_ring_size = DEFAULT_TX_RING_SIZE;

    // parse arguments to this application
    argvopt = argv;
    while ((opt = getopt_long(argc, argvopt, "b:c:d:hp:q:s:t:w:x:", lgopts, &option_index)) != EOF) {
        switch (opt) {

            // help
            case 'h':
                print_usage();
                return -1;

            // rx burst size
            case 'b':
                p->rx_burst_size = atoi(optarg);
                if(p->rx_burst_size < 1 || p->rx_burst_size > MAX_RX_BURST_SIZE) {
                    fprintf(stderr, "Invalid burst size; burst=%u must be in [1, %u]. \n", p->rx_burst_size, MAX_RX_BURST_SIZE);
                    print_usage();
                    return -1;
                }
                break;

            // tx burst size
            case 'w':
                p->tx_burst_size = atoi(optarg);
                if(p->tx_burst_size < 1) {
                    fprintf(stderr, "Invalid burst size; burst=%u must be > 0. \n", p->tx_burst_size);
                    print_usage();
                    return -1;
                }
                break;

            // number of receive descriptors
            case 'd':
                p->nb_rx_desc = atoi(optarg);
                if (p->nb_rx_desc < 1) {
                    fprintf(stderr, "Invalid num of receive descriptors: '%s' \n", optarg);
                    print_usage();
                    return -1;
                }
                break;

            // size of each transmit ring
            case 'x':
                p->tx_ring_size = atoi(optarg);

                break;

            // number of receive queues for each device
            case 'q':
                p->nb_rx_queue = atoi(optarg);
                if (p->nb_rx_queue < 1) {
                    fprintf(stderr, "Invalid num of receive queues: '%s' \n", optarg);
                    print_usage();
                    return -1;
                }
                break;

          // port mask
          case 'p':
              p->enabled_port_mask = parse_portmask(optarg);
              if (p->enabled_port_mask == 0) {
                  fprintf(stderr, "Invalid portmask: '%s'\n", optarg);
                  print_usage();
                  return -1;
              }
              break;

          // kafka topic
          case 't':
              p->kafka_topic = strdup(optarg);
              if (!valid(p->kafka_topic)) {
                  printf("Invalid kafka topic: '%s'\n", optarg);
                  print_usage();
                  return -1;
              }
              break;

          // kafka config path
          case 'c':
              p->kafka_config_path = strdup(optarg);
              if (!valid(p->kafka_config_path) || !file_exists(p->kafka_config_path)) {
                  fprintf(stderr, "Invalid kafka config: '%s'\n", optarg);
                  print_usage();
                  return -1;
              }
              break;

          // kafka stats path
          case 's':
              p->kafka_stats_path = strdup(optarg);
              break;

          default:
              print_usage();
              return -1;
          }
    }

    // check number of ethernet devices
    if (rte_eth_dev_count() == 0) {
         rte_exit(EXIT_FAILURE, "No ethernet ports detected.\n");
     }

    // check number of workers
    nb_workers = rte_lcore_count() - 1;

    // need at least 1 worker for each receive queue
    if(nb_workers < p->nb_rx_queue) {
        rte_exit(EXIT_FAILURE, "Minimum 1 worker per receive queue; workers=%u rx_queues=%u. \n",
            nb_workers, p->nb_rx_queue);
    }

    p->nb_rx_workers = p->nb_rx_queue;
    p->nb_tx_workers = nb_workers - p->nb_rx_workers;

    printf("[ -p PORT_MASK ] defined as %d \n", p->enabled_port_mask);
    printf("[ -b RX_BURST_SIZE ] defined as %d \n", p->rx_burst_size);
    printf("[ -w TX_BURST_SIZE ] defined as %d \n", p->tx_burst_size);
    printf("[ -d NB_RX_DESC ] defined as %d \n", p->nb_rx_desc);
    printf("[ -x TX_RING_SIZE ] defined as %d \n", p->tx_ring_size);
    printf("[ -q NB_RX_QUEUE ] defined as %d \n", p->nb_rx_queue);
    printf("[ -t KAFKA_TOPIC ] defined as %s \n", p->kafka_topic);
    printf("[ -c KAFKA_CONFIG ] defined as %s \n", p->kafka_config_path);
    printf("[ -s KAFKA_STATS ] defined as %s \n", p->kafka_stats_path);
    printf("[ NUM_RX_WORKERS ] defined as %d \n", p->nb_rx_workers);
    printf("[ NUM_TX_WORKERS ] defined as %d \n", p->nb_tx_workers);

    // reset getopt lib
    optind = 0;
    return 0;
}

/*
 * Parse the command line arguments passed to the application.
 */
int parse_args(int argc, char** argv, app_params* p)
{
  // initialize the environment
  int ret = rte_eal_init(argc, argv);
  if (ret < 0) {
      rte_exit(EXIT_FAILURE, "Failed to initialize EAL: %i\n", ret);
  }

  // advance past the environmental settings
  argc -= ret;
  argv += ret;

  // parse arguments to the application
  ret = parse_app_args(argc, argv, p);
  if (ret < 0) {
      rte_exit(EXIT_FAILURE, "\n");
  }

  p->nb_ports = rte_eth_dev_count();
  p->nb_rx_workers = p->nb_rx_queue;
  p->nb_tx_workers = (rte_lcore_count() - 1) - p->nb_rx_workers;

  // validate the number of workers
  if(p->nb_tx_workers < p->nb_rx_workers) {
      rte_exit(EXIT_FAILURE, "Additional lcore(s) required; found=%u, required=%u \n",
          rte_lcore_count(), (p->nb_rx_queue*2) + 1);
  }

  return 0;
}
