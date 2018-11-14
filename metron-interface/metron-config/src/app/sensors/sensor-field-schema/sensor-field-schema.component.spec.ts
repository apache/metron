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
/* tslint:disable:max-line-length */

import { TestBed, async, ComponentFixture } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { SimpleChanges, SimpleChange } from '@angular/core';
import { SensorParserConfigService } from '../../service/sensor-parser-config.service';
import { StellarService } from '../../service/stellar.service';
import { MetronAlerts } from '../../shared/metron-alerts';
import { SensorFieldSchemaModule } from './sensor-field-schema.module';
import {
  SensorFieldSchemaComponent,
  FieldSchemaRow
} from './sensor-field-schema.component';
import { KafkaService } from '../../service/kafka.service';
import { Observable, throwError } from 'rxjs';
import { StellarFunctionDescription } from '../../model/stellar-function-description';
import { SensorParserConfig } from '../../model/sensor-parser-config';
import {
  SensorEnrichmentConfig,
  EnrichmentConfig,
  ThreatIntelConfig
} from '../../model/sensor-enrichment-config';
import { ParseMessageRequest } from '../../model/parse-message-request';
import { AutocompleteOption } from '../../model/autocomplete-option';
import { FieldTransformer } from '../../model/field-transformer';
import { SensorEnrichmentConfigService } from '../../service/sensor-enrichment-config.service';

class MockSensorParserConfigService {
  parseMessage(parseMessageRequest: ParseMessageRequest): Observable<{}> {
    let parsedJson = {
      elapsed: 415,
      code: 200,
      ip_dst_addr: '207.109.73.154',
      original_string:
        '1467011157.401 415 127.0.0.1 TCP_MISS/200 337891 GET http://www.aliexpress.com/',
      method: 'GET',
      bytes: 337891,
      action: 'TCP_MISS',
      ip_src_addr: '127.0.0.1',
      url: 'http://www.aliexpress.com/af/shoes.html?',
      timestamp: '1467011157.401'
    };
    return Observable.create(observable => {
      observable.next(parsedJson);
      observable.complete();
    });
  }
}

class MockTransformationValidationService {
  public listSimpleFunctions(): Observable<StellarFunctionDescription[]> {
    let stellarFunctionDescription: StellarFunctionDescription[] = [];
    stellarFunctionDescription.push(
      new StellarFunctionDescription('TO_LOWER', 'TO_LOWER description', [
        'input - input field'
      ])
    );
    stellarFunctionDescription.push(
      new StellarFunctionDescription('TO_UPPER', 'TO_UPPER description', [
        'input - input field'
      ])
    );
    stellarFunctionDescription.push(
      new StellarFunctionDescription('TRIM', 'Lazy to copy desc', [
        'input - input field'
      ])
    );
    return Observable.create(observer => {
      observer.next(stellarFunctionDescription);
      observer.complete();
    });
  }
}

class MockSensorEnrichmentConfigService {
  public getAvailableEnrichments(): Observable<string[]> {
    return Observable.create(observer => {
      observer.next(['geo', 'host', 'whois']);
      observer.complete();
    });
  }
}

class MockKafkaService {}

describe('Component: SensorFieldSchema', () => {
  let component: SensorFieldSchemaComponent;
  let sensorEnrichmentConfigService: SensorEnrichmentConfigService;
  let sensorParserConfigService: SensorParserConfigService;
  let fixture: ComponentFixture<SensorFieldSchemaComponent>;
  let transformationValidationService: StellarService;

  let squidSensorConfigJson = {
    parserClassName: 'org.apache.metron.parsers.GrokParser',
    sensorTopic: 'squid',
    parserConfig: {
      grokPath: 'target/patterns/squid',
      grokStatement:
        '%{NUMBER:timestamp} %{INT:elapsed} %{IPV4:ip_src_addr} %{WORD:action}/%{NUMBER:code} ' +
        '%{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url} - %{WORD:UNWANTED}\\/%{IPV4:ip_dst_addr} ' +
        '%{WORD:UNWANTED}\\/%{WORD:UNWANTED}'
    },
    fieldTransformations: [
      {
        input: [],
        output: ['method'],
        transformation: 'STELLAR',
        config: {
          method: 'TRIM(TO_LOWER(method))'
        }
      },
      {
        input: ['code'],
        output: null,
        transformation: 'REMOVE',
        config: {
          condition: 'exists(field2)'
        }
      },
      {
        input: ['ip_src_addr'],
        output: null,
        transformation: 'REMOVE'
      }
    ]
  };
  let squidEnrichmentJson = {
    index: 'squid',
    batchSize: 1,
    enrichment: {
      fieldMap: {
        geo: ['ip_dst_addr', 'ip_src_addr'],
        host: ['ip_dst_addr'],
        whois: ['ip_src_addr']
      },
      fieldToTypeMap: {},
      config: {}
    },
    threatIntel: {
      fieldMap: {
        hbaseThreatIntel: ['ip_dst_addr']
      },
      fieldToTypeMap: {
        ip_dst_addr: ['malicious_ip']
      },
      config: {},
      triageConfig: {
        riskLevelRules: {},
        aggregator: 'MAX',
        aggregationConfig: {}
      }
    },
    configuration: {}
  };
  let sensorParserConfig = Object.assign(
    new SensorParserConfig(),
    squidSensorConfigJson
  );
  let sensorEnrichmentConfig = Object.assign(
    new SensorEnrichmentConfig(),
    squidEnrichmentJson
  );

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [SensorFieldSchemaModule],
      providers: [
        MetronAlerts,
        { provide: HttpClient },
        { provide: KafkaService, useClass: MockKafkaService },
        {
          provide: SensorEnrichmentConfigService,
          useClass: MockSensorEnrichmentConfigService
        },
        {
          provide: SensorParserConfigService,
          useClass: MockSensorParserConfigService
        },
        {
          provide: StellarService,
          useClass: MockTransformationValidationService
        }
      ]
    });
    fixture = TestBed.createComponent(SensorFieldSchemaComponent);
    component = fixture.componentInstance;
    sensorParserConfigService = TestBed.get(
        SensorParserConfigService
    );
    transformationValidationService = TestBed.get(
        StellarService
    );
    sensorEnrichmentConfigService = TestBed.get(
        SensorEnrichmentConfigService
    );
  }));

  it('should create an instance', () => {
    expect(component).toBeDefined();
    fixture.destroy();
  });

  it('should read TransformFunctions, EnrichmentFunctions, ThreatIntelfunctions', () => {
    component.ngOnInit();

    expect(component.transformOptions.length).toEqual(3);
    expect(Object.keys(component.transformFunctions).length).toEqual(3);
    expect(component.enrichmentOptions.length).toEqual(3);
    expect(component.threatIntelOptions.length).toEqual(1);

    fixture.destroy();
  });

  it('should call getSampleData if showFieldSchema', () => {
    spyOn(component.sampleData, 'getNextSample');

    let changes: SimpleChanges = {
      showFieldSchema: new SimpleChange(false, true, true)
    };
    component.ngOnChanges(changes);
    expect(component.sampleData.getNextSample['calls'].count()).toEqual(1);

    changes = {
      showFieldSchema: new SimpleChange(true, false, false)
    };
    component.ngOnChanges(changes);
    expect(component.sampleData.getNextSample['calls'].count()).toEqual(1);

    fixture.destroy();
  });

  it('should return isSimple function', () => {
    component.ngOnInit();

    expect(component.isSimpleFunction(['TO_LOWER', 'TO_UPPER'])).toEqual(true);
    expect(
      component.isSimpleFunction(['TO_LOWER', 'TO_UPPER', 'TEST'])
    ).toEqual(false);

    fixture.destroy();
  });

  it('should create FieldSchemaRows', () => {
    component.ngOnInit();

    component.sensorParserConfig = sensorParserConfig;
    component.sensorEnrichmentConfig = sensorEnrichmentConfig;
    component.onSampleDataChanged('DoctorStrange');
    component.createFieldSchemaRows();

    expect(component.fieldSchemaRows.length).toEqual(10);
    expect(component.savedFieldSchemaRows.length).toEqual(10);

    let methodFieldSchemaRow: FieldSchemaRow = component.fieldSchemaRows.filter(
      row => row.inputFieldName === 'method'
    )[0];
    expect(methodFieldSchemaRow).toBeDefined();
    expect(methodFieldSchemaRow.transformConfigured.length).toEqual(2);
    expect(methodFieldSchemaRow.enrichmentConfigured.length).toEqual(0);
    expect(methodFieldSchemaRow.threatIntelConfigured.length).toEqual(0);

    let ipSrcAddrFieldSchemaRow: FieldSchemaRow = component.fieldSchemaRows.filter(
      row => row.inputFieldName === 'ip_src_addr'
    )[0];
    expect(ipSrcAddrFieldSchemaRow).toBeDefined();
    expect(ipSrcAddrFieldSchemaRow.transformConfigured.length).toEqual(0);
    expect(ipSrcAddrFieldSchemaRow.enrichmentConfigured.length).toEqual(2);
    expect(ipSrcAddrFieldSchemaRow.threatIntelConfigured.length).toEqual(0);

    let ipDstAddrFieldSchemaRow: FieldSchemaRow = component.fieldSchemaRows.filter(
      row => row.inputFieldName === 'ip_dst_addr'
    )[0];
    expect(ipDstAddrFieldSchemaRow).toBeDefined();
    expect(ipDstAddrFieldSchemaRow.transformConfigured.length).toEqual(0);
    expect(ipDstAddrFieldSchemaRow.enrichmentConfigured.length).toEqual(2);
    expect(ipDstAddrFieldSchemaRow.threatIntelConfigured.length).toEqual(1);

    let codeSchemaRow: FieldSchemaRow = component.fieldSchemaRows.filter(
      row => row.inputFieldName === 'code'
    )[0];
    expect(codeSchemaRow).toBeDefined();
    expect(codeSchemaRow.isRemoved).toEqual(true);
    expect(codeSchemaRow.conditionalRemove).toEqual(true);
    expect(codeSchemaRow.transformConfigured.length).toEqual(0);
    expect(codeSchemaRow.enrichmentConfigured.length).toEqual(0);
    expect(codeSchemaRow.threatIntelConfigured.length).toEqual(0);

    fixture.destroy();
  });

  it('should  return getChanges', () => {
    let fieldSchemaRow = new FieldSchemaRow('method');
    fieldSchemaRow.transformConfigured = [];
    fieldSchemaRow.enrichmentConfigured = [
      new AutocompleteOption('GEO'),
      new AutocompleteOption('WHOIS')
    ];
    fieldSchemaRow.threatIntelConfigured = [
      new AutocompleteOption('MALICIOUS-IP')
    ];

    expect(component.getChanges(fieldSchemaRow)).toEqual(
      'Enrichments: GEO, WHOIS <br> Threat Intel: MALICIOUS-IP'
    );

    fieldSchemaRow.transformConfigured = [new AutocompleteOption('TO_STRING')];
    fieldSchemaRow.enrichmentConfigured = [new AutocompleteOption('GEO')];
    fieldSchemaRow.threatIntelConfigured = [
      new AutocompleteOption('MALICIOUS-IP'),
      new AutocompleteOption('MALICIOUS-IP')
    ];

    expect(component.getChanges(fieldSchemaRow)).toEqual(
      'Transforms: TO_STRING(method) <br> Enrichments: GEO <br> Threat Intel: MALICIOUS-IP, MALICIOUS-IP'
    );

    fieldSchemaRow.transformConfigured = [
      new AutocompleteOption('TO_STRING'),
      new AutocompleteOption('TO_STRING')
    ];
    fieldSchemaRow.enrichmentConfigured = [];
    fieldSchemaRow.threatIntelConfigured = [
      new AutocompleteOption('MALICIOUS-IP'),
      new AutocompleteOption('MALICIOUS-IP')
    ];

    expect(component.getChanges(fieldSchemaRow)).toEqual(
      'Transforms: TO_STRING(TO_STRING(method)) <br> Threat Intel: MALICIOUS-IP, MALICIOUS-IP'
    );

    fieldSchemaRow.transformConfigured = [
      new AutocompleteOption('TO_STRING'),
      new AutocompleteOption('TO_STRING')
    ];
    fieldSchemaRow.enrichmentConfigured = [];
    fieldSchemaRow.threatIntelConfigured = [];

    expect(component.getChanges(fieldSchemaRow)).toEqual(
      'Transforms: TO_STRING(TO_STRING(method)) <br> '
    );

    fieldSchemaRow.transformConfigured = [
      new AutocompleteOption('TO_STRING'),
      new AutocompleteOption('TO_STRING')
    ];
    fieldSchemaRow.isRemoved = true;
    expect(component.getChanges(fieldSchemaRow)).toEqual('Disabled');

    fixture.destroy();
  });

  it('should call appropriate functions when onSampleDataChanged is called ', () => {
    let returnSuccess = true;
    spyOn(component, 'createFieldSchemaRows');
    spyOn(component, 'onSampleDataNotAvailable');
    spyOn(sensorParserConfigService, 'parseMessage').and.callFake(function(
      parseMessageRequest: ParseMessageRequest
    ) {
      expect(
        parseMessageRequest.sensorParserConfig.parserConfig['patternLabel']
      ).toEqual(
        parseMessageRequest.sensorParserConfig.sensorTopic.toUpperCase()
      );
      expect(
        parseMessageRequest.sensorParserConfig.parserConfig['grokPath']
      ).toEqual('./' + parseMessageRequest.sensorParserConfig.sensorTopic);
      if (returnSuccess) {
        return Observable.create(observer => {
          observer.next({ a: 'b', c: 'd' });
          observer.complete();
        });
      }
      return throwError('Error');
    });

    component.sensorParserConfig = sensorParserConfig;
    component.sensorParserConfig.parserConfig['patternLabel'] = null;
    component.onSampleDataChanged('DoctorStrange');
    expect(component.parserResult).toEqual({ a: 'b', c: 'd' });
    expect(component.createFieldSchemaRows).toHaveBeenCalled();
    expect(component.onSampleDataNotAvailable).not.toHaveBeenCalled();

    returnSuccess = false;
    component.parserResult = {};
    component.onSampleDataChanged('DoctorStrange');
    expect(component.parserResult).toEqual({});
    expect(component.onSampleDataNotAvailable).toHaveBeenCalled();
    expect(component.onSampleDataNotAvailable['calls'].count()).toEqual(1);

    fixture.destroy();
  });

  it('should onSampleDataChanged available and onSampleDataNotAvailable ', () => {
    let returnSuccess = true;
    spyOn(component, 'createFieldSchemaRows');

    component.onSampleDataNotAvailable();
    expect(component.createFieldSchemaRows['calls'].count()).toEqual(1);

    fixture.destroy();
  });

  it('should call onSaveChange on onRemove/onEnable ', () => {
    spyOn(component, 'onSave');

    let fieldSchemaRow = new FieldSchemaRow('method');
    fieldSchemaRow.outputFieldName = 'copy-of-method';
    fieldSchemaRow.preview = 'TRIM(TO_LOWER(method))';
    fieldSchemaRow.isRemoved = false;

    component.savedFieldSchemaRows = [fieldSchemaRow];

    let removeFieldSchemaRow = JSON.parse(JSON.stringify(fieldSchemaRow));
    component.onRemove(removeFieldSchemaRow);
    expect(removeFieldSchemaRow.isRemoved).toEqual(true);
    expect(component.savedFieldSchemaRows[0].isRemoved).toEqual(true);
    expect(component.onSave['calls'].count()).toEqual(1);

    fieldSchemaRow.isRemoved = true;
    let enableFieldSchemaRow = JSON.parse(JSON.stringify(fieldSchemaRow));
    component.onEnable(enableFieldSchemaRow);
    expect(fieldSchemaRow.isRemoved).toEqual(false);
    expect(component.savedFieldSchemaRows[0].isRemoved).toEqual(false);
    expect(component.onSave['calls'].count()).toEqual(2);

    fixture.destroy();
  });

  it('should revert changes on cancel ', () => {
    let fieldSchemaRow = new FieldSchemaRow('method');
    fieldSchemaRow.showConfig = true;
    fieldSchemaRow.outputFieldName = 'method';
    fieldSchemaRow.preview = 'TRIM(TO_LOWER(method))';
    fieldSchemaRow.isRemoved = false;
    fieldSchemaRow.isSimple = true;
    fieldSchemaRow.transformConfigured = [
      new AutocompleteOption('TO_LOWER'),
      new AutocompleteOption('TRIM')
    ];

    component.savedFieldSchemaRows.push(fieldSchemaRow);

    component.onCancelChange(fieldSchemaRow);
    expect(fieldSchemaRow.showConfig).toEqual(false);

    component.hideFieldSchema.emit = jasmine.createSpy('emit');
    component.onCancel();
    expect(component.hideFieldSchema.emit).toHaveBeenCalled();

    fixture.destroy();
  });

  it('should return formatted function on createTransformFunction call ', () => {
    let fieldSchemaRow = new FieldSchemaRow('method');
    fieldSchemaRow.transformConfigured = [
      new AutocompleteOption('TRIM'),
      new AutocompleteOption('TO_STRING')
    ];

    expect(component.createTransformFunction(fieldSchemaRow)).toEqual(
      'TO_STRING(TRIM(method))'
    );

    fixture.destroy();
  });

  it('should set preview value for FieldSchemaRow ', () => {
    let fieldSchemaRow = new FieldSchemaRow('method');
    fieldSchemaRow.transformConfigured = [
      new AutocompleteOption('TRIM'),
      new AutocompleteOption('TO_STRING')
    ];

    component.onTransformsChange(fieldSchemaRow);
    expect(fieldSchemaRow.preview).toEqual('TO_STRING(TRIM(method))');

    fieldSchemaRow.transformConfigured = [new AutocompleteOption('TRIM')];
    component.onTransformsChange(fieldSchemaRow);
    expect(fieldSchemaRow.preview).toEqual('TRIM(method)');

    fieldSchemaRow.transformConfigured = [];
    component.onTransformsChange(fieldSchemaRow);
    expect(fieldSchemaRow.preview).toEqual('');

    fixture.destroy();
  });

  it('isConditionalRemoveTransform ', () => {
    let fieldTransformationJson = {
      input: ['method'],
      transformation: 'REMOVE',
      config: {
        condition: 'IS_DOMAIN(elapsed)'
      }
    };
    let simpleFieldTransformationJson = {
      input: ['method'],
      transformation: 'REMOVE'
    };
    let fieldTransformation: FieldTransformer = Object.assign(
      new FieldTransformer(),
      fieldTransformationJson
    );
    expect(component.isConditionalRemoveTransform(fieldTransformation)).toEqual(
      true
    );

    let simpleFieldTransformation: FieldTransformer = Object.assign(
      new FieldTransformer(),
      simpleFieldTransformationJson
    );
    expect(
      component.isConditionalRemoveTransform(simpleFieldTransformation)
    ).toEqual(false);

    fixture.destroy();
  });

  it('should save data ', () => {
    let methodFieldSchemaRow = new FieldSchemaRow('method');
    methodFieldSchemaRow.outputFieldName = 'method';
    methodFieldSchemaRow.preview = 'TRIM(TO_LOWER(method))';
    methodFieldSchemaRow.isRemoved = false;
    methodFieldSchemaRow.isSimple = true;
    methodFieldSchemaRow.transformConfigured = [
      new AutocompleteOption('TO_LOWER'),
      new AutocompleteOption('TRIM')
    ];

    let elapsedFieldSchemaRow = new FieldSchemaRow('elapsed');
    elapsedFieldSchemaRow.outputFieldName = 'elapsed';
    elapsedFieldSchemaRow.preview = 'IS_DOMAIN(elapsed)';
    elapsedFieldSchemaRow.isRemoved = true;
    elapsedFieldSchemaRow.isSimple = true;
    elapsedFieldSchemaRow.transformConfigured = [
      new AutocompleteOption('IS_DOMAIN')
    ];
    elapsedFieldSchemaRow.enrichmentConfigured = [
      new AutocompleteOption('host')
    ];

    let ipDstAddrFieldSchemaRow = new FieldSchemaRow('ip_dst_addr');
    ipDstAddrFieldSchemaRow.outputFieldName = 'ip_dst_addr';
    ipDstAddrFieldSchemaRow.preview = 'IS_DOMAIN(elapsed)';
    ipDstAddrFieldSchemaRow.isRemoved = false;
    ipDstAddrFieldSchemaRow.isSimple = false;
    ipDstAddrFieldSchemaRow.threatIntelConfigured = [
      new AutocompleteOption('malicious_ip')
    ];
    ipDstAddrFieldSchemaRow.enrichmentConfigured = [
      new AutocompleteOption('host')
    ];

    let codeFieldSchemaRow = new FieldSchemaRow('code');
    codeFieldSchemaRow.outputFieldName = 'code';
    codeFieldSchemaRow.isRemoved = true;
    codeFieldSchemaRow.conditionalRemove = true;

    component.savedFieldSchemaRows = [
      methodFieldSchemaRow,
      elapsedFieldSchemaRow,
      ipDstAddrFieldSchemaRow,
      codeFieldSchemaRow
    ];

    component.sensorParserConfig = new SensorParserConfig();
    component.sensorParserConfig.parserClassName =
      'org.apache.metron.parsers.GrokParser';
    component.sensorParserConfig.sensorTopic = 'squid';

    component.sensorParserConfig.fieldTransformations = [
      new FieldTransformer()
    ];
    component.sensorParserConfig.fieldTransformations[0].transformation =
      'REMOVE';
    component.sensorParserConfig.fieldTransformations[0].input = ['code'];
    component.sensorParserConfig.fieldTransformations[0].config = {
      condition: 'exists(method)'
    };

    component.sensorEnrichmentConfig = new SensorEnrichmentConfig();
    component.sensorEnrichmentConfig.enrichment = new EnrichmentConfig();
    component.sensorEnrichmentConfig.threatIntel = new ThreatIntelConfig();
    component.sensorEnrichmentConfig.configuration = {};

    component.onSave();

    let fieldTransformationJson = {
      output: ['method', 'elapsed'],
      transformation: 'STELLAR',
      config: {
        method: 'TRIM(TO_LOWER(method))',
        elapsed: 'IS_DOMAIN(elapsed)'
      }
    };

    let fieldTransformationRemoveJson = {
      input: ['elapsed'],
      transformation: 'REMOVE'
    };

    let conditionalFieldTransformationRemoveJson = {
      input: ['code'],
      transformation: 'REMOVE',
      config: {
        condition: 'exists(method)'
      }
    };

    let fieldTransformation = Object.assign(
      new FieldTransformer(),
      fieldTransformationJson
    );
    let fieldTransformationRemove = Object.assign(
      new FieldTransformer(),
      fieldTransformationRemoveJson
    );
    let conditionalFieldTransformationRemove = Object.assign(
      new FieldTransformer(),
      conditionalFieldTransformationRemoveJson
    );

    expect(component.sensorParserConfig.fieldTransformations.length).toEqual(3);
    let expectedStellar = component.sensorParserConfig.fieldTransformations.filter(
      transform => transform.transformation === 'STELLAR'
    )[0];
    let expectedRemove = component.sensorParserConfig.fieldTransformations.filter(
      transform => transform.transformation === 'REMOVE' && !transform.config
    )[0];
    let expectedConditionalRemove = component.sensorParserConfig.fieldTransformations.filter(
      transform => transform.transformation === 'REMOVE' && transform.config
    )[0];
    expect(expectedStellar).toEqual(fieldTransformation);
    expect(expectedRemove).toEqual(fieldTransformationRemove);
    expect(expectedConditionalRemove).toEqual(
      conditionalFieldTransformationRemove
    );

    fixture.destroy();
  });
});
