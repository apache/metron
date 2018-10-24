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

declare var Promise: any;
var chalk = require('chalk');
var Client = require('ssh2').Client;
var errorMsg = '';

export function cleanMetronUpdateTable() {
  return  new Promise(
      function (resolve, reject) {
        resolve();
        // cleanupTable(resolve, reject);
      }
  );
}

function cleanupTable(resolve, reject) {
  var conn = new Client();
  conn.on('ready', function() {
    console.log(chalk.bold.green('Connected to node1 as root'));
    conn.shell(function(err, stream) {
      if (err) throw err;
      stream.on('close', function() {
        if (errorMsg.length > 0) {
          console.log(chalk.bold.red('Error is:') + errorMsg);
          console.log(chalk.red.bgBlack.bold('Unable to truncate metron_update table in HBase. Most likely reason is HBase is down !!!'));
          reject();
        } else {
          console.log(chalk.bold.green('Truncated metron_update table in HBase'));
          resolve();
        }
        conn.end();
      }).on('data', function(data) {
        console.log('STDOUT: ' + data);
        if (data.indexOf('ERROR') !== -1) {
          errorMsg = data;
        }
      }).stderr.on('data', function(data) {
        console.log('STDERR: ' + data);
      });
      var comands = [
        'echo \'truncate "metron_update"\' | /usr/hdp/current/hbase-master/bin/hbase shell',
        'exit',
        ''
      ];
      stream.end(comands.join('\r\n'));
    });
  }).connect({
    host: 'node1',
    port: 22,
    username: 'root',
    password: 'vagrant'
  });
}
