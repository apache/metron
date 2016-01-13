/* global assert:true */

var assert = require('chai').assert
  , request = require('supertest')
	, app = require('../lib/opensoc-ui').app;

describe('index', function () {
  it('responds with success', function (done) {
    request(app).
      get('/').
      set('Accept', 'text/html').
      expect('Content-Type', /html/).
      expect(302, done); // unauthed
  });
});
