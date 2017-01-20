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
import {SensorStellarModule} from './sensor-stellar.module';
import {SensorIndexingConfig} from '../../model/sensor-indexing-config';
import '../../rxjs-operators';

describe('Component: SensorStellarComponent', () => {

    let fixture: ComponentFixture<SensorStellarComponent>;
    let component: SensorStellarComponent;
    let sensorParserConfigString = '{"parserClassName":"org.apache.metron.parsers.bro.BasicBroParser","sensorTopic":"bro",' +
        '"parserConfig": {},"fieldTransformations":[]}';
    let sensorParserConfig: SensorParserConfig = new SensorParserConfig();
    sensorParserConfig.sensorTopic = 'bro';
    sensorParserConfig.parserClassName = 'org.apache.metron.parsers.bro.BasicBroParser';
    sensorParserConfig.parserConfig = {};

    let sensorParserConfigWithClassNameString = `{"parserClassName":"org.apache.metron.parsers.bro.BasicBroParser","sensorTopic":"bro", 
        "parserConfig": {},"fieldTransformations":[], "writerClassName": "org.example.writerClassName", 
        "errorWriterClassName": "org.example.errorWriterClassName", 
        "filterClassName": "org.example.filterClassName", "invalidWriterClassName": "org.example.invalidWriterClassName"}`;
    let sensorParserConfigWithClassName = Object.assign(new SensorParserConfig(), sensorParserConfig);
    sensorParserConfigWithClassName.writerClassName = 'org.example.writerClassName';
    sensorParserConfigWithClassName.errorWriterClassName = 'org.example.errorWriterClassName';
    sensorParserConfigWithClassName.filterClassName = 'org.example.filterClassName';
    sensorParserConfigWithClassName.invalidWriterClassName = 'org.example.invalidWriterClassName';

    let sensorEnrichmentConfigString = '{"index": "bro","batchSize": 5,"enrichment" : {"fieldMap": ' +
        '{"geo": ["ip_dst_addr", "ip_src_addr"],"host": ["host"]}},"threatIntel": {"fieldMap": {"hbaseThreatIntel":' +
        ' ["ip_src_addr", "ip_dst_addr"]},"fieldToTypeMap": {"ip_src_addr" : ["malicious_ip"],"ip_dst_addr" : ["malicious_ip"]}}}';
    let sensorEnrichmentConfig = new SensorEnrichmentConfig();
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

    let sensorEnrichmentConfigWithConfigString = `{"index": "bro","batchSize": 5, "configuration": "some-configuration", 
         "enrichment" : {"fieldMap": {"geo": ["ip_dst_addr", "ip_src_addr"],"host": ["host"]}},
         "threatIntel": {"fieldMap": {"hbaseThreatIntel":["ip_src_addr", "ip_dst_addr"]},
         "fieldToTypeMap": {"ip_src_addr" : ["malicious_ip"],"ip_dst_addr" : ["malicious_ip"]}}}`;
    let sensorEnrichmentConfigWithConfig = Object.assign(new SensorEnrichmentConfig(), sensorEnrichmentConfig);
    sensorEnrichmentConfigWithConfig.configuration = 'some-configuration';

    let sensorIndexingConfig = new SensorIndexingConfig();
    sensorIndexingConfig.index = 'bro';
    sensorIndexingConfig.batchSize = 5;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            imports: [SharedModule, SensorStellarModule],
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

        component.init();
        expect(component.newSensorParserConfig).toEqual(undefined);
        expect(component.newSensorEnrichmentConfig).toEqual(undefined);

        component.sensorParserConfig = sensorParserConfig;
        component.sensorEnrichmentConfig = sensorEnrichmentConfig;
        component.init();
        expect(component.newSensorParserConfig).toEqual(JSON.stringify(sensorParserConfig, null, '\t'));
        expect(component.newSensorEnrichmentConfig).toEqual(JSON.stringify(sensorEnrichmentConfig, null, '\t'));

        fixture.destroy();
    });

    it('should save the fields', () => {
        spyOn(component.hideStellar, 'emit');
        spyOn(component.onStellarChanged, 'emit');

        component.sensorParserConfig = new SensorParserConfig();
        component.sensorEnrichmentConfig = new SensorEnrichmentConfig();

        component.newSensorParserConfig = sensorParserConfigString;
        component.newSensorEnrichmentConfig = sensorEnrichmentConfigString;
        component.onSave();
        expect(component.sensorParserConfig).toEqual(sensorParserConfig);
        expect(component.sensorEnrichmentConfig).toEqual(sensorEnrichmentConfig);
        expect(component.hideStellar.emit).toHaveBeenCalled();
        expect(component.onStellarChanged.emit).toHaveBeenCalled();


        component.newSensorParserConfig = sensorParserConfigWithClassNameString;
        component.newSensorEnrichmentConfig = sensorEnrichmentConfigWithConfigString;
        component.onSave();
        expect(component.sensorParserConfig).toEqual(sensorParserConfigWithClassName);
        expect(component.sensorEnrichmentConfig).toEqual(sensorEnrichmentConfigWithConfig);
        expect(component.hideStellar.emit['calls'].count()).toEqual(2);
        expect(component.onStellarChanged.emit['calls'].count()).toEqual(2);

        fixture.destroy();
    });

    it('should hide panel', () => {
        spyOn(component.hideStellar, 'emit');

        component.onCancel();

        expect(component.hideStellar.emit).toHaveBeenCalled();

        fixture.destroy();
    });
});
