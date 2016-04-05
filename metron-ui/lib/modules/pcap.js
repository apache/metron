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

function readRawBytes(size, transit) {
  var buffer = new Buffer(size);
  var bytesRead = 0;
  var bytesLeft, dataLeft, len, leftOver;
  var data, offset;

  while (bytesRead < size) {
    if (!data || offset >= data.length) {
      offset = 0;
      data = transit.shift();
    }

    bytesLeft = size - bytesRead;
    dataLeft = data.length - offset;
    len = bytesLeft < dataLeft  ? bytesLeft : dataLeft;
    data.copy(buffer, bytesRead, offset, offset + len);
    bytesRead += len;
    offset += len;
  }

  if (offset < data.length) {
    dataLeft = data.length - offset;
    leftOver = new Buffer(dataLeft);
    data.copy(leftOver, 0, offset, offset + dataLeft);
    transit.unshift(leftOver);
  }

  return buffer;
}


exports = module.exports = function(app, config) {
  var _ = require("lodash");
  var fs = require("fs");
  var spawn = require('child_process').spawn;
  var querystring = require('querystring');
  var XmlStream = require('xml-stream');

  // Mock pcap service for use in development
  if (config.pcap.mock) {
    app.get('/sample/pcap/:command', function(req, res) {
      res.sendfile('/vagrant/seed/hbot.pcap');
    });
  }

  app.get('/pcap/:command', function(req, res) {
    if (config.auth && (!req.user || !req.user.permissions.pcap)) {
      res.send(403, 'Forbidden!');
      return;
    }

    var transit = [];
    var pcapUrl = config.pcap.url + '/' + req.param('command');
    pcapUrl += '?' + querystring.stringify(req.query);

    var curl = spawn('curl', ['-s', pcapUrl]);

    if (true) {
      res.set('Content-Type', 'application/cap');
      var fileName = req.query.srcIp + "-" + req.query.dstIp + '-' + req.query.srcPort + '-' + req.query.dstPort + '-' + req.query.protocol + '-' + req.query.includeReverseTraffic;
      fileName = fileName.replace(/\./g, '_');
      res.set('Content-Disposition', 'attachment; filename="' + fileName + '.pcap"');
      curl.stdout.pipe(res);
      return;
    }

    var tshark = spawn('tshark', ['-i', '-', '-T', 'pdml']);
    var xml = new XmlStream(tshark.stdout);

    xml.collect('proto');
    xml.collect('field');

    curl.stdout.pipe(tshark.stdin);
    curl.stdout.on('data', function (data) {
      transit.push(data);
    });

    var npcaps = 0;
    xml.on('end', function() {
      res.end(']}');
      curl.stdout.unpipe(tshark.stdin);
      curl.kill('SIGKILL');
      tshark.kill('SIGKILL');
    });

    xml.on('endElement: packet', function(packet) {
      var psize = parseInt(packet.proto[0].$.size);

      if (!npcaps) {
        res.set('Content-Type', 'application/json');
        res.write('{objects: [\n');
        // skip global header
        readRawBytes(24, transit);
      } else {
        res.write(',\n');
      }

      // skip packet header
      readRawBytes(16, transit);
      packet.hexdump = readRawBytes(psize, transit).toString('hex');
      res.write(JSON.stringify(packet));
      npcaps++;
    });
  });
};
