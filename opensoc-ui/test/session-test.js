/*global assert: true*/

if (!process.env.IN_TRAVIS) {
  var assert = require('chai').assert
    , request = require('supertest')
    , app = require('../lib/opensoc-ui').app;

  describe('sessions', function () {
    describe('log in / log out', function () {
      var session = request.agent(app);

      it('redirects to login unless logged in', function (done) {
        session.
          get('/').
          end(function (err, res) {
            assert.equal(res.statusCode, 302);
            assert.equal(res.header['location'], '/login');
            done();
          });
      });

      it('logs in', function (done) {
        session.
          post('/login').
          send({ email: 'joesmith@opensoc.dev', password: 'opensoc' }).
          end(function (err, res) {
            // redirects to home
            assert.equal(res.header['location'], '/');
            assert.equal(res.statusCode, 302);
            done();
          });
      });

      it('logs out', function (done) {
        session.
          get('/logout').
          end(function (err, res) {
            // redirects to login
            assert.equal(res.header['location'], '/login');
            assert.notMatch(res.header['set-cookie'], /joesmith/);
            assert.equal(res.statusCode, 302);
            done();
          });
      });
    });

    describe('session not set', function () {
      var session = request.agent(app);

      it('fails log in', function (done) {
        session.
          post('/login').
          send({ email: 'joesmith@opensoc.dev', password: 'foobar' }).
          end(function (err, res) {
            assert.equal(res.header['location'], '/login');
            assert.notMatch(res.header['set-cookie'], /joesmith/);
            assert.equal(res.statusCode, 302);
            done();
          });
      });
    });
  });
}
