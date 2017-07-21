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

#include "nic.h"

/**
 * Default receive queue settings.
 */
static const struct rte_eth_conf rx_conf_default = {
    .rxmode = {
        .mq_mode = ETH_MQ_RX_RSS,
        .max_rx_pkt_len = ETHER_MAX_LEN,
        .enable_scatter = 0,
        .enable_lro = 0
    },
    .txmode = {
        .mq_mode = ETH_MQ_TX_NONE,
    },
    .rx_adv_conf = {
      .rss_conf = {
        .rss_hf = ETH_RSS_IP | ETH_RSS_UDP | ETH_RSS_TCP | ETH_RSS_SCTP,
      }
    },
};

/**
 * Default transmit queue settings.
 */
static const struct rte_eth_txconf tx_conf_default = {
    .tx_thresh = {
        .pthresh = 0,
        .hthresh = 0,
        .wthresh = 0
    },
    .tx_rs_thresh = 0,
    .tx_free_thresh = 0,
    .txq_flags = 0,
    .tx_deferred_start = 0
};

/*
 * Initialize a NIC port.
 */
static int init_port(
    const uint8_t port_id,
    struct rte_mempool* mem_pool,
    const uint16_t nb_rx_queue,
    const uint16_t nb_rx_desc)
{
    struct rte_eth_conf rx_conf = rx_conf_default;
    struct rte_eth_txconf tx_conf = tx_conf_default;
    int retval;
    uint16_t q;
    int retry = 5;
    const uint16_t tx_queues = 1;
    int socket;
    struct rte_eth_dev_info dev_info;

    if (port_id >= rte_eth_dev_count()) {
        rte_exit(EXIT_FAILURE, "Port does not exist; port=%u \n", port_id);
        return -1;
    }

    // check that the number of RX queues does not exceed what is supported by the device
    rte_eth_dev_info_get(port_id, &dev_info);
    if (nb_rx_queue > dev_info.max_rx_queues) {
        rte_exit(EXIT_FAILURE, "Too many RX queues for device; port=%u, rx_queues=%u, max_queues=%u \n",
            port_id, nb_rx_queue, dev_info.max_rx_queues);
        return -EINVAL;
    }

    // check that the number of TX queues does not exceed what is supported by the device
    if (tx_queues > dev_info.max_tx_queues) {
        rte_exit(EXIT_FAILURE, "Too many TX queues for device; port=%u, tx_queues=%u, max_queues=%u \n",
            port_id, tx_queues, dev_info.max_tx_queues);
        return -EINVAL;
    }

    retval = rte_eth_dev_configure(port_id, nb_rx_queue, tx_queues, &rx_conf);
    if (retval != 0) {
        rte_exit(EXIT_FAILURE, "Cannot configure device; port=%u, err=%s \n", port_id, strerror(-retval));
        return retval;
    }

    // create the receive queues
    socket = rte_eth_dev_socket_id(port_id);
    for (q = 0; q < nb_rx_queue; q++) {
        retval = rte_eth_rx_queue_setup(port_id, q, nb_rx_desc, socket, NULL, mem_pool);
        if (retval != 0) {
            rte_exit(EXIT_FAILURE, "Cannot setup RX queue; port=%u, err=%s \n", port_id, strerror(-retval));
            return retval;
        }
    }

    // create the transmit queues - at least one TX queue must be setup even though we don't use it
    for (q = 0; q < tx_queues; q++) {
        retval = rte_eth_tx_queue_setup(port_id, q, TX_QUEUE_SIZE, socket, &tx_conf);
        if (retval != 0) {
            rte_exit(EXIT_FAILURE, "Cannot setup TX queue; port=%u, err=%s \n", port_id, strerror(-retval));
            return retval;
        }
    }

    // start the receive and transmit units on the device
    retval = rte_eth_dev_start(port_id);
    if (retval < 0) {
        rte_exit(EXIT_FAILURE, "Cannot start device; port=%u, err=%s \n", port_id, strerror(-retval));
        return retval;
    }

    // retrieve information about the device
    struct rte_eth_link link;
    do {
        rte_eth_link_get_nowait(port_id, &link);

    } while (retry-- > 0 && !link.link_status && !sleep(1));

    // if still no link information, must be down
    if (!link.link_status) {
        rte_exit(EXIT_FAILURE, "Link down; port=%u \n", port_id);
        return 0;
    }

    // enable promisc mode
    rte_eth_promiscuous_enable(port_id);

    // print diagnostics
    struct ether_addr addr;
    rte_eth_macaddr_get(port_id, &addr);
    LOG_INFO(USER1, "Device setup successfully; port=%u, mac=%02" PRIx8 " %02" PRIx8 " %02" PRIx8 " %02" PRIx8 " %02" PRIx8 " %02" PRIx8 "\n",
        (unsigned int) port_id,
        addr.addr_bytes[0], addr.addr_bytes[1],
        addr.addr_bytes[2], addr.addr_bytes[3],
        addr.addr_bytes[4], addr.addr_bytes[5]);

    return 0;
}

/*
 * Preparation for receiving and processing packets.
 */
int init_receive(
    const uint8_t enabled_port_mask,
    const uint16_t nb_rx_queue,
    const uint16_t nb_rx_desc)
{
    unsigned int nb_ports_available;
    unsigned int nb_ports;
    unsigned int port_id;
    unsigned int size;

    nb_ports_available = nb_ports = rte_eth_dev_count();

    // create memory pool
    size = NUM_MBUFS * nb_ports;
    struct rte_mempool* mem_pool = rte_pktmbuf_pool_create("mbuf-pool", size, MBUF_CACHE_SIZE, 0, RTE_MBUF_DEFAULT_BUF_SIZE, rte_socket_id());
    if (mem_pool == NULL) {
        rte_exit(EXIT_FAILURE, "Unable to create memory pool; n=%u, cache_size=%u, data_room_size=%u, socket=%u \n",
            size, MBUF_CACHE_SIZE, RTE_MBUF_DEFAULT_BUF_SIZE, rte_socket_id());
    }

    // initialize each specified ethernet ports
    for (port_id = 0; port_id < nb_ports; port_id++) {

        // skip over ports that are not enabled
        if ((enabled_port_mask & (1 << port_id)) == 0) {
            LOG_INFO(USER1, "Skipping over disabled port '%d'\n", port_id);
            nb_ports_available--;
            continue;
        }

        // initialize the port - creates one receive queue for each worker
        LOG_INFO(USER1, "Initializing port %u\n", (unsigned)port_id);
        if (init_port(port_id, mem_pool, nb_rx_queue, nb_rx_desc) != 0) {
            rte_exit(EXIT_FAILURE, "Cannot initialize port %" PRIu8 "\n", port_id);
        }
    }

    // ensure that we were able to initialize enough ports
    if (nb_ports_available < 1) {
        rte_exit(EXIT_FAILURE, "Error: No available enabled ports. Portmask set?\n");
    }

    return 0;
}

/*
 * Preparation for transmitting packets.
 */
int init_transmit(
    struct rte_ring **tx_rings,
    const unsigned int count,
    const unsigned int size)
{
    unsigned int i;
    char buf[32];

    for(i = 0; i < count; i++) {
        sprintf(buf, "tx-ring-%d", i);
        tx_rings[i] = rte_ring_create(buf, size, rte_socket_id(), RING_F_SP_ENQ);
        if(NULL == tx_rings[i]) {
            rte_exit(EXIT_FAILURE, "Unable to create transmit ring: %s \n", rte_strerror(rte_errno));
        }
    }

    return 0;
}
