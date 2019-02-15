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
import { TestBed } from '@angular/core/testing';
import { StoreModule, Store, combineReducers, Action } from '@ngrx/store';
import { SensorsEffects } from './sensors.effects';
import { SensorsModule } from '../sensors.module';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import * as fromModule from '../reducers';
import { EffectsModule, Actions } from '@ngrx/effects';
import * as fromActions from '../actions';
import { ParserMetaInfoModel } from '../models/parser-meta-info.model';
import { ParserGroupModel } from '../models/parser-group.model';
import { ParserConfigModel } from '../models/parser-config.model';
import { SensorParserConfigService } from '../../service/sensor-parser-config.service';
import { Injectable } from '@angular/core';
import { of, throwError, Observable } from 'rxjs';
import { MetronAlerts } from '../../shared/metron-alerts';
import { RestError } from '../../model/rest-error';
import { StormService } from '../../service/storm.service';
import { cold, hot } from 'jasmine-marbles';
import { TopologyResponse } from '../../model/topology-response';
import { provideMockActions } from '@ngrx/effects/testing';
import { AppConfigService } from 'app/service/app-config.service';
import { MockAppConfigService } from 'app/service/mock.app-config.service';

@Injectable()
class FakeParserService {
  syncConfigs = jasmine.createSpy().and.returnValue(of({}));
  syncGroups = jasmine.createSpy().and.returnValue(of({}));
  getAllConfig = jasmine.createSpy().and.returnValue(of([]));
  getAllGroups = jasmine.createSpy().and.returnValue(of([]));
}

@Injectable()
class FakeMetronAlerts {
  showErrorMessage = jasmine.createSpy();
  showSuccessMessage = jasmine.createSpy();
}

@Injectable()
class FakeStormService {
  startParser = jasmine.createSpy().and.returnValue(of(new TopologyResponse()));
  stopParser = jasmine.createSpy().and.returnValue(of(new TopologyResponse()));
  activateParser = jasmine.createSpy().and.returnValue(of(new TopologyResponse()));
  deactivateParser = jasmine.createSpy().and.returnValue(of(new TopologyResponse()));
}

describe('sensor.effects.ts', () => {
  let store: Store<fromModule.State>;
  let service: FakeParserService;
  let userNotificationSvc: MetronAlerts;
  let effects: SensorsEffects;
  let testParsers: ParserMetaInfoModel[];
  let testGroups: ParserMetaInfoModel[];

  function fillStoreWithTestData() {
    testParsers = [
      { config: new ParserConfigModel('TestConfig01', { sensorTopic: 'TestKafkaTopicId01' })},
      { config: new ParserConfigModel('TestConfig01', { sensorTopic: 'TestKafkaTopicId02' })},
    ];
    testGroups = [
      { config: new ParserGroupModel({ name: 'TestGroup01', description: '' })},
      { config: new ParserGroupModel({ name: 'TestGroup02', description: '' })},
    ];

    store.dispatch(new fromActions.LoadSuccess({
      parsers: testParsers,
      groups: testGroups,
      statuses: []
    }));
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        SensorsModule,
        StoreModule.forRoot({ sensors: combineReducers(fromModule.reducers) }),
        EffectsModule.forRoot([]),
        HttpClientTestingModule
      ],
      providers: [
        SensorsEffects,
        HttpClient,
        { provide: AppConfigService, useClass: MockAppConfigService },
        { provide: SensorParserConfigService, useClass: FakeParserService },
        { provide: MetronAlerts, useClass: FakeMetronAlerts },
      ]
    });

    store = TestBed.get(Store);
    service = TestBed.get(SensorParserConfigService);
    userNotificationSvc = TestBed.get(MetronAlerts);
    effects = TestBed.get(SensorsEffects);

    fillStoreWithTestData();
  });

  it('Should pass state of parsers to service.syncConfigs() on action ApplyChanges', () => {
    store.dispatch(new fromActions.ApplyChanges());
    expect(service.syncConfigs).toHaveBeenCalledWith(testParsers);
  });

  it('Should pass state of groups to service.syncGroup() on action ApplyChanges', () => {
    store.dispatch(new fromActions.ApplyChanges());
    expect(service.syncGroups).toHaveBeenCalledWith(testGroups);
  });

  it('Should return with an LoadStart action when syncConfigs() and syncGroups() finished', () => {
    effects.applyChanges$.subscribe((result: Action) => {
      expect(result.type).toBeDefined(fromActions.SensorsActionTypes.LoadStart);
    });

    store.dispatch(new fromActions.ApplyChanges());
  });

  it('Should show notification when operation SUCCESSFULL', () => {
    store.dispatch(new fromActions.ApplyChanges());
    expect(userNotificationSvc.showSuccessMessage).toHaveBeenCalled();
  });

  it('Should show notification when operation FAILED', () => {
    service.syncConfigs = jasmine.createSpy().and.callFake(params => throwError(new RestError()));
    store.dispatch(new fromActions.ApplyChanges());
    expect(userNotificationSvc.showErrorMessage).toHaveBeenCalled();
  });
});

describe('sensors control operation effects', () => {
  let userNotificationSvc: MetronAlerts;
  let effects: SensorsEffects;
  let stormService: StormService;
  let actions$: Observable<any>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        SensorsModule,
        StoreModule.forRoot({ sensors: combineReducers(fromModule.reducers) }),
        EffectsModule.forRoot([]),
        HttpClientTestingModule
      ],
      providers: [
        SensorsEffects,
        HttpClient,
        { provide: AppConfigService, useClass: MockAppConfigService },
        { provide: MetronAlerts, useClass: FakeMetronAlerts },
        { provide: StormService, useClass: FakeStormService },
        provideMockActions(() => actions$),
      ]
    });

    userNotificationSvc = TestBed.get(MetronAlerts);
    effects = TestBed.get(SensorsEffects);
    stormService = TestBed.get(StormService);
    actions$ = TestBed.get(Actions);
  });

  it('startSensor$: dispatch success action with proper payload', () => {
    const parser = { config: new ParserConfigModel('foo', { sensorTopic: 'foo' }) };
    const action = new fromActions.StartSensor({ parser });
    const successAction = new fromActions.StartSensorSuccess({
      parser,
      status: 'SUCCESS'
    });
    actions$ = hot('-a-', { a: action });
    const expected = cold('-b', { b: successAction });
    expect(effects.startSensor$).toBeObservable(expected);
    expect(stormService.startParser).toHaveBeenCalledWith('foo');
    expect(userNotificationSvc.showSuccessMessage).toHaveBeenCalledWith(
      'Started sensor foo'
    )
  });

  it('startSensor$: dispatch failure action with proper payload if it throws', () => {
    const parser = { config: new ParserConfigModel('foo', { sensorTopic: 'foo' }) };
    const action = new fromActions.StartSensor({ parser });
    const error = new HttpErrorResponse({ error: new Error('some error') });
    const status = new TopologyResponse('ERROR', error.message);
    const failureAction = new fromActions.StartSensorFailure({
      parser,
      status: 'ERROR'
    });
    stormService.startParser = jasmine.createSpy().and.returnValue(of(error));
    actions$ = hot('-a--', { a: action });
    const expected = cold('-b', { b: failureAction });
    expect(effects.startSensor$).toBeObservable(expected);
    expect(userNotificationSvc.showErrorMessage).toHaveBeenCalledWith(
      'Unable to start sensor foo: ' + error.message
    )
  });

  it('startSensor$: dispatch failure action with proper payload if it is an error from the server', () => {
    const parser = { config: new ParserConfigModel('foo', { sensorTopic: 'foo' }) };
    const action = new fromActions.StartSensor({ parser });
    const status = new TopologyResponse('ERROR', 'some error from server');
    const failureAction = new fromActions.StartSensorFailure({
      parser,
      status: 'ERROR'
    });
    stormService.startParser = jasmine.createSpy().and.returnValue(of(status));
    actions$ = hot('-a--', { a: action });
    const expected = cold('-b', { b: failureAction });
    expect(effects.startSensor$).toBeObservable(expected);
    expect(userNotificationSvc.showErrorMessage).toHaveBeenCalledWith(
      'Unable to start sensor foo: some error from server'
    )
  });

  it('stopSensor$: dispatch success action with proper payload', () => {
    const parser = { config: new ParserConfigModel('foo', { sensorTopic: 'foo' }) };
    const action = new fromActions.StopSensor({ parser });
    const successAction = new fromActions.StopSensorSuccess({
      parser,
      status: 'SUCCESS'
    });
    actions$ = hot('-a-', { a: action });
    const expected = cold('-b', { b: successAction });
    expect(effects.stopSensor$).toBeObservable(expected);
    expect(stormService.stopParser).toHaveBeenCalledWith('foo');
    expect(userNotificationSvc.showSuccessMessage).toHaveBeenCalledWith(
      'Stopped sensor foo'
    )
  });

  it('stopSensor$: dispatch failure action with proper payload if it throws', () => {
    const parser = { config: new ParserConfigModel('foo', { sensorTopic: 'foo' }) };
    const action = new fromActions.StopSensor({ parser });
    const error = new HttpErrorResponse({ error: new Error('some error') });
    const status = new TopologyResponse('ERROR', error.message);
    const failureAction = new fromActions.StopSensorFailure({
      parser,
      status: 'ERROR'
    });
    stormService.stopParser = jasmine.createSpy().and.returnValue(of(error));
    actions$ = hot('-a--', { a: action });
    const expected = cold('-b', { b: failureAction });
    expect(effects.stopSensor$).toBeObservable(expected);
    expect(userNotificationSvc.showErrorMessage).toHaveBeenCalledWith(
      'Unable to stop sensor foo: ' + error.message
    )
  });

  it('stopSensor$: dispatch failure action with proper payload if it is an error from the server', () => {
    const parser = { config: new ParserConfigModel('foo', { sensorTopic: 'foo' }) };
    const action = new fromActions.StopSensor({ parser });
    const status = new TopologyResponse('ERROR', 'some error from server');
    const failureAction = new fromActions.StopSensorFailure({
      parser,
      status: 'ERROR'
    });
    stormService.stopParser = jasmine.createSpy().and.returnValue(of(status));
    actions$ = hot('-a--', { a: action });
    const expected = cold('-b', { b: failureAction });
    expect(effects.stopSensor$).toBeObservable(expected);
    expect(userNotificationSvc.showErrorMessage).toHaveBeenCalledWith(
      'Unable to stop sensor foo: some error from server'
    )
  });

  it('enableSensor$: dispatch success action with proper payload', () => {
    const parser = { config: new ParserConfigModel('foo', { sensorTopic: 'foo' }) };
    const action = new fromActions.EnableSensor({ parser });
    const successAction = new fromActions.EnableSensorSuccess({
      parser,
      status: 'SUCCESS'
    });
    actions$ = hot('-a-', { a: action });
    const expected = cold('-b', { b: successAction });
    expect(effects.enableSensor$).toBeObservable(expected);
    expect(stormService.activateParser).toHaveBeenCalledWith('foo');
    expect(userNotificationSvc.showSuccessMessage).toHaveBeenCalledWith(
      'Enabled sensor foo'
    )
  });

  it('enableSensor$: dispatch failure action with proper payload if it throws', () => {
    const parser = { config: new ParserConfigModel('foo', { sensorTopic: 'foo' }) };
    const action = new fromActions.EnableSensor({ parser });
    const error = new HttpErrorResponse({ error: new Error('some error') });
    const status = new TopologyResponse('ERROR', error.message);
    const failureAction = new fromActions.EnableSensorFailure({
      parser,
      status: 'ERROR'
    });
    stormService.activateParser = jasmine.createSpy().and.returnValue(of(error));
    actions$ = hot('-a--', { a: action });
    const expected = cold('-b', { b: failureAction });
    expect(effects.enableSensor$).toBeObservable(expected);
    expect(userNotificationSvc.showErrorMessage).toHaveBeenCalledWith(
      'Unable to enable sensor foo: ' + error.message
    )
  });

  it('enableSensor$: dispatch failure action with proper payload if it is an error from the server', () => {
    const parser = { config: new ParserConfigModel('foo', { sensorTopic: 'foo' }) };
    const action = new fromActions.EnableSensor({ parser });
    const status = new TopologyResponse('ERROR', 'some error from server');
    const failureAction = new fromActions.EnableSensorFailure({
      parser,
      status: 'ERROR'
    });
    stormService.activateParser = jasmine.createSpy().and.returnValue(of(status));
    actions$ = hot('-a--', { a: action });
    const expected = cold('-b', { b: failureAction });
    expect(effects.enableSensor$).toBeObservable(expected);
    expect(userNotificationSvc.showErrorMessage).toHaveBeenCalledWith(
      'Unable to enable sensor foo: some error from server'
    )
  });

  it('disableSensor$: dispatch success action with proper payload', () => {
    const parser = { config: new ParserConfigModel('foo', { sensorTopic: 'foo' }) };
    const action = new fromActions.DisableSensor({ parser });
    const successAction = new fromActions.DisableSensorSuccess({
      parser,
      status: 'SUCCESS'
    });
    actions$ = hot('-a-', { a: action });
    const expected = cold('-b', { b: successAction });
    expect(effects.disableSensor$).toBeObservable(expected);
    expect(stormService.deactivateParser).toHaveBeenCalledWith('foo');
    expect(userNotificationSvc.showSuccessMessage).toHaveBeenCalledWith(
      'Disabled sensor foo'
    )
  });

  it('disableSensor$: dispatch failure action with proper payload if it throws', () => {
    const parser = { config: new ParserConfigModel('foo', { sensorTopic: 'foo' }) };
    const action = new fromActions.DisableSensor({ parser });
    const error = new HttpErrorResponse({ error: new Error('some error') });
    const status = new TopologyResponse('ERROR', error.message);
    const failureAction = new fromActions.DisableSensorFailure({
      parser,
      status: 'ERROR'
    });
    stormService.deactivateParser = jasmine.createSpy().and.returnValue(of(error));
    actions$ = hot('-a--', { a: action });
    const expected = cold('-b', { b: failureAction });
    expect(effects.disableSensor$).toBeObservable(expected);
    expect(userNotificationSvc.showErrorMessage).toHaveBeenCalledWith(
      'Unable to disable sensor foo: ' + error.message
    )
  });

  it('disableSensor$: dispatch failure action with proper payload if it is an error from the server', () => {
    const parser = { config: new ParserConfigModel('foo', { sensorTopic: 'foo' }) };
    const action = new fromActions.DisableSensor({ parser });
    const status = new TopologyResponse('ERROR', 'some error from server');
    const failureAction = new fromActions.DisableSensorFailure({
      parser,
      status: 'ERROR'
    });
    stormService.deactivateParser = jasmine.createSpy().and.returnValue(of(status));
    actions$ = hot('-a--', { a: action });
    const expected = cold('-b', { b: failureAction });
    expect(effects.disableSensor$).toBeObservable(expected);
    expect(userNotificationSvc.showErrorMessage).toHaveBeenCalledWith(
      'Unable to disable sensor foo: some error from server'
    )
  });
})
