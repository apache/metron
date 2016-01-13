var _ = require('lodash');
var http = require('http');
var path = require('path');

var express = require('express');

var connect = require('connect');
var flash = require('connect-flash');

var cookieParser = require('cookie-parser');
var bodyParser = require('body-parser');
var cookieSession = require('cookie-session');

var passport = require('passport');
var ldapauth = require('passport-ldapauth');

var esProxy = require('./modules/es-proxy');
var login = require('./modules/login');
var pcap = require('./modules/pcap');

var app = express();
var config = require('./config');


try {
  config = _.merge(config, require('../config'));
  console.log('Loaded config overrides');
} catch(err) {
  console.log('No config overrides provided');
}

app.set('view engine', 'jade');
app.set('views', path.join(__dirname, 'views/'));

// Cookie middleware
app.use(connect.logger('dev'));
app.use(flash());
app.use(cookieParser());
app.use(cookieSession({
  secret: config.secret,
  cookie: {maxAge: 1 * 24 * 60 * 60 * 1000} // 1-day sessions
}));

app.use(passport.initialize());
app.use(passport.session());

app.use("/__es", esProxy(config));
app.use(bodyParser.urlencoded({extended: true}));
app.use(bodyParser.json());

// LDAP integration
passport.use(new ldapauth.Strategy({
  usernameField: 'email',
  passwordField: 'password',
  server: config.ldap
}, function (user, done) {
    return done(null, user);
}));


// Serialize LDAP user into session.
passport.serializeUser(function (ldapUser, done) {
  // ensure that memberOf is an array.
  var memberOf = ldapUser.memberOf || [];
  memberOf = _.isArray(memberOf) ? memberOf : [memberOf];
  ldapUser.memberOf = memberOf;

  // LDAP permissions
  ldapUser.permissions = {};
  var permissions = _.keys(config.permissions);
  _.each(permissions, function (perm) {
    var group = config.permissions[perm];
    ldapUser.permissions[perm] = _.contains(memberOf, group);
  });

  done(null, JSON.stringify(ldapUser));
});


// De-serialize user from session.
passport.deserializeUser(function (ldapUser, done) {
  try {
    done(null, JSON.parse(ldapUser));
  } catch(err) {
    done(null, null);
  }
});


// Setup routes
pcap(app, config);
login(app, config);

// Serve static assets
app.use(connect.static(path.join(__dirname, 'public')));


// Start server
var server = http.createServer(app);
server.listen(config.port || 5000);

exports.app = app;
