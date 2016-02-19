exports = module.exports = function(config) {
  var httpProxy = require('http-proxy');
  var proxy = httpProxy.createProxy();

  proxy.on('error', function (err, req, res) {
    console.log("[proxyError]", err);
  });

  return function(req, res, next) {
    if (!req.user) {
      res.send(403, 'Forbidden!');
      return;
    }

    delete req.headers.cookie;
    proxy.web(req, res, {
      target: config.elasticsearch.url
    });
  };
};
