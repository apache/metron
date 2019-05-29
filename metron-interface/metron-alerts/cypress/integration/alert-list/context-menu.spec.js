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

context('Context Menu on Alerts', () => {

  beforeEach(() => {
    cy.server();
    cy.route({
      method: 'GET',
      url: '/api/v1/user',
      response: 'user'
    });
    cy.route({
        method: 'POST',
        url: '/api/v1/logout',
        response: []
    });

    cy.route('GET', '/api/v1/global/config', 'fixture:config.json');
    cy.route('POST', 'search', 'fixture:search.json');

    cy.route('GET', appConfigJSON.contextMenuConfigURL, 'fixture:context-menu.conf.json');

    cy.visit('login');
    cy.get('[name="user"]').type('user');
    cy.get('[name="password"]').type('password');
    cy.contains('LOG IN').click();

    cy.get('[data-qe-id="alert-search-btn"]').click();
  });

  it('clicking on a table cell should show context menu', () => {
    cy.get('[data-qe-id="row-5"] > :nth-child(6) > a').click();
    cy.get('[data-qe-id="cm-dropdown"]').should('be.visible');
  });

  it('clicking on "Add to search bar" should apply value to filter bar', () => {
    cy.get('[data-qe-id="row-5"] > :nth-child(6) > a').click();
    cy.get('[data-qe-id="cm-dropdown"]').should('be.visible');
    cy.contains('Add to search bar').click();
    cy.get('.ace_keyword').should('contain', 'ip_src_addr:');
    cy.get('.ace_value').should('contain', '192.168.66.121');
  });

  it('clicking on "Add to search bar" should close the dropdown of context menu', () => {
    cy.get('[data-qe-id="row-5"] > :nth-child(6) > a').click();
    cy.get('[data-qe-id="cm-dropdown"]').should('be.visible');
    cy.contains('Add to search bar').click();
    cy.get('[data-qe-id="cm-dropdown"]').should('not.be.visible');
  });

  it('dynamic items should be rendered', () => {
    cy.get('[data-qe-id="row-5"] > :nth-child(6) > a').click();
    cy.get('[data-qe-id="cm-dropdown"]').should('be.visible');
    cy.get('[data-qe-id="cm-dropdown"]').contains('IP Investigation Notebook').should('be.visible');
  });

  // this use case was a former bug caused by the behaviour of the rxjs Subject class
  // here we pinning down the fix with a test
  it('dynamic items should be rendered after a clicking a predefined item', () => {
    cy.get('[data-qe-id="row-5"] > :nth-child(6) > a').click();
    cy.get('[data-qe-id="cm-dropdown"]').should('be.visible');
    cy.contains('Add to search bar').click();

    cy.wait(300);

    cy.get('[data-qe-id="row-5"] > :nth-child(6) > a').click();
    cy.get('[data-qe-id="cm-dropdown"]').should('be.visible');
    cy.get('[data-qe-id="cm-dropdown"]').contains('IP Investigation Notebook').should('be.visible');
  });

})
