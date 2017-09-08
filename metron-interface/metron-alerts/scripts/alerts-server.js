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

'use strict';

var os          = require('os');
var app         = require('express')();
var path        = require('path');
var compression = require('compression');
var serveStatic = require('serve-static');
var favicon     = require('serve-favicon');
var proxy       = require('http-proxy-middleware');
var argv        = require('optimist')
                  .demand(['p', 'r'])
                  .usage('Usage: alert-server.js -p [port] -r [restUrl]')
                  .describe('p', 'Port to run metron management ui')
                  .describe('r', 'Url where metron rest application is available')
                  .argv;

var port = argv.p;
var metronUIAddress = '';
var ifaces = os.networkInterfaces();
var restUrl =  argv.r || argv.resturl;
var conf = {
  "rest": {
    "target": restUrl,
    "secure": false
  }
};

Object.keys(ifaces).forEach(function (dev) {
  ifaces[dev].forEach(function (details) {
    if (details.family === 'IPv4') {
      metronUIAddress += '\n';
      metronUIAddress += 'http://' + details.address + ':' + port;
    }
  });
});

function setCustomCacheControl (res, path) {
  if (serveStatic.mime.lookup(path) === 'text/html') {
    res.setHeader('Cache-Control', 'public, max-age=10')
  }
  res.setHeader("Expires", new Date(Date.now() + 2592000000).toUTCString());
}

var rewriteSearchProxy = proxy({
  target: restUrl,
  ws: true,
  pathRewrite: {
    '^/search' : ''
  }
});

app.use(compression());

app.use('/api', proxy(conf.rest));

app.use(favicon(path.join(__dirname, '../alerts-ui/favicon.ico')));

app.use(serveStatic(path.join(__dirname, '../alerts-ui'), {
  maxAge: '1d',
  setHeaders: setCustomCacheControl
}));

app.get('*', function(req, res){
  res.sendFile(path.resolve('../alerts-ui/index.html'));
});

app.listen(port, function(){
  console.log("Metron alerts ui is listening on " + metronUIAddress);
});
