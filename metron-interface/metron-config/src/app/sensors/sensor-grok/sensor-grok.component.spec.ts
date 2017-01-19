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

import { TestBed, async, ComponentFixture } from '@angular/core/testing';
import {SimpleChange, SimpleChanges} from '@angular/core';
import {Http} from '@angular/http';
import {SensorParserConfigService} from '../../service/sensor-parser-config.service';
import {MetronAlerts} from '../../shared/metron-alerts';
import {KafkaService} from '../../service/kafka.service';
import {Observable} from 'rxjs/Observable';
import {ParseMessageRequest} from '../../model/parse-message-request';
import {SensorGrokComponent} from './sensor-grok.component';
import {GrokValidationService} from '../../service/grok-validation.service';
import {SensorGrokModule} from './sensor-grok.module';
import {SensorParserConfig} from '../../model/sensor-parser-config';
import '../../rxjs-operators';

class MockSensorParserConfigService {

  private parsedMessage: string;

  public parseMessage(parseMessageRequest: ParseMessageRequest): Observable<{}> {
    if (this.parsedMessage === 'ERROR') {
      return Observable.throw({'_body': JSON.stringify({'abc': 'def'}) });
    }

    return Observable.create(observer => {
      observer.next(this.parsedMessage);
      observer.complete();
    });
  }

  public setParsedMessage(parsedMessage: any) {
    this.parsedMessage = parsedMessage;
  }
}

class MockGrokValidationService {
  public list(): Observable<string[]> {
    return Observable.create(observer => {
      observer.next({
        'BASE10NUM': '(?<![0-9.+-])(?>[+-]?(?:(?:[0-9]+(?:\\.[0-9]+)?)|(?:\\.[0-9]+)))',
        'BASE16FLOAT': '\\b(?<![0-9A-Fa-f.])(?:[+-]?(?:0x)?(?:(?:[0-9A-Fa-f]+(?:\\.[0-9A-Fa-f]*)?)|(?:\\.[0-9A-Fa-f]+)))\\b',
        'BASE16NUM': '(?<![0-9A-Fa-f])(?:[+-]?(?:0x)?(?:[0-9A-Fa-f]+))',
        'CISCOMAC': '(?:(?:[A-Fa-f0-9]{4}\\.){2}[A-Fa-f0-9]{4})',
        'COMMONMAC': '(?:(?:[A-Fa-f0-9]{2}:){5}[A-Fa-f0-9]{2})',
        'DATA': '.*?'
      });
      observer.complete();
    });
  }
}

class MockKafkaService {

}

describe('Component: SensorFieldSchema', () => {
  let component: SensorGrokComponent;
  let grokValidationService: GrokValidationService;
  let fixture: ComponentFixture<SensorGrokComponent>;
  let sensorParserConfigService: MockSensorParserConfigService;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [SensorGrokModule],
      providers: [
        MetronAlerts,
        {provide: Http},
        {provide: KafkaService, useClass: MockKafkaService},
        {provide: SensorParserConfigService, useClass: MockSensorParserConfigService},
        {provide: GrokValidationService, useClass: MockGrokValidationService},

      ]
    }).compileComponents()
        .then(() => {
          fixture = TestBed.createComponent(SensorGrokComponent);
          component = fixture.componentInstance;
          sensorParserConfigService = fixture.debugElement.injector.get(SensorParserConfigService);
          grokValidationService = fixture.debugElement.injector.get(GrokValidationService);
        });
  }));

  it('should create an instance', () => {
    expect(component).toBeDefined();
    expect(component.autocompleteStatementGenerator).toBeDefined();
    fixture.destroy();
  });

  it('should create edit forms for SensorParserConfigComponent', async(() => {
    component.ngOnInit();

    expect(Object.keys(component.grokFunctionList).length).toEqual(6);

    fixture.destroy();
  }));

  it('should test grok statement validation', async(() => {
    let parsedMessage = {
      'action': 'TCP_MISS',
      'bytes': 337891,
      'code': 200,
      'elapsed': 415,
      'ip_dst_addr': '207.109.73.154',
      'ip_src_addr': '127.0.0.1',
      'method': 'GET',
      'timestamp': '1467011157.401',
      'url': 'http://www.aliexpress.com/af/shoes.html?'
    };
    sensorParserConfigService.setParsedMessage(parsedMessage);

    let sampleData = '1467011157.401 415 127.0.0.1 TCP_MISS/200 337891 GET http://www.aliexpress.com/af/shoes.html? ' +
      '- DIRECT/207.109.73.154 text/html';
    let grokStatement = '%{NUMBER:timestamp} %{INT:elapsed} %{IPV4:ip_src_addr} %{WORD:action}/%{NUMBER:code} %{NUMBER:bytes} ' +
      '%{WORD:method} %{NOTSPACE:url} - %{WORD:UNWANTED}\/%{IPV4:ip_dst_addr} %{WORD:UNWANTED}\/%{WORD:UNWANTED}';

    component.sensorParserConfig = new SensorParserConfig();
    component.sensorParserConfig.sensorTopic = 'squid';
    component.parseMessageRequest.sampleData = sampleData;
    component.grokStatement = grokStatement;

    component.onTestGrokStatement();

    expect(component.parseMessageRequest.sensorParserConfig.parserConfig['grokStatement']).toEqual(grokStatement);
    expect(component.parsedMessage).toEqual(parsedMessage);

    sensorParserConfigService.setParsedMessage('ERROR');
    component.onTestGrokStatement();
    expect(component.parsedMessage).toEqual({'abc': 'def'});

    component.grokStatement = '';
    component.onTestGrokStatement();
    expect(component.parsedMessage).toEqual({});

    fixture.destroy();
  }));


  it('should test prepareGrokStatement', async(() => {

    component.sensorParserConfig = new SensorParserConfig();
    component.sensorParserConfig.parserConfig = {};

    component.prepareGrokStatement();
    expect(component.grokStatement).toEqual('');

    component.sensorParserConfig.parserConfig['grokStatement'] = 'REMOVETHIS %{key:value} %{key:value} %{key:value}';
    component.prepareGrokStatement();
    expect(component.grokStatement).toEqual('%{key:value} %{key:value} %{key:value}');


    component.sensorParserConfig.parserConfig['grokStatement'] = '%{key:value} %{key:value} %{key:value}';
    component.prepareGrokStatement();
    expect(component.grokStatement).toEqual('%{key:value} %{key:value} %{key:value}');
  }));

  it('should call getSampleData if showGrok', () => {
    spyOn(component.sampleData, 'getNextSample');
    spyOn(component, 'prepareGrokStatement');

    let changes: SimpleChanges = {
      'showGrok': new SimpleChange(false, true),
      'sensorParserConfig': new SimpleChange(null, null)
    };
    component.ngOnChanges(changes);
    expect(component.sampleData.getNextSample['calls'].count()).toEqual(1);
    expect(component.prepareGrokStatement['calls'].count()).toEqual(1);

    changes = {
      'showGrok': new SimpleChange(true, false),
      'sensorParserConfig': new SimpleChange(null, new SensorParserConfig())
    };
    component.ngOnChanges(changes);
    expect(component.sampleData.getNextSample['calls'].count()).toEqual(1);
    expect(component.prepareGrokStatement['calls'].count()).toEqual(2);

    fixture.destroy();
  });

  it('should call onTestGrokStatement on calling onSampleDataChanged  ', () => {
    spyOn(component, 'onTestGrokStatement');

    component.onSampleDataChanged('Some sample data');

    expect(component.parseMessageRequest.sampleData).toEqual('Some sample data');
    expect(component.onTestGrokStatement).toHaveBeenCalled();

    fixture.destroy();
  });

  it('should return keys of parsed message  ', () => {
    component.grokStatement = 'sample statement';
    component.sensorParserConfig = new SensorParserConfig();
    component.sensorParserConfig.sensorTopic = 'abc';
    sensorParserConfigService.setParsedMessage({'def': 'test-agin', 'abc': 'test'});

    component.onTestGrokStatement();
    expect(component.parsedMessageKeys).toEqual(['abc', 'def']);
    expect(component.parseMessageRequest.sensorParserConfig.parserConfig['patternLabel']).toEqual('ABC');

    sensorParserConfigService.setParsedMessage({});
    component.onTestGrokStatement();
    expect(component.parsedMessageKeys).toEqual([]);
    expect(component.parseMessageRequest.sensorParserConfig.parserConfig['patternLabel']).toEqual('ABC');

    sensorParserConfigService.setParsedMessage(null);
    component.onTestGrokStatement();
    expect(component.parsedMessageKeys).toEqual([]);
    expect(component.parseMessageRequest.sensorParserConfig.parserConfig['patternLabel']).toEqual('ABC');

    component.sensorParserConfig.parserConfig['patternLabel'] = 'def';
    sensorParserConfigService.setParsedMessage('ERROR');
    component.onTestGrokStatement();
    expect(component.parsedMessageKeys).toEqual(['abc']);
    expect(component.parseMessageRequest.sensorParserConfig.parserConfig['patternLabel']).toEqual('def');

    fixture.destroy();
  });

  it('should call apprprate functions on save ', () => {
    spyOn(component.hideGrok, 'emit');
    component.showGrok = true;
    component.grokStatement = 'test sample';
    component.sensorParserConfig = new SensorParserConfig();
    component.sensorParserConfig.sensorTopic = 'abc';
    component.onSaveGrok();


    expect(component.showGrok).toEqual(false);
    expect(component.sensorParserConfig.parserConfig['grokStatement']).toEqual('test sample');
    expect(component.hideGrok.emit).toHaveBeenCalled();
    fixture.destroy();
  });

  it('should call apprprate functions on cancel ', () => {
    spyOn(component.hideGrok, 'emit');
    component.onCancelGrok();

    expect(component.hideGrok.emit).toHaveBeenCalled();
    fixture.destroy();
  });

});
