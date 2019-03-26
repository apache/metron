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

describe('Grok Parser Creation', function() {

  beforeEach(function () {
    cy.server()
      .route('GET', '/api/v1/sensor/parser/config', 'fixture:config.json').as('config')
      .route('GET', '/api/v1/sensor/parser/config/list/available', 'fixture:sensor-config-single-parser/config-list-available.json')
      .route({
        method: 'GET',
        url: '/api/v1/kafka/topic/test-topic',
        status: 200,
        response: {
          name: "bro",
          numPartitions: 1,
          properties: {},
          replicationFactor: 1,
        }
      })
      .route({
        method: 'GET',
        url: '/api/v1/kafka/topic/test-topic/sample',
        status: 200,
        response: '"it has length"'
      }).as('sample')
      .route({
        method: 'POST',
        url: '/api/v1/sensor/parser/config/parseMessage',
        status: 200,
        response: {}
      });

    cy.login();
  });

  it('should add e2e parser', () => {
    cy.get('metron-config-sensor-parser-list [data-qe-id="add-new-sensor-btn"]').click();

    cy.get('metron-config-sensor [data-qe-id="sensor-name-input"]').type('test-grok-parser');
    cy.get('metron-config-sensor [data-qe-id="kafka-topic-input"]').type('test-topic');
    cy.get('metron-config-sensor [data-qe-id="parser-type-select"]').select('Grok');
    cy.wait('@sample').get('metron-config-sensor [data-qe-id="grok-statement-details"]').click();

    const sampleMessage = 'DIRECT/207.109.73.154 text/html';
    const grokStatement = '%{{}NUMBER:timestamp} %{{}INT:elapsed}';

    cy.get('metron-config-sensor-grok [data-qe-id="sample-data-input"]')
      .focus({ force: true })
      .clear({ force: true });

    cy.get('metron-config-sensor-grok [data-qe-id="sample-data-input"]').type(sampleMessage);

    cy.get('metron-config-sensor-grok [data-qe-id="ace-editor"] textarea')
      .focus({ force: true })
      .clear({ force: true });

    cy.get('metron-config-sensor-grok [data-qe-id="ace-editor"] textarea').type(grokStatement, { force: true }).type(' ', { force: true });

    cy.get('metron-config-sensor-grok [data-qe-id="grok-statement-test-btn"]').click();

    cy.get('metron-config-sensor-grok [data-qe-id="pattern-label-select"]').select('%{NUMBER:timestamp}');

    cy.get('metron-config-sensor-grok [data-qe-id="parser-save-btn"]').click();

    const expectedStatement = '%{NUMBER:timestamp} %{INT:elapsed} '

    cy.get('metron-config-sensor [data-qe-id="grok-statement-input"]').should('have.value', expectedStatement);
  });
});