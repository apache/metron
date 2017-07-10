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

#ifndef METRON_NIC_H
#define METRON_NIC_H

#include <signal.h>
#include <unistd.h>
#include <rte_ethdev.h>
#include <rte_mempool.h>
#include <rte_mbuf.h>
#include <rte_errno.h>
#include "types.h"

#define MBUF_CACHE_SIZE 250
#define TX_QUEUE_SIZE 64
#define NUM_MBUFS ((64 * 1024) - 1)

/*
 * Preparation for receiving and processing packets.
 */
int init_receive(
    const uint8_t enabled_port_mask,
    const uint16_t nb_rx_queue,
    const uint16_t nb_rx_desc);

/*
 * Preparation for transmitting packets.
 */
int init_transmit(
    struct rte_ring **tx_rings,
    const unsigned int count,
    const unsigned int size);

#endif
