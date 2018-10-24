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

// Protractor configuration file, see link for more information
// https://github.com/angular/protractor/blob/master/docs/referenceConf.js

/*global jasmine */
const { SpecReporter } = require('jasmine-spec-reporter');

exports.config = {
  allScriptsTimeout: 15000,
  specs: [
    './e2e/login/login.e2e-spec.ts',
    './e2e/alerts-list/alerts-list.e2e-spec.ts',
    './e2e/alerts-list/configure-table/configure-table.e2e-spec.ts',
    './e2e/alerts-list/save-search/save-search.e2e-spec.ts',
    './e2e/alerts-list/tree-view/tree-view.e2e-spec.ts',
    './e2e/alerts-list/table-view/table-view.e2e-spec.ts',
    './e2e/alerts-list/alert-filters/alert-filters.e2e-spec.ts',
    './e2e/alerts-list/alert-status/alerts-list-status.e2e-spec.ts',
    './e2e/alert-details/alert-status/alert-details-status.e2e-spec.ts',
    './e2e/alerts-list/meta-alerts/meta-alert.e2e-spec.ts'
  ],
  capabilities: {
    'browserName': 'chrome',
    'chromeOptions': {
      args: ["--disable-gpu", "--window-size=1435,850", "--headless" ],
      'prefs': {
        'credentials_enable_service': false,
        'profile': { 'password_manager_enabled': false}
      }
    }
  },
  directConnect: true,
  baseUrl: 'http://localhost:4200/',
  framework: 'jasmine',
  jasmineNodeOpts: {
    showColors: true,
    defaultTimeoutInterval: 50000,
    includeStackTrace: true
  },
  beforeLaunch: function () { require('ts-node/register') },
  onPrepare: function () {

    jasmine.getEnv().addReporter(new SpecReporter({ spec: { displayStacktrace: true } }));

    return new Promise(function(resolve, reject) {
      var cleanMetronUpdateTable = require('./e2e/utils/clean_metron_update_table').cleanMetronUpdateTable;
      var createMetaAlertsIndex = require('./e2e/utils/e2e_util').createMetaAlertsIndex;
      cleanMetronUpdateTable()
      .then(function() {
        createMetaAlertsIndex();
        resolve();
      })
      .catch(function (error) {
        reject();
      });
    });
  },
  onComplete: function() {
    var createMetaAlertsIndex =  require('./e2e/utils/e2e_util').createMetaAlertsIndex;
    createMetaAlertsIndex();
  }
};
