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
    if (!req.user || !req.user.permissions.pcap) {
      res.send(403, 'Forbidden!');
      return;
    }

    var transit = [];
    var pcapUrl = config.pcap.url + '/' + req.param('command');
    pcapUrl += '?' + querystring.stringify(req.query);

    var curl = spawn('curl', ['-s', pcapUrl]);
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
