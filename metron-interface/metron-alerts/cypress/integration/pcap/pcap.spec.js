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
    
    cy.visit('http://localhost:4200/login');
    cy.get('[name="user"]').type('user');
    cy.get('[name="password"]').type('password');
    cy.contains('LOG IN').click();

    cy.route({
      method: 'GET',
      url: '/api/v1/pcap?state=RUNNING',
      response: []
    });

    cy.contains('PCAP').click();
  });

  // after(() => {
  //   cy.get('.logout-link').click();
  // });

  it('.blur() - blur off a DOM element', () => {
    
  })
})
