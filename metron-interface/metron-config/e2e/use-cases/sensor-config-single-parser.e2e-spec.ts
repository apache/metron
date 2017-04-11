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
import {LoginPage} from '../login/login.po';
import {SensorConfigPage} from '../sensor-config/sensor-config.po';
import {SensorListPage} from '../sensor-list/sensor-list.po';
import {SensorDetailsPage} from '../sensor-config-readonly/sensor-config-readonly.po';

describe('Sensor Config for parser e2e1', function() {
  let page = new SensorConfigPage();
  let sensorListPage = new SensorListPage();
  let sensorDetailsPage = new SensorDetailsPage();
  let loginPage = new LoginPage();
  let grokPathFore2e1 = '/apps/metron/patterns/e2e1';

  beforeAll(() => {
    loginPage.login();
  });

  afterAll(() => {
    loginPage.logout();
  });

  it('should add e2e parser', (done) => {
    let expectedGrokResponse = [
      'action TCP_MISS',
      'bytes 337891',
      'code 200',
      'elapsed 415',
      'ip_dst_addr 207.109.73.154',
      'ip_src_addr 127.0.0.1',
      'method GET',
      'original_string 1467011157.401 415 127.0.0.1 TCP_MISS/200 337891 GET http://www.aliexpress.com/af/shoes.html? - DIRECT/207.109.73.154 text/html', 'timestamp 1467011157.401', 'url http://www.aliexpress.com/af/shoes.html?' ];
    let grokStatement = '%{NUMBER:timestamp} %{INT:elapsed} %{IPV4:ip_src_addr} %{WORD:action}/%{NUMBER:code} %{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url} - %{WORD:UNWANTED}\/%{IPV4:ip_dst_addr} %{WORD:UNWANTED}\/%{WORD:UNWANTED}';
    let sampleMessage = '1467011157.401 415 127.0.0.1 TCP_MISS/200 337891 GET http://www.aliexpress.com/af/shoes.html? - DIRECT/207.109.73.154 text/html';
    let expectedFieldSchemaResponse = [ 'elapsed', 'code', 'ip_dst_addr', 'original_string', 'method', 'bytes', 'action', 'ip_src_addr', 'url', 'timestamp' ];

    page.clickAddButton();
    page.setParserName('e2e1');
    page.setParserType('Grok');

    page.clickGrokStatement();
    page.setSampleMessage('sensor-grok', sampleMessage);
    page.clearGrokStatement(5);
    page.setGrokStatement('E2E1 ' + grokStatement);
    page.testGrokStatement();
    expect(page.getGrokResponse()).toEqual(expectedGrokResponse);
    page.saveGrokStatement();
    expect(page.getGrokStatementFromMainPane()).toEqual(['E2E1 ' +grokStatement]);

    page.clickSchema();
    page.setSampleMessage('sensor-field-schema', '1467011157.401 415 127.0.0.1 TCP_MISS/200 337891 GET http://www.aliexpress.com/af/shoes.html? - DIRECT/207.109.73.154 text/html');
    page.clickSchema();
    expect(page.getFieldSchemaValues()).toEqual(expectedFieldSchemaResponse);
    page.setSchemaConfig('elapsed', ['TRIM', 'TO_INTEGER'], ['geo', 'host'], ['malicious_ip']);
    expect(page.getTransformText()).toEqual(['TO_INTEGER(TRIM(elapsed))']);
    page.saveFieldSchemaConfig();
    page.setSchemaConfig('ip_dst_addr', [], ['geo'], ['malicious_ip']);
    page.saveFieldSchemaConfig();
    page.closeSchemaPane();
    expect(page.getFieldSchemaSummary()).toEqual( [ 'TRANSFORMATIONS 1', 'ENRICHMENTS 3', 'THREAT INTEL 2' ]);

    page.clickThreatTriage();
    page.clickAddThreatTriageRule();
    page.setThreatTriageRule('IN_SUBNET(ip_dst_addr, \'192.168.0.0/24\')');
    page.setThreatTriageRuleScore('10');
    page.saveThreatTriageRule();
    expect(page.getThreatTrigaeRule()).toEqual([ 'IN_SUBNET(ip_dst_addr, \'192.168.0.0/24\')']);

    page.clickThreatTriage();
    page.clickAddThreatTriageRule();
    page.setThreatTriageRule('ip_src_addr == \'10.0.2.3\' or ip_dst_addr == \'10.0.2.3\'');
    page.setThreatTriageRuleScore('5');
    page.saveThreatTriageRule();
    expect(page.getThreatTrigaeRule()).toEqual([ 'IN_SUBNET(ip_dst_addr, \'192.168.0.0/24\')', 'ip_src_addr == \'10.0.2.3\' or ip_dst_addr == \'10.0.2.3\'']);

    page.setThreatTriageRuleSortBy('Lowest Score');
    expect(page.getThreatTrigaeRule()).toEqual([ 'ip_src_addr == \'10.0.2.3\' or ip_dst_addr == \'10.0.2.3\'', 'IN_SUBNET(ip_dst_addr, \'192.168.0.0/24\')']);

    page.setThreatTriageRuleSortBy('Lowest Name');
    expect(page.getThreatTrigaeRule()).toEqual([ 'IN_SUBNET(ip_dst_addr, \'192.168.0.0/24\')', 'ip_src_addr == \'10.0.2.3\' or ip_dst_addr == \'10.0.2.3\'']);

    page.closeThreatTriagePane();
    expect(page.getThreatTriageSummary()).toEqual([ 'RULES 2' ]);

    page.saveParser();
    
    done();

  }, 50000);

  it('should have all the config for e2e parser', (done) => {
    let grokStatement = 'E2E1 %{NUMBER:timestamp} %{INT:elapsed} %{IPV4:ip_src_addr} %{WORD:action}/%{NUMBER:code} %{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url} - %{WORD:UNWANTED}/%{IPV4:ip_dst_addr} %{WORD:UNWANTED}/%{WORD:UNWANTED}';
    let expectedFormData = {
      title: 'e2e1',
      parserName: 'e2e1',
      parserType: 'org.apache.metron.parsers.GrokParser',
      grokStatement: grokStatement,
      fieldSchemaSummary: [ 'TRANSFORMATIONS 1', 'ENRICHMENTS 3', 'THREAT INTEL 2' ],
      threatTriageSummary: [ 'RULES 2' ],
      hdfsIndex: 'e2e1',
      hdfsBatchSize: '1',
      hdfsEnabled: 'on',
      solrIndex: 'e2e1',
      solrBatchSize: '1',
      solrEnabled: 'on',
      advanced: [ 'grokPath', '/apps/metron/patterns/e2e1', 'patternLabel', 'E2E1', 'enter field', 'enter value' ]
    };
    expect(sensorListPage.openEditPane('e2e1')).toEqual('http://localhost:4200/sensors(dialog:sensors-config/e2e1)');
    expect(page.getFormData()).toEqual(expectedFormData);

    page.closeMainPane().then(() => {
      done();
    });
  });

  it('should have all the config details for  e2e parser', () => {
    let parserNotRunnigExpected = [ '',
      'PARSER:Grok',
      'LAST UPDATED:-',
      'LAST EDITOR:-',
      'STATE:-',
      'ORIGINATOR:-',
      'CREATION DATE:-',
      ' ',
      'STORM:Stopped',
      'LATENCY:-',
      'THROUGHPUT:-',
      'EMITTED(10 MIN):-',
      'ACKED(10 MIN):-',
      ' ',
      'KAFKA:No Kafka Topic',
      'PARTITONS:-',
      'REPLICATION FACTOR:-',
      ''
    ];

    let grokStatement = 'E2E1 %{NUMBER:timestamp} %{INT:elapsed} %{IPV4:ip_src_addr} %{WORD:action}/%{NUMBER:code} %{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url} - %{WORD:UNWANTED}\/%{IPV4:ip_dst_addr} %{WORD:UNWANTED}\/%{WORD:UNWANTED}';

    sensorDetailsPage.navigateTo('e2e1')
    expect(sensorDetailsPage.getCurrentUrl()).toEqual('http://localhost:4200/sensors(dialog:sensors-readonly/e2e1)');
    expect(sensorDetailsPage.getTitle()).toEqual("e2e1");
    expect(sensorDetailsPage.getParserConfig()).toEqual(parserNotRunnigExpected);
    expect(sensorDetailsPage.getButtons()).toEqual([ 'EDIT', 'START', 'Delete' ]);
    expect(sensorDetailsPage.getGrokStatement()).toEqual(grokStatement);

    expect(sensorDetailsPage.getSchemaSummaryTitle()).toEqual(['Transforms']);
    expect(sensorDetailsPage.getSchemaSummary()).toEqual(['elapsed']);
    sensorDetailsPage.clickToggleShowMoreLess('show more', 1);
    expect(sensorDetailsPage.getSchemaFullSummary()).toEqual({ 'elapsed':'TO_INTEGER(TRIM(elapsed))' });
    sensorDetailsPage.clickToggleShowMoreLess('show less', 0);

    expect(sensorDetailsPage.getThreatTriageSummary()).toEqual([ 'AGGREGATOR', 'MAX', '', 'IN_SUBNET(ip_dst_addr, \'192.168.0.0/24\'), ip_src_addr == \'10.0.2.3\' or ip_dst_addr == \'10.0.2.3\'' ]);
    sensorDetailsPage.clickToggleShowMoreLess('show more', 2);
    expect(sensorDetailsPage.getThreatTriageTableHeaders()).toEqual([ 'NAME', 'SCORE' ]);
    expect(sensorDetailsPage.getThreatTriageTableValues()).toEqual({ 'IN_SUBNET(ip_dst_addr, \'192.168.0.0/24\')': '10', 'ip_src_addr == \'10.0.2.3\' or ip_dst_addr == \'10.0.2.3\'': '5' });
    sensorDetailsPage.clickToggleShowMoreLess('show less', 0);

    sensorDetailsPage.closePane('e2e1');
    
  });


  it('should delete the e2e parser', (done) => {
    expect(sensorListPage.getParserCount()).toEqual(8);
    expect(sensorListPage.deleteParser('e2e1')).toEqual(true);
    expect(sensorListPage.getParserCount()).toEqual(7);
    done();
  })

});
