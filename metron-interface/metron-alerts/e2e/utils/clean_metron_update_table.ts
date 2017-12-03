declare var Promise: any;
var chalk = require('chalk');
var Client = require('ssh2').Client;
var errorMsg = '';

export function cleanMetronUpdateTable() {
  return  new Promise(
      function (resolve, reject) {
        cleanupTable(resolve, reject);
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
