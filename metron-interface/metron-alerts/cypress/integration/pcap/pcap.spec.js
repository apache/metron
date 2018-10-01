/// <reference types="Cypress" />

context('PCAP Tab', () => {

  beforeEach(() => {
    cy.server();
    cy.route({
      method: 'GET',
      url: '/api/v1/user',
      response: 'user'
    });

    cy.route('GET', 'config', 'fixture:config.json');
    cy.route('POST', 'search', 'fixture:search.json');

    cy.route({
      method: 'GET',
      url: '/api/v1/pcap?state=*',
      response: []
    }).as('runningJobs');
    
    cy.visit('http://localhost:4200/login');
    cy.get('[name="user"]').type('user');
    cy.get('[name="password"]').type('password');
    cy.contains('LOG IN').click();
  });

  afterEach(() => {
    cy.get('.logout-link').click();
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
})
