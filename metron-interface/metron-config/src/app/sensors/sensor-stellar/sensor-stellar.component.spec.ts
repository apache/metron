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
import {SensorStellarComponent} from './sensor-stellar.component';
import {SharedModule} from '../../shared/shared.module';
import {SimpleChanges, SimpleChange} from '@angular/core';
import {SensorParserConfig} from '../../model/sensor-parser-config';
import {SensorEnrichmentConfig, EnrichmentConfig, ThreatIntelConfig} from '../../model/sensor-enrichment-config';

describe('Component: SensorStellarComponent', () => {

    let fixture: ComponentFixture<SensorStellarComponent>;
    let component: SensorStellarComponent;
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
            declarations: [ SensorStellarComponent ],
            providers: [
                SensorStellarComponent
            ]
        });

        fixture = TestBed.createComponent(SensorStellarComponent);
        component = fixture.componentInstance;
    }));

    it('should create an instance', () => {
        expect(component).toBeDefined();
    });

    it('should create an instance', () => {
        spyOn(component, 'init');
        let changes: SimpleChanges = {'showStellar': new SimpleChange(false, true)};

        component.ngOnChanges(changes);
        expect(component.init).toHaveBeenCalled();

        changes = {'showStellar': new SimpleChange(true, false)};
        component.ngOnChanges(changes);
        expect(component.init['calls'].count()).toEqual(1);

        fixture.destroy();
    });

    it('should initialise the fields', () => {

        component.sensorParserConfig = this.sensorParserConfig;
        component.sensorEnrichmentConfig = this.sensorEnrichmentConfig;

        component.init();

        expect(component.newSensorParserConfig).toEqual('');
        expect(component.newSensorEnrichmentConfig).toEqual('');

        fixture.destroy();
    });

    it('should save the fields', () => {
        spyOn(component.hideStellar, 'emit');
        spyOn(component.onStellarChanged, 'emit');

        component.sensorParserConfig = sensorParserConfig;
        component.sensorEnrichmentConfig = sensorEnrichmentConfig;

        component.newSensorParserConfig = sensorParserConfigString;
        component.newSensorEnrichmentConfig = sensorEnrichmentConfigString;

        component.ngAfterViewInit();
        component.init();
        component.onSave();

        expect(component.sensorParserConfig).toEqual(sensorParserConfig);
        expect(component.sensorEnrichmentConfig).toEqual(sensorEnrichmentConfig);

        expect(component.hideStellar.emit).toHaveBeenCalled();
        expect(component.onStellarChanged.emit).toHaveBeenCalled();

        fixture.destroy();
    });

    it('should hide panel', () => {
        spyOn(component, 'init');
        spyOn(component.hideStellar, 'emit');

        component.onCancel();

        expect(component.init).toHaveBeenCalled();
        expect(component.hideStellar.emit).toHaveBeenCalled();

        fixture.destroy();
    });

    it('should format transformationConfig', () => {
        component.newSensorParserConfig = '{"parserClassName":"org.apache.metron.parsers.bro.BasicBroParser",' +
            '"sensorTopic":"bro","parserConfig": {}}';

        component.onSensorParserConfigBlur();

        expect(component.newSensorParserConfig).toEqual(JSON.stringify(JSON.parse(component.newSensorParserConfig), null, '\t'));

        fixture.destroy();
    });

    it('should format enrichmentConfig', () => {
        component.newSensorEnrichmentConfig = '{"index": "bro","batchSize": 5,"enrichment" : {"fieldMap": ' +
            '{"geo": ["ip_dst_addr", "ip_src_addr"],"host": ["host"]}},"threatIntel": {"fieldMap": {"hbaseThreatIntel":' +
            ' ["ip_src_addr", "ip_dst_addr"]},"fieldToTypeMap": {"ip_src_addr" : ["malicious_ip"],"ip_dst_addr" : ["malicious_ip"]}}}';

        component.onSensorEnrichmentConfigBlur();

        expect(component.newSensorEnrichmentConfig).toEqual(JSON.stringify(JSON.parse(component.newSensorEnrichmentConfig), null, '\t'));

        fixture.destroy();
    });
});
