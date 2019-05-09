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
context('PCAP Tab', () => {

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

    cy.route({
      method: 'GET',
      url: '/api/v1/pcap?state=*',
      response: []
    }).as('runningJobs');

    cy.visit('login');
    cy.get('[name="user"]').type('user');
    cy.get('[name="password"]').type('password');
    cy.contains('LOG IN').click();
  });

  afterEach(() => {
  });

  it('checking running jobs on navigating to PCAP tab', () => {
    cy.contains('PCAP').click();
    cy.wait('@runningJobs').its('url').should('include', '?state=RUNNING');
  });

  it('submitting PCAP job request', () => {
    cy.contains('PCAP').click();
    cy.route('POST', '/api/v1/pcap/fixed', 'fixture:pcap.status-00.json')
      .as('postingPcapJob');

    cy.get('[data-qe-id="ip-src-addr"]').type('222.123.111.000');
    cy.get('[data-qe-id="ip-dst-addr"]').type('111.123.222.000');
    cy.get('[data-qe-id="ip-src-port"]').type('3333');
    cy.get('[data-qe-id="ip-dst-port"]').type('7777');
    cy.get('[data-qe-id="protocol"]').type('24');
    cy.get('[data-qe-id="include-reverse"]').check();
    cy.get('[data-qe-id="packet-filter"]').type('filter');

    cy.get('[data-qe-id="submit-button"]').click();

    cy.wait('@postingPcapJob').then((xhr) => {
      expect(xhr.request.body.ipSrcAddr).to.equal('222.123.111.000');
      expect(xhr.request.body.ipDstAddr).to.equal('111.123.222.000');
      expect(xhr.request.body.ipSrcPort).to.equal('3333');
      expect(xhr.request.body.ipDstPort).to.equal('7777');
      expect(xhr.request.body.protocol).to.equal('24');
      expect(xhr.request.body.includeReverse).to.equal(true);
      expect(xhr.request.body.packetFilter).to.equal('filter');
    });
  });

  it('requesting job status', () => {
    cy.route('POST', '/api/v1/pcap/fixed', 'fixture:pcap.status-00.json');
    cy.route('GET', '/api/v1/pcap/*', 'fixture:pcap.status-01.json').as('jobStatusCheck');

    cy.contains('PCAP').click();
    cy.get('[data-qe-id="submit-button"]').click();

    cy.wait('@jobStatusCheck').its('url').should('include', '/api/v1/pcap/job_1537878471649_0001');
  });

  it('process status in percentage', () => {
    cy.route('POST', '/api/v1/pcap/fixed', 'fixture:pcap.status-00.json');
    cy.route('GET', '/api/v1/pcap/*', 'fixture:pcap.status-01.json').as('jobStatusCheck');

    cy.contains('PCAP').click();
    cy.get('[data-qe-id="submit-button"]').click();

    cy.wait('@jobStatusCheck');

    cy.contains('75%').should('be.visible');
  });

  it('getting pcap json', () => {
    cy.route('POST', '/api/v1/pcap/fixed', 'fixture:pcap.status-00.json');
    cy.route('GET', '/api/v1/pcap/*', 'fixture:pcap.status-02.json').as('statusCheck');
    cy.route('GET', '/api/v1/pcap/*/pdml*', 'fixture:pcap.page-01.json').as('gettingPdml');

    cy.contains('PCAP').click();
    cy.get('[data-qe-id="submit-button"]').click();

    cy.wait('@statusCheck');

    cy.wait('@gettingPdml').its('url').should('include', '/api/v1/pcap/job_1537878471649_0001/pdml?page=1');
  });


  it('rendering pcap table', () => {
    cy.route('POST', '/api/v1/pcap/fixed', 'fixture:pcap.status-00.json');
    cy.route('GET', '/api/v1/pcap/*', 'fixture:pcap.status-02.json').as('statusCheck');
    cy.route('GET', '/api/v1/pcap/*/pdml*', 'fixture:pcap.page-01.json').as('gettingPdml');

    cy.contains('PCAP').click();
    cy.get('[data-qe-id="submit-button"]').click();

    cy.wait('@statusCheck');

    cy.wait('@gettingPdml');

    cy.get('app-pcap-list table').should('be.visible');
    cy.get(':nth-child(3) > .timestamp').should('contain', '1458240269.414664000 Mar 17, 2016 18:44:29.414664000 UTC');
  });

  it('showing pcap details', () => {
    cy.route('POST', '/api/v1/pcap/fixed', 'fixture:pcap.status-00.json');
    cy.route('GET', '/api/v1/pcap/*', 'fixture:pcap.status-02.json').as('statusCheck');
    cy.route('GET', '/api/v1/pcap/*/pdml*', 'fixture:pcap.page-01.json').as('gettingPdml');

    cy.contains('PCAP').click();
    cy.get('[data-qe-id="submit-button"]').click();

    cy.wait('@statusCheck');
    cy.wait('@gettingPdml');

    cy.get('app-pcap-list table').should('be.visible');
    cy.contains('General information').should('not.be.visible');

    cy.get(':nth-child(3) > .timestamp').click();

    cy.contains('General information').should('be.visible');
    cy.get('[data-qe-id="proto"]').should('have.length', 6);
  });

  it('navigating accross pages', () => {
    cy.route('POST', '/api/v1/pcap/fixed', 'fixture:pcap.status-00.json');
    cy.route('GET', '/api/v1/pcap/*', 'fixture:pcap.status-02.json').as('statusCheck');
    cy.route('GET', '/api/v1/pcap/*/pdml*', 'fixture:pcap.page-01.json').as('gettingPdml');

    cy.contains('PCAP').click();
    cy.get('[data-qe-id="submit-button"]').click();

    cy.wait('@statusCheck');
    cy.wait('@gettingPdml');

    cy.contains('Page 1 of 2').should('be.visible');

    cy.get('.fa-chevron-right').click();

    cy.wait('@gettingPdml').its('url').should('include', '?page=2');
  });

  it('downloading pdml', () => {
    cy.route('POST', '/api/v1/pcap/fixed', 'fixture:pcap.status-00.json');
    cy.route('GET', '/api/v1/pcap/*', 'fixture:pcap.status-02.json').as('statusCheck');
    cy.route('GET', '/api/v1/pcap/*/pdml*', 'fixture:pcap.page-01.json').as('gettingPdml');
    // cy.route('GET', '/api/v1/pcap/*/pdml*').as('download');

    cy.contains('PCAP').click();
    cy.get('[data-qe-id="submit-button"]').click();

    cy.wait('@statusCheck');
    cy.wait('@gettingPdml');

    cy.contains('Download').should('be.visible');
    cy.get('[href="/api/v1/pcap/job_1537878471649_0001/raw?page=1"]').should('be.visible');

    cy.get('.fa-chevron-right').click();

    cy.get('[href="/api/v1/pcap/job_1537878471649_0001/raw?page=2"]').should('be.visible');
  });


  it('cancelling (kill) pcap query job', () => {
    cy.route('POST', '/api/v1/pcap/fixed', 'fixture:pcap.status-00.json');
    cy.route('GET', '/api/v1/pcap/*', 'fixture:pcap.status-01.json').as('jobStatusCheck');
    cy.route('DELETE', '/api/v1/pcap/kill/*', 'fixture:pcap.status-02.json').as('killJob');

    cy.contains('PCAP').click();
    cy.get('[data-qe-id="submit-button"]').click();

    cy.wait('@jobStatusCheck');

    cy.get('[data-qe-id="pcap-cancel-query-button"]').click();
    cy.contains('Yes').click();

    cy.wait('@killJob').its('url').should('include', '/api/v1/pcap/kill/job_1537878471649_0001');
  });

  it('showing filter validation messages', () => {
    cy.contains('PCAP').click();

    cy.get('[data-qe-id="ip-src-addr"]').type('aaa.bbb.111.000');
    cy.get('[data-qe-id="ip-dst-addr"]').type('ccc.ddd.222.000');
    cy.get('[data-qe-id="ip-src-port"]').type('99999');
    cy.get('[data-qe-id="ip-dst-port"]').type('aaaa');

    cy.get('.pcap-search-validation-errors').should('be.visible');
    cy.get('.pcap-search-validation-errors li').should('have.length', 4);
  });

  it('showing date validation messages', () => {
    cy.contains('PCAP').click();

    cy.get('[data-qe-id="end-time"]').click();
    cy.get('.pika-select-year').select('2015');
    cy.get('[data-day="11"] > .pika-button').click();

    cy.get('.pcap-search-validation-errors').should('be.visible');
    cy.get('.pcap-search-validation-errors li').should('have.length', 1);
  });
})
