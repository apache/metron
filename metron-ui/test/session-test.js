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
/*global assert: true*/

if (!process.env.IN_TRAVIS) {
  var assert = require('chai').assert
    , request = require('supertest')
    , app = require('../lib/metron-ui').app;

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
          send({ email: 'joesmith@metron.dev', password: 'metron' }).
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
          send({ email: 'joesmith@metron.dev', password: 'foobar' }).
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
