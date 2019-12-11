/// <reference types="Cypress" />
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
import * as appConfigJSON from '../../../src/assets/app-config.json';

describe('Automatic data polling on Alerts View', () => {

  const configuringDefaultStubs = () => {
    cy.route({
      method: 'GET',
      url: '/api/v1/user',
      response: 'user'
    });

    cy.route('GET', '/api/v1/global/config', 'fixture:config.json');
    cy.route('GET', appConfigJSON.contextMenuConfigURL, 'fixture:context-menu.conf.json');

    cy.route({
      url: '/api/v1/hdfs?path=user-settings',
      method: 'GET',
      response: {},
    });

    cy.route({
      url: '/api/v1/hdfs?path=user-settings',
      method: 'POST',
      response: {},
    });
  };

  beforeEach(() => {
    cy.server();
    configuringDefaultStubs();
  });

  it('auto polling should keep polling after start depending on polling interval', () => {
    cy.visit('login');
    cy.get('[name="user"]').type('user');
    cy.get('[name="password"]').type('password');
    cy.contains('LOG IN').click();

    // defining response for initial poll request
    cy.route({
      url: '/api/v1/search/search',
      method: 'POST',
      response: 'fixture:search.json',
    }).as('initReq');

    cy.log('Turning polling on');
    cy.get('app-auto-polling > .btn').click();

    cy.log('changing interval to 5 sec');
    cy.get('.settings').click();
    cy.get('[value="5"]').click();
    cy.get('.settings').click();

    // defining respons for the first scheduled poll
    cy.route({
      url: '/api/v1/search/search',
      method: 'POST',
      response: 'fixture:search-1.1.json',
    }).as('1stPoll');

    // Waiting 5.5 sec for the request
    cy.wait('@1stPoll', { timeout: 5500 });
    // Validating dom change
    cy.contains('test-id-1.1').should('be.visible');

    // defining respons for the second scheduled poll
    cy.route({
      url: '/api/v1/search/search',
      method: 'POST',
      response: 'fixture:search-1.2.json',
    }).as('2ndPoll');;

    // Waiting 5.5 sec for the request
    cy.wait('@2ndPoll', { timeout: 5500 });
    // Validating dom change
    cy.contains('test-id-2.1').should('be.visible');

    // turning off polling
    cy.get('app-auto-polling > .btn').click();

    cy.route({
      url: '/api/v1/search/search',
      method: 'POST',
      response: 'fixture:search.json',
    });

    cy.wait(5500).then(() => {
      // same element should be visible bc the polling is turned off
    cy.contains('test-id-2.1').should('be.visible');
    })
  });
});
