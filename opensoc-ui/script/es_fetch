#!/usr/bin/env node

/*
 * fetch.js
 * A small utility to fetch records from Elasticsearch and save as JSON
 *
 */

var http = require('http')
  , fs = require('fs')
  , _ = require('lodash');

var options = {
  host: process.env.ES_HOST || 'localhost',
  port: 9200
};

var size = 1000;
var fields = [ '_source' ];

// indices to pull test data from
var indices = [
  'sourcefire',
  'qosmos',
  'qradar',
  'fireeye',
  'bro-201405050800'
];

var retrieve = function (index, i) {
  options.path =
    '/' + index + '/_search?size=' + size + '&fields=' + fields.join(',');

  http.get(options, function (response) {
    var data = [];

    response.on('data', function (chunk) {
      data.push(chunk);
    });

    response.on('end', function () {
      var filePath = 'seed/es/' + index + '.json'
        , results = _.pluck(JSON.parse(data.join('')).hits.hits, '_source');

      var output = results.map(function (v) {
        return JSON.stringify(v);
      });

      // ES-friendly bulk format
      var fmt = "{\"index\": { \"_index\": \"" + index +
                "\", \"_type\": \"" + index + "\"}}\n";
      var toWrite = fmt + output.join("\n" + fmt) + "\n";

      fs.writeFile(filePath, toWrite, function (err) {
        if (err) {
          throw err;
        }
      });
    });
  });
};

indices.forEach(retrieve);
