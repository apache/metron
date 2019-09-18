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

context('Automatic data polling on Alerts View', () => {

  const configuringDefaultStubs = () => {
    cy.route({
      method: 'GET',
      url: '/api/v1/user',
      response: 'user'
    });

    cy.route('GET', '/api/v1/global/config', 'fixture:config.json');
    cy.route('GET', appConfigJSON.contextMenuConfigURL, 'fixture:context-menu.conf.json');
  };

  beforeEach(() => {
    cy.server();
    configuringDefaultStubs();
  });

  it('auto polling should start by clicking on play icon', () => {
    cy.visit('login');
    cy.get('[name="user"]').type('user');
    cy.get('[name="password"]').type('password');
    cy.contains('LOG IN').click();

    cy.route({
      url: '/api/v1/search/search',
      method: 'POST',
      onRequest: (req) => {
        expect(req).to.not.be.undefined;
      }
    });

    cy.get('app-auto-polling > .btn').click();
  });

  it('auto polling should keep polling after start depending on polling interval', () => {
    cy.clock(new Date().getTime());

    cy.visit('login');
    cy.get('[name="user"]').type('user');
    cy.get('[name="password"]').type('password');
    cy.contains('LOG IN').click();

    cy.route({
      url: '/api/v1/search/search',
      method: 'POST',
      response: 'fixture:search.json',
    }).as('initReq');;

    cy.get('app-auto-polling > .btn').click();

    cy.route({
      url: '/api/v1/search/search',
      method: 'POST',
      response: 'fixture:search-1.1.json',
    }).as('1stPoll');

    cy.tick(10000);

    cy.contains('test-id-1.1').should('be.visible');

    cy.route({
      url: '/api/v1/search/search',
      method: 'POST',
      response: 'fixture:search-1.2.json',
    }).as('2ndPoll');;

    cy.tick(10000);

    cy.contains('test-id-2.1').should('be.visible');

    cy.wait(30000);
    // cy.wait('@thirdPollingRequest').then((req) => {
    //   expect(req).to.not.be.undefined;
    // });

    // cy.tick(10000);
  });
});
