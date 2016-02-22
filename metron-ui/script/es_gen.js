#!/usr/bin/env node

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

/*
 * es_gen.js
 * A small utility that generates json seed data for Elasticsearch
 *
 */

var _ = require('lodash');
var Chance = require('chance');
var fs = require('fs');

var chance = new Chance();
var documentsPerIndex = 1000;
var numEnrichedMachines = 100;
var numOtherMachines = 200;

var oneMonthAgo = new Date();
oneMonthAgo.setMonth(oneMonthAgo.getMonth() - 1);

var startTimestamp = oneMonthAgo.getTime();
var endTimestamp = startTimestamp + (90 * 24 * 60 * 60 * 1000);
var sources = [
  'bro',
  'fireeye',
  'lancope',
  'qosmos',
  'qradar',
  'sourcefire'
];


var inventory = [];
var assetValues = ['important', 'mundane'];
var assetTypes = ['printer', 'server', 'router'];
var alertType = ['error', 'warning', 'alert'];
var clusters = ['preprod', 'cluster A', 'cluster B'];
var protocols = ['tcp', 'udp'];
var protocolMap = {tcp: 6, udp: 17};

function pad(n, width, z) {
  z = z || '0';
  n = n + '';
  return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
}

function ipToHex(ip) {
  var parts = ip.split('.');
  for (var i = 0; i < parts.length; i++) {
    parts[i] = parseInt(parts[i]).toString(16);
  }
  return parts.join('');
}

function choice(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function randomAlert(source) {
  var dst = choice(inventory);
  var src = choice(inventory);
  var protocol = choice(protocols);
  var instance = pad(chance.integer({min: 1, max: 3}), 3);
  var triggered = [];

  for(var i = 0; i < chance.integer({min: 1, max: 1}); i++) {
    triggered.push({
      body: chance.sentence(),
      title: chance.word(),
      type: choice(alertType),
      priority: chance.integer({min: 1, max: 3})
    });
  }

  return {
    alerts: {
      identifier: {
        topology: {
          topology: source,
          topology_instance: source[0].toUpperCase() + instance
        },
        environment: {
          customer: 'mtd',
          instance: 'dev',
          datacenter: choice(clusters)
        }
      },
      triggered: triggered[0]
    },
    message: {
      ip_dst_addr: dst.ip,
      ip_src_addr: src.ip,
      ip_dst_port: chance.integer({min: 22, max: 65535}),
      ip_src_port: chance.integer({min: 22, max: 65535}),
      protocol: protocol,
      original_string: chance.paragraph(),
      timestamp: chance.integer({min: startTimestamp, max: endTimestamp})
    },
    enrichment: {
      geo: {
        ip_dst_addr: dst.geo,
        ip_src_addr: src.geo
      },
      host: {
        ip_dst_addr: dst.host,
        ip_src_addr: src.host
      }
    }
  };
}

for (var i = 0; i < numEnrichedMachines; i++) {
  inventory.push({
    ip: chance.ip(),
    geo: {
      country: 'US',
      dmaCode: chance.integer({min: 500, max: 700}),
      city: chance.city(),
      postalCode: chance.zip(),
      latitude: chance.latitude({fixed: 4}),
      longitude: chance.longitude({fixed: 4}),
      locID: chance.integer({min: 10000, max: 30000})
    },
    host: {
      known_info: {
        asset_value: choice(assetValues),
        type: choice(assetTypes),
        local: choice(['YES', 'NO'])
      }
    }
  });
}

for (var i = 0; i < numOtherMachines; i++) {
  inventory.push({ip: chance.ip()});
}

for (var i = 0; i < sources.length; i++) {
  var source = sources[i];
  var filename = source + '.json';
  var json = fs.createWriteStream('seed/es/' + filename);
  var objects = [];

  for (var j = 0; j < documentsPerIndex; j++) {
    var index = source + '_index';
    var type = source + '_type';
    objects.push(JSON.stringify({index: {_index: index, _type: type}}));

    var alertData = randomAlert(source);
    objects.push(JSON.stringify(alertData));

    objects.push(JSON.stringify({index: {_index: 'pcap_all', _type: 'pcap'}}));
    objects.push(JSON.stringify({
      ip_src_addr: alertData.message.ip_src_addr,
      ip_dst_addr: alertData.message.ip_dst_addr,
      ip_src_port: alertData.message.ip_src_port,
      ip_dst_port: alertData.message.ip_dst_port,
      protocol: protocolMap[alertData.message.protocol],
      pcap_id: [
        ipToHex(alertData.message.ip_src_addr),
        ipToHex(alertData.message.ip_dst_addr),
        protocolMap[alertData.message.protocol],
        alertData.message.ip_src_port,
        alertData.message.ip_dst_port,
        pad(chance.integer({min: 0, max: 99999}), 5),
        pad(chance.integer({min: 0, max: 99999}), 5)
      ].join('-')
    }));
  }

  json.write(objects.join('\n'));
  json.close();
}
