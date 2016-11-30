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
/* tslint:disable:no-unused-variable */

import {async, TestBed, ComponentFixture} from '@angular/core/testing';
import {SensorStellarComponent} from './sensor-stellar.component';
import {SharedModule} from '../../shared/shared.module';
import {SimpleChanges, SimpleChange} from '@angular/core';
import {SensorParserConfig} from '../../model/sensor-parser-config';
import {SensorEnrichmentConfig} from '../../model/sensor-enrichment-config';
import {FieldTransformer} from '../../model/field-transformer';

describe('Component: SensorStellarComponent', () => {

    let fixture: ComponentFixture<SensorStellarComponent>;
    let component: SensorStellarComponent;

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
        let transforms =
        [
            Object.assign(new FieldTransformer(), {
                'input': [
                    'method'
                ],
                'output': null,
                'transformation': 'REMOVE',
                'config': {
                    'condition': 'exists(method) and method == "foo"'
                }
            }),
            Object.assign(new FieldTransformer(), {
                'input': [],
                'output': [
                    'method',
                    'status_code',
                    'url'
                ],
                'transformation': 'STELLAR',
                'config': {
                    'method': 'TO_UPPER(method)',
                    'status_code': 'TO_LOWER(code)',
                    'url': 'TO_STRING(TRIM(url))'
                }
            })
        ];
        let stellarconfig = {
            'numeric': {
                'foo': '1 + 1'
            },
            'ALL_CAPS': 'TO_UPPER(method)'
        };
        let triageConfig = {
            'riskLevelRules': {},
            'aggregator': 'MAX',
            'aggregationConfig': {}
        };

        component.sensorParserConfig = new SensorParserConfig();
        component.sensorEnrichmentConfig = new SensorEnrichmentConfig();
        component.sensorParserConfig.fieldTransformations = transforms;
        component.sensorEnrichmentConfig.enrichment.fieldMap['stellar'] = {'config': stellarconfig};
        component.sensorEnrichmentConfig.threatIntel.triageConfig = triageConfig;

        component.init();

        expect(component.transformationConfig).toEqual(JSON.stringify(transforms, null, '\t'));
        expect(component.enrichmentConfig).toEqual(JSON.stringify(stellarconfig, null, '\t'));
        expect(component.triageConfig).toEqual(JSON.stringify(triageConfig, null, '\t'));

        delete component.sensorEnrichmentConfig.enrichment.fieldMap['stellar'];

        component.init();

        expect(component.enrichmentConfig).toEqual('{}');

        fixture.destroy();
    });

    it('should save the fields', () => {
        spyOn(component.hideStellar, 'emit');
        spyOn(component.onStellarChanged, 'emit');

        let transforms =
        [
            Object.assign(new Object(), {
                'input': [
                    'method'
                ],
                'output': null,
                'transformation': 'REMOVE',
                'config': {
                    'condition': 'exists(method) and method == "foo"'
                }
            }),
            Object.assign(new Object(), {
                'input': [],
                'output': [
                    'method',
                    'status_code',
                    'url'
                ],
                'transformation': 'STELLAR',
                'config': {
                    'method': 'TO_UPPER(method)',
                    'status_code': 'TO_LOWER(code)',
                    'url': 'TO_STRING(TRIM(url))'
                }
            })
        ];
        let stellarconfig = {
            'numeric': {
                'foo': '1 + 1'
            },
            'ALL_CAPS': 'TO_UPPER(method)'
        };
        let triageConfig = {
            'riskLevelRules': {},
            'aggregator': 'MAX',
            'aggregationConfig': {}
        };

        component.sensorParserConfig = new SensorParserConfig();
        component.sensorEnrichmentConfig = new SensorEnrichmentConfig();

        component.transformationConfig = JSON.stringify(transforms);
        component.enrichmentConfig = JSON.stringify(stellarconfig);
        component.triageConfig = JSON.stringify(triageConfig);

        component.onSave();

        expect(component.sensorParserConfig.fieldTransformations).toEqual(transforms);
        expect(component.sensorEnrichmentConfig.enrichment.fieldMap['stellar']).toEqual({'config': stellarconfig});
        expect(component.sensorEnrichmentConfig.threatIntel.triageConfig).toEqual(triageConfig);

        expect(component.hideStellar.emit).toHaveBeenCalled();
        expect(component.onStellarChanged.emit).toHaveBeenCalled();

        fixture.destroy();
    });

    it('should save the fields', () => {
        spyOn(component, 'init');
        spyOn(component.hideStellar, 'emit');

        component.onCancel();

        expect(component.init).toHaveBeenCalled();
        expect(component.hideStellar.emit).toHaveBeenCalled();

        fixture.destroy();
    });

    it('should format transformationConfig', () => {
        component.transformationConfig = '[{"input":[],"output":' +
                        '["method","status_code","url"],"transformation":"STELLAR","config":' +
                        '{"method":"TO_UPPER(method)","status_code":"TO_LOWER(code)","url":"TO_STRING(TRIM(url))"}}]';

        component.onTransformationBlur();

        expect(component.transformationConfig).toEqual(JSON.stringify(JSON.parse(component.transformationConfig), null, '\t'));

        fixture.destroy();
    });

    it('should format enrichmentConfig', () => {
        component.enrichmentConfig = '[{"input":[],"output":' +
            '["method","status_code","url"],"transformation":"STELLAR","config":' +
            '{"method":"TO_UPPER(method)","status_code":"TO_LOWER(code)","url":"TO_STRING(TRIM(url))"}}]';

        component.onEnrichmentBlur();

        expect(component.enrichmentConfig).toEqual(JSON.stringify(JSON.parse(component.enrichmentConfig), null, '\t'));

        fixture.destroy();
    });

    it('should format triageConfig', () => {
        component.triageConfig = '[{"input":[],"output":' +
            '["method","status_code","url"],"transformation":"STELLAR","config":' +
            '{"method":"TO_UPPER(method)","status_code":"TO_LOWER(code)","url":"TO_STRING(TRIM(url))"}}]';

        component.onTriageBlur();

        expect(component.triageConfig).toEqual(JSON.stringify(JSON.parse(component.triageConfig), null, '\t'));

        fixture.destroy();
    });
});
