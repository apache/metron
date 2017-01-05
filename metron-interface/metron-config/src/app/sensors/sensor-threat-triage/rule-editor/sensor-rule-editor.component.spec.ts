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

import {async, TestBed, ComponentFixture} from '@angular/core/testing';
import {SensorRuleEditorComponent} from './sensor-rule-editor.component';
import {SharedModule} from '../../../shared/shared.module';
import {SimpleChanges, SimpleChange} from '@angular/core';
import {SensorParserConfig} from '../../../model/sensor-parser-config';
import {SensorEnrichmentConfig, EnrichmentConfig, ThreatIntelConfig} from '../../../model/sensor-enrichment-config';

describe('Component: SensorStellarComponent', () => {

    let fixture: ComponentFixture<SensorRuleEditorComponent>;
    let component: SensorRuleEditorComponent;
    let sensorParserConfig: SensorParserConfig = new SensorParserConfig();
    sensorParserConfig.sensorTopic = 'bro';
    sensorParserConfig.parserClassName = 'org.apache.metron.parsers.bro.BasicBroParser';
    sensorParserConfig.parserConfig = {};
    let sensorParserConfigString = '{"parserClassName":"org.apache.metron.parsers.bro.BasicBroParser","sensorTopic":"bro",' +
        '"parserConfig": {},"fieldTransformations":[]}';
    let sensorEnrichmentConfig = new SensorEnrichmentConfig();
    sensorEnrichmentConfig.index = 'bro';
    sensorEnrichmentConfig.batchSize = 5;
    sensorEnrichmentConfig.enrichment = Object.assign(new EnrichmentConfig(), {
      'fieldMap': {
        'geo': ['ip_dst_addr', 'ip_src_addr'],
        'host': ['host']
      }
    });
    sensorEnrichmentConfig.threatIntel = Object.assign(new ThreatIntelConfig(), {
          'fieldMap': {
            'hbaseThreatIntel': ['ip_src_addr', 'ip_dst_addr']
          },
          'fieldToTypeMap': {
            'ip_src_addr' : ['malicious_ip'],
            'ip_dst_addr' : ['malicious_ip']
          }
        });

    let sensorEnrichmentConfigString = '{"index": "bro","batchSize": 5,"enrichment" : {"fieldMap": ' +
        '{"geo": ["ip_dst_addr", "ip_src_addr"],"host": ["host"]}},"threatIntel": {"fieldMap": {"hbaseThreatIntel":' +
        ' ["ip_src_addr", "ip_dst_addr"]},"fieldToTypeMap": {"ip_src_addr" : ["malicious_ip"],"ip_dst_addr" : ["malicious_ip"]}}}';

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            imports: [SharedModule
            ],
            declarations: [ SensorRuleEditorComponent ],
            providers: [
              SensorRuleEditorComponent
            ]
        });

        fixture = TestBed.createComponent(SensorRuleEditorComponent);
        component = fixture.componentInstance;
    }));

    it('should create an instance', () => {
        expect(component).toBeDefined();
    });
});
