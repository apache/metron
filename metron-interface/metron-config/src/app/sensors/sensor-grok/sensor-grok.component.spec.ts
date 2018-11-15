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
import { SimpleChange } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { SensorParserConfigService } from '../../service/sensor-parser-config.service';
import { MetronAlerts } from '../../shared/metron-alerts';
import { KafkaService } from '../../service/kafka.service';
import { Observable, throwError } from 'rxjs';
import { ParseMessageRequest } from '../../model/parse-message-request';
import { SensorGrokComponent } from './sensor-grok.component';
import { GrokValidationService } from '../../service/grok-validation.service';
import { SensorGrokModule } from './sensor-grok.module';
import { SensorParserConfig } from '../../model/sensor-parser-config';

class MockSensorParserConfigService {
  private parsedMessage: string;

  public parseMessage(
    parseMessageRequest: ParseMessageRequest
  ): Observable<{}> {
    if (this.parsedMessage === 'ERROR') {
      return throwError({ _body: JSON.stringify({ abc: 'def' }) });
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
        BASE10NUM:
          '(?<![0-9.+-])(?>[+-]?(?:(?:[0-9]+(?:\\.[0-9]+)?)|(?:\\.[0-9]+)))',
        BASE16FLOAT:
          '\\b(?<![0-9A-Fa-f.])(?:[+-]?(?:0x)?(?:(?:[0-9A-Fa-f]+(?:\\.[0-9A-Fa-f]*)?)|(?:\\.[0-9A-Fa-f]+)))\\b',
        BASE16NUM: '(?<![0-9A-Fa-f])(?:[+-]?(?:0x)?(?:[0-9A-Fa-f]+))',
        CISCOMAC: '(?:(?:[A-Fa-f0-9]{4}\\.){2}[A-Fa-f0-9]{4})',
        COMMONMAC: '(?:(?:[A-Fa-f0-9]{2}:){5}[A-Fa-f0-9]{2})',
        DATA: '.*?'
      });
      observer.complete();
    });
  }
}

class MockKafkaService {}

describe('Component: SensorGrok', () => {
  let component: SensorGrokComponent;
  let grokValidationService: GrokValidationService;
  let fixture: ComponentFixture<SensorGrokComponent>;
  let sensorParserConfigService: MockSensorParserConfigService;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [SensorGrokModule],
      providers: [
        MetronAlerts,
        { provide: HttpClient },
        { provide: KafkaService, useClass: MockKafkaService },
        {
          provide: SensorParserConfigService,
          useClass: MockSensorParserConfigService
        },
        { provide: GrokValidationService, useClass: MockGrokValidationService }
      ]
    });
    fixture = TestBed.createComponent(SensorGrokComponent);
    component = fixture.componentInstance;
    sensorParserConfigService = TestBed.get(SensorParserConfigService);
    grokValidationService = TestBed.get(GrokValidationService);
  }));

  it('should create an instance', () => {
    expect(component).toBeDefined();
    fixture.destroy();
  });

  it('should handle ngOnInit', async(() => {
    component.ngOnInit();

    expect(Object.keys(component.grokFunctionList).length).toEqual(6);

    fixture.destroy();
  }));

  it('should handle ngOnChanges', async(() => {
    spyOn(component.sampleData, 'getNextSample');

    let changes = {
      showGrok: new SimpleChange(true, false, true)
    };
    component.ngOnChanges(changes);
    expect(component.sampleData.getNextSample['calls'].count()).toEqual(0);

    changes = {
      showGrok: new SimpleChange(false, true, false)
    };

    component.grokStatement =
      'STATEMENT_1 grok statement 1\nSTATEMENT_2 grok statement 2\n';
    component.patternLabel = 'STATEMENT_2';
    component.ngOnChanges(changes);
    expect(component.newGrokStatement).toEqual(
      'STATEMENT_1 grok statement 1\nSTATEMENT_2 grok statement 2\n'
    );
    expect(component.newPatternLabel).toEqual('STATEMENT_2');
    expect(component.availablePatternLabels).toEqual([
      'STATEMENT_1',
      'STATEMENT_2'
    ]);

    component.grokStatement = '';
    component.patternLabel = 'PATTERN_LABEL';
    component.ngOnChanges(changes);
    expect(component.newGrokStatement).toEqual('PATTERN_LABEL ');
    expect(component.newPatternLabel).toEqual('PATTERN_LABEL');
    expect(component.availablePatternLabels).toEqual(['PATTERN_LABEL']);

    expect(component.sampleData.getNextSample['calls'].count()).toEqual(2);

    fixture.destroy();
  }));

  it('should test grok statement validation', async(() => {
    let parsedMessage = {
      action: 'TCP_MISS',
      bytes: 337891,
      code: 200,
      elapsed: 415,
      ip_dst_addr: '207.109.73.154',
      ip_src_addr: '127.0.0.1',
      method: 'GET',
      timestamp: '1467011157.401',
      url: 'http://www.aliexpress.com/af/shoes.html?'
    };
    sensorParserConfigService.setParsedMessage(parsedMessage);

    let sampleData =
      '1467011157.401 415 127.0.0.1 TCP_MISS/200 337891 GET http://www.aliexpress.com/af/shoes.html? ' +
      '- DIRECT/207.109.73.154 text/html';
    let grokStatement =
      'SQUID_DELIMITED %{NUMBER:timestamp} %{INT:elapsed} %{IPV4:ip_src_addr} %{WORD:action}/%{NUMBER:code} ' +
      '%{NUMBER:bytes} %{WORD:method} %{NOTSPACE:url} - %{WORD:UNWANTED}/%{IPV4:ip_dst_addr} %{WORD:UNWANTED}/%{WORD:UNWANTED}';

    component.sensorParserConfig = new SensorParserConfig();
    component.sensorParserConfig.sensorTopic = 'squid';
    component.newGrokStatement = grokStatement;

    component.onSampleDataChanged('');
    expect(component.parsedMessage).toEqual({});
    expect(component.parsedMessageKeys).toEqual([]);

    component.onSampleDataChanged(sampleData);
    expect(component.parsedMessage).toEqual(parsedMessage);
    expect(component.parsedMessageKeys).toEqual([
      'action',
      'bytes',
      'code',
      'elapsed',
      'ip_dst_addr',
      'ip_src_addr',
      'method',
      'timestamp',
      'url'
    ]);

    sensorParserConfigService.setParsedMessage('ERROR');
    component.onTestGrokStatement();

    expect(component.parsedMessage).toEqual({});

    component.newGrokStatement = '';
    component.onTestGrokStatement();
    expect(component.parsedMessage).toEqual({});

    fixture.destroy();
  }));

  it('should call appropriate functions on save ', () => {
    spyOn(component.hideGrok, 'emit');
    spyOn(component.onSaveGrokStatement, 'emit');
    spyOn(component.onSavePatternLabel, 'emit');
    component.newGrokStatement = 'grok statement';
    component.newPatternLabel = 'PATTERN_LABEL';

    component.onSaveGrok();

    expect(component.onSaveGrokStatement.emit).toHaveBeenCalledWith(
      'grok statement'
    );
    expect(component.onSavePatternLabel.emit).toHaveBeenCalledWith(
      'PATTERN_LABEL'
    );
    expect(component.hideGrok.emit).toHaveBeenCalled();
    fixture.destroy();
  });

  it('should call appropriate functions on cancel ', () => {
    spyOn(component.hideGrok, 'emit');
    spyOn(component.onSaveGrokStatement, 'emit');
    spyOn(component.onSavePatternLabel, 'emit');

    component.onCancelGrok();

    expect(component.onSaveGrokStatement.emit).not.toHaveBeenCalled();
    expect(component.onSavePatternLabel.emit).not.toHaveBeenCalled();
    expect(component.hideGrok.emit).toHaveBeenCalled();
    fixture.destroy();
  });

  it('should disable test', () => {
    expect(component.isTestDisabled()).toEqual(true);
    component.newGrokStatement = 'new grok statement';
    expect(component.isTestDisabled()).toEqual(true);
    component.parseMessageRequest.sampleData = 'sample data';
    expect(component.isTestDisabled()).toEqual(false);
  });

  it('should disable save', () => {
    component.availablePatternLabels = ['LABEL_1', 'LABEL_2'];
    expect(component.isSaveDisabled()).toEqual(true);
    component.newGrokStatement = 'new grok statement';
    expect(component.isSaveDisabled()).toEqual(true);
    component.newPatternLabel = 'LABEL_2';
    expect(component.isSaveDisabled()).toEqual(false);
  });
});
