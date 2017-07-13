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

#ifndef METRON_WORKER_H
#define METRON_WORKER_H

#include <stdint.h>
#include <unistd.h>

#include <rte_ethdev.h>

#include "types.h"
#include "kafka.h"

volatile uint8_t quit_signal;

/*
 * Start the receive and transmit works.
 */
int start_workers(
    rx_worker_params* rx_params,
    tx_worker_params* tx_params,
    struct rte_ring **tx_rings,
    app_params *p);

/*
 * Monitors the receive and transmit workers.  Executed by the main thread, while
 * other threads are created to perform the actual packet processing.
 */
int monitor_workers(
    const rx_worker_params *rx_params,
    const unsigned nb_rx_workers,
    const tx_worker_params *tx_params,
    const unsigned nb_tx_workers);

#endif
