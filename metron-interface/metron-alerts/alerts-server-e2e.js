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
var jsonfile    = require('jsonfile');
var bodyParser  = require('body-parser');
var app         = require('express')();
var path        = require('path');
var compression = require('compression');
var serveStatic = require('serve-static');
var favicon     = require('serve-favicon');
var proxy       = require('http-proxy-middleware');
var argv        = require('optimist')
                  .demand(['p'])
                  .usage('Usage: server.js -p [port]')
                  .describe('p', 'Port to run metron alerts ui')
                  .argv;

var port = argv.p;
var metronUIAddress = '';
var ifaces = os.networkInterfaces();
var restUrl =  argv.r || argv.resturl;
var conf = {
  "elastic": {
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

var indexHTML = function(req, res){
  res.sendFile(path.resolve('dist/index.html'));
};

var searchResult = function(req, res){
  console.log('Serving ', req.originalUrl ,'from alert-list.json');
  jsonfile.readFile('e2e/mock-data/alert-list.json', function(err, obj) {
    if(err) {
      res.json({status: 'error', reason: err.toString()});
      return;
    }

    var filter = req.body.query.query_string.query;
    if (filter !== '*') {
      filter = filter.replace(/\\/g, '');
      var lastIndex = filter.lastIndexOf(':');
      var key = filter.substr(0, lastIndex);
      var value = filter.substr(lastIndex+1);
      obj.hits.hits =  obj.hits.hits.filter(function (hits) {
        return hits._source[key] === value;
      });
    }

    var sortField = req.body.sort && req.body.sort.length === 1 && req.body.sort[0];
    if (sortField) {
      var key = Object.keys(sortField)[0];
      var order = sortField[key].order;
      obj.hits.hits = obj.hits.hits.sort(function(o1, o2) {
        if (!o1._source[key] || !o2._source[key]) {
          return -1;
        } 

        if (typeof(o1._source[key]) === 'number' && typeof(o2._source[key]) === 'number') {
          return order === 'desc' ? o2._source[key]- (o1._source[key]) : o1._source[key] - (o2._source[key]);
        } else {
          return order === 'desc' ? o2._source[key].localeCompare(o1._source[key]) : o1._source[key].localeCompare(o2._source[key]);
        }

      });
    }

    obj.hits.total = obj.hits.hits.length;
    obj.hits.hits = obj.hits.hits.splice(req.body.from, req.body.size);
    res.json(obj);
  });
};

var clusterState = function(req, res){
  console.log('Serving ', req.originalUrl ,'from cluster-state.json');
  jsonfile.readFile('e2e/mock-data/cluster-state.json', function(err, obj) {
    if(err) {
      res.json({status: 'error', reason: err.toString()});
      return;
    }
    res.json(obj);
  });
};


app.use(compression());
app.use(bodyParser.json());
app.use(favicon(path.join(__dirname, 'dist/favicon.ico')));
app.use(serveStatic(path.join(__dirname, 'dist'), {
  maxAge: '1d',
  setHeaders: setCustomCacheControl
}));

app.post('/search/*,-*kibana/_search', searchResult);
app.use('/_cluster', clusterState);
app.get('/alerts-list', indexHTML);
app.get('', indexHTML);
app.use(function(req, res, next){
  res.status(404).sendStatus(304);
});


app.listen(port, function(){
  console.log("Metron alerts ui is listening on " + metronUIAddress);
});
