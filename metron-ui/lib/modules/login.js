exports = module.exports = function(app, config) {
  var passport = require('passport');

  app.get('/', function (req, res, next) {
    if (!req.user) {
      res.redirect('/login');
      return;
    }

    res.render('index', {
      user: JSON.stringify(req.user),
      config: JSON.stringify({
        elasticsearch: config.elasticsearch.url
      })
    });
  });

  app.get('/login', function (req, res) {
    res.render('login', { flash: req.flash() });
  });

  app.post('/login', passport.authenticate('ldapauth', {
    successRedirect: '/',
    failureRedirect: '/login',
    failureFlash: true
  }));

  app.get('/logout', function (req, res) {
    req.logout();
    res.redirect('/login');
  });
};