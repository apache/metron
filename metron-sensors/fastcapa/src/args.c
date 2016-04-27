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
void print_usage(const char* prgname)
{
    printf("%s [EAL options] -- [APP options]\n"
           "  -p PORTMASK     hex bitmask of ports to bind  [0x01]\n"
           "  -t KAFKATOPIC   kafka topic                   [pcap]\n"
           "  -c KAFKACONF    kafka config file             [conf/kafka.conf]\n",
        prgname);
}

/*
 * Parse the 'portmask' command line argument.
 */
int parse_portmask(const char* portmask)
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
    char* prgname = argv[0];
    static struct option lgopts[] = {
        { NULL, 0, 0, 0 }
    };

    // initialize args
    memset(&app, 0, sizeof(struct app_params));

    // parse arguments to this application
    argvopt = argv;
    while ((opt = getopt_long(argc, argvopt, "p:b:t:c:", lgopts, &option_index)) != EOF) {
        switch (opt) {

        // portmask
        case 'p':
            app.enabled_port_mask = parse_portmask(optarg);
            if (app.enabled_port_mask == 0) {
                printf("Error: Invalid portmask: '%s'\n", optarg);
                print_usage(prgname);
                return -1;
            }
            break;

        // kafka topic
        case 't':
            app.kafka_topic = strdup(optarg);
            if (!valid(app.kafka_topic)) {
                printf("Error: Invalid kafka topic: '%s'\n", optarg);
                print_usage(prgname);
                return -1;
            }
            break;

        // kafka config path
        case 'c':
            app.kafka_config_path = strdup(optarg);
            if (!valid(app.kafka_config_path) || !file_exists(app.kafka_config_path)) {
                printf("Error: Invalid kafka config: '%s'\n", optarg);
                print_usage(prgname);
                return -1;
            }
            break;

        default:
            printf("Error: Invalid argument: '%s'\n", optarg);
            print_usage(prgname);
            return -1;
        }
    }

    // check for required command-line arguments
    if (app.enabled_port_mask == 0) {
        printf("Error: Missing -p PORTMASK\n");
        print_usage(prgname);
        return -1;
    }

    if (!valid(app.kafka_topic)) {
        printf("Error: Missing -t KAFKATOPIC\n");
        print_usage(prgname);
        return -1;
    }

    argv[optind - 1] = prgname;

    // reset getopt lib
    optind = 0;
    return 0;
}
