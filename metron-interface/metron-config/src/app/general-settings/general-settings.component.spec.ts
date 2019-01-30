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
import { Inject } from '@angular/core';
import { async, TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { GeneralSettingsComponent } from './general-settings.component';
import { MetronAlerts } from '../shared/metron-alerts';
import { MetronDialogBox } from '../shared/metron-dialog-box';
import { GlobalConfigService } from '../service/global-config.service';
import { GeneralSettingsModule } from './general-settings.module';
import { Observable, throwError } from 'rxjs';
import {AppConfigService} from '../service/app-config.service';
import {MockAppConfigService} from '../service/mock.app-config.service';

class MockGlobalConfigService extends GlobalConfigService {
  _config: any = {};
  _postSuccess = true;

  public post(globalConfig: {}): Observable<{}> {
    if (this._postSuccess) {
      return Observable.create(observer => {
        observer.next(globalConfig);
        observer.complete();
      });
    }

    return throwError('Error');
  }

  public get(): Observable<{}> {
    return Observable.create(observer => {
      observer.next(this._config);
      observer.complete();
    });
  }
}

describe('GeneralSettingsComponent', () => {
  let metronAlerts: MetronAlerts;
  let metronDialogBox: MetronDialogBox;
  let component: GeneralSettingsComponent;
  let globalConfigService: MockGlobalConfigService;
  let fixture: ComponentFixture<GeneralSettingsComponent>;
  let config = {
    'solr.collection': 'metron',
    'storm.indexingWorkers': 1,
    'storm.indexingExecutors': 2,
    'hdfs.boltBatchSize': 5000,
    'hdfs.boltFieldDelimiter': '|',
    'hdfs.boltFileRotationSize': 5,
    'hdfs.boltCompressionCodecClass':
      'org.apache.hadoop.io.compress.SnappyCodec',
    'hdfs.indexOutput': '/tmp/metron/enriched',
    'kafkaWriter.topic': 'outputTopic',
    'kafkaWriter.keySerializer':
      'org.apache.kafka.common.serialization.StringSerializer',
    'kafkaWriter.valueSerializer':
      'org.apache.kafka.common.serialization.StringSerializer',
    'kafkaWriter.requestRequiredAcks': 1,
    'solrWriter.indexName': 'alfaalfa',
    'solrWriter.shards': 1,
    'solrWriter.replicationFactor': 1,
    'solrWriter.batchSize': 50,
    fieldValidations: { field: 'validation' }
  };

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [GeneralSettingsModule],
      providers: [
        { provide: HttpClient },
        MetronAlerts,
        MetronDialogBox,
        { provide: GlobalConfigService, useClass: MockGlobalConfigService },
        { provide: AppConfigService, useClass: MockAppConfigService }
      ]
    });
    fixture = TestBed.createComponent(GeneralSettingsComponent);
    component = fixture.componentInstance;
    globalConfigService = TestBed.get(GlobalConfigService);
    metronAlerts = TestBed.get(MetronAlerts);
    metronDialogBox = TestBed.get(MetronDialogBox);
  }));

  it('can instantiate GeneralSettingsComponent', async(() => {
    expect(component instanceof GeneralSettingsComponent).toBe(true);
  }));

  it('should load global config', async(() => {
    globalConfigService._config = config;
    component.ngOnInit();

    expect(component.globalConfig).toEqual(config);
  }));

  it('should save global config', async(() => {
    globalConfigService._config = config;
    component.ngOnInit();
    fixture.detectChanges();
    spyOn(metronAlerts, 'showSuccessMessage');
    spyOn(metronAlerts, 'showErrorMessage');

    component.onSave();
    expect(metronAlerts.showSuccessMessage).toHaveBeenCalledWith(
      'Saved Global Settings'
    );

    globalConfigService._postSuccess = false;

    component.onSave();
    expect(metronAlerts.showErrorMessage).toHaveBeenCalledWith(
      'Unable to save Global Settings: Error'
    );
  }));

  it('should handle onCancel', async(() => {
    let dialogReturnTrue = true;
    let confirmationMsg =
      'Cancelling will revert all the changes made to the form. Do you wish to continue ?';

    spyOn(component, 'ngOnInit');
    spyOn(metronDialogBox, 'showConfirmationMessage').and.callFake(function() {
      return Observable.create(observer => {
        observer.next(dialogReturnTrue);
        observer.complete();
      });
    });

    component.onCancel();

    expect(component.ngOnInit).toHaveBeenCalled();
    expect(component.ngOnInit['calls'].count()).toEqual(1);
    expect(metronDialogBox.showConfirmationMessage['calls'].count()).toEqual(1);
    expect(metronDialogBox.showConfirmationMessage).toHaveBeenCalledWith(
      confirmationMsg
    );

    dialogReturnTrue = false;
    component.onCancel();

    expect(metronDialogBox.showConfirmationMessage['calls'].count()).toEqual(2);
    expect(component.ngOnInit['calls'].count()).toEqual(1);
  }));
});
