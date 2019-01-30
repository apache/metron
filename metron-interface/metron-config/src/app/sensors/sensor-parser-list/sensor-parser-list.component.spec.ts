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
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { SpyLocation } from '@angular/common/testing';
import { HttpClient, HttpResponse } from '@angular/common/http';

import { DebugElement, Inject } from '@angular/core';
import { By } from '@angular/platform-browser';
import { Router, NavigationStart } from '@angular/router';
import { Observable } from 'rxjs';
import { SensorParserListComponent } from './sensor-parser-list.component';
import { SensorParserConfigService } from '../../service/sensor-parser-config.service';
import { MetronAlerts } from '../../shared/metron-alerts';
import { TopologyStatus } from '../../model/topology-status';
import { SensorParserConfig } from '../../model/sensor-parser-config';
import { AuthenticationService } from '../../service/authentication.service';
import { SensorParserListModule } from './sensor-parser-list.module';
import { MetronDialogBox } from '../../shared/metron-dialog-box';
import { Sort } from '../../util/enums';
import 'jquery';
import { SensorParserConfigHistoryService } from '../../service/sensor-parser-config-history.service';
import { SensorParserConfigHistory } from '../../model/sensor-parser-config-history';
import { StormService } from '../../service/storm.service';
import {AppConfigService} from '../../service/app-config.service';
import {MockAppConfigService} from '../../service/mock.app-config.service';

class MockAuthenticationService extends AuthenticationService {
  public checkAuthentication() {}

  public getCurrentUser(options: {}): Observable<HttpResponse<{}>> {
    return Observable.create(observer => {
      observer.next(new HttpResponse({ body: 'test' }));
      observer.complete();
    });
  }
}

class MockSensorParserConfigHistoryService extends SensorParserConfigHistoryService {
  private allSensorParserConfigHistory: SensorParserConfigHistory[];

  public setSensorParserConfigHistoryForTest(
    allSensorParserConfigHistory: SensorParserConfigHistory[]
  ) {
    this.allSensorParserConfigHistory = allSensorParserConfigHistory;
  }

  public getAll(): Observable<SensorParserConfigHistory[]> {
    return Observable.create(observer => {
      observer.next(this.allSensorParserConfigHistory);
      observer.complete();
    });
  }
}

class MockSensorParserConfigService extends SensorParserConfigService {
  private sensorParserConfigs: {};

  public setSensorParserConfigForTest(sensorParserConfigs: {}) {
    this.sensorParserConfigs = sensorParserConfigs;
  }

  public getAll(): Observable<{ string: SensorParserConfig }> {
    return Observable.create(observer => {
      observer.next(this.sensorParserConfigs);
      observer.complete();
    });
  }

  public deleteSensorParserConfigs(
    sensorNames: string[]
  ): Observable<{ success: Array<string>; failure: Array<string> }> {
    let result: { success: Array<string>; failure: Array<string> } = {
      success: [],
      failure: []
    };
    let observable = Observable.create(observer => {
      for (let i = 0; i < sensorNames.length; i++) {
        result.success.push(sensorNames[i]);
      }
      observer.next(result);
      observer.complete();
    });
    return observable;
  }
}

class MockStormService extends StormService {
  private topologyStatuses: TopologyStatus[];

  public setTopologyStatusForTest(topologyStatuses: TopologyStatus[]) {
    this.topologyStatuses = topologyStatuses;
  }

  public pollGetAll(): Observable<TopologyStatus[]> {
    return Observable.create(observer => {
      observer.next(this.topologyStatuses);
      observer.complete();
    });
  }

  public getAll(): Observable<TopologyStatus[]> {
    return Observable.create(observer => {
      observer.next(this.topologyStatuses);
      observer.complete();
    });
  }
}

class MockRouter {
  events: Observable<Event> = Observable.create(observer => {
    observer.next(new NavigationStart(1, '/sensors'));
    observer.complete();
  });

  navigateByUrl(url: string) {}
}

class MockMetronDialogBox {
  public showConfirmationMessage(message: string) {
    return Observable.create(observer => {
      observer.next(true);
      observer.complete();
    });
  }
}

describe('Component: SensorParserList', () => {
  let comp: SensorParserListComponent;
  let fixture: ComponentFixture<SensorParserListComponent>;
  let authenticationService: MockAuthenticationService;
  let sensorParserConfigService: MockSensorParserConfigService;
  let stormService: MockStormService;
  let sensorParserConfigHistoryService: MockSensorParserConfigHistoryService;
  let router: Router;
  let metronAlerts: MetronAlerts;
  let metronDialog: MetronDialogBox;
  let dialogEl: DebugElement;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [SensorParserListModule],
      providers: [
        { provide: HttpClient },
        { provide: Location, useClass: SpyLocation },
        { provide: AuthenticationService, useClass: MockAuthenticationService },
        {
          provide: SensorParserConfigService,
          useClass: MockSensorParserConfigService
        },
        { provide: StormService, useClass: MockStormService },
        {
          provide: SensorParserConfigHistoryService,
          useClass: MockSensorParserConfigHistoryService
        },
        { provide: Router, useClass: MockRouter },
        { provide: MetronDialogBox, useClass: MockMetronDialogBox },
        { provide: AppConfigService, useClass: MockAppConfigService },
        MetronAlerts
      ]
    });
    fixture = TestBed.createComponent(SensorParserListComponent);
    comp = fixture.componentInstance;
    authenticationService = TestBed.get(AuthenticationService);
    sensorParserConfigService = TestBed.get(SensorParserConfigService);
    stormService = TestBed.get(StormService);
    sensorParserConfigHistoryService = TestBed.get(
      SensorParserConfigHistoryService
    );
    router = TestBed.get(Router);
    metronAlerts = TestBed.get(MetronAlerts);
    metronDialog = TestBed.get(MetronDialogBox);
    dialogEl = fixture.debugElement.query(By.css('.primary'));
  }));

  it('should create an instance', async(() => {
    let component: SensorParserListComponent = fixture.componentInstance;
    expect(component).toBeDefined();
    fixture.destroy();
  }));

  it('getSensors should call getStatus and poll status and all variables should be initialised', async(() => {
    let sensorParserConfigHistory1 = new SensorParserConfigHistory();
    let sensorParserConfigHistory2 = new SensorParserConfigHistory();
    let sensorParserConfig1 = new SensorParserConfig();
    let sensorParserConfig2 = new SensorParserConfig();

    sensorParserConfigHistory1.sensorName = 'squid';
    sensorParserConfigHistory2.sensorName = 'bro';
    sensorParserConfigHistory1.config = sensorParserConfig1;
    sensorParserConfigHistory2.config = sensorParserConfig2;

    let sensorParserStatus1 = new TopologyStatus();
    let sensorParserStatus2 = new TopologyStatus();
    sensorParserStatus1.name = 'squid';
    sensorParserStatus1.status = 'KILLED';
    sensorParserStatus2.name = 'bro';
    sensorParserStatus2.status = 'KILLED';

    sensorParserConfigService.setSensorParserConfigForTest({
      squid: sensorParserConfig1,
      bro: sensorParserConfig2
    });
    stormService.setTopologyStatusForTest([
      sensorParserStatus1,
      sensorParserStatus2
    ]);

    let component: SensorParserListComponent = fixture.componentInstance;

    component.enableAutoRefresh = false;

    component.ngOnInit();

    expect(component.sensors[0].sensorName).toEqual(
      sensorParserConfigHistory1.sensorName
    );
    expect(component.sensors[1].sensorName).toEqual(
      sensorParserConfigHistory2.sensorName
    );
    expect(component.sensorsStatus[0]).toEqual(
      Object.assign(new TopologyStatus(), sensorParserStatus1)
    );
    expect(component.sensorsStatus[1]).toEqual(
      Object.assign(new TopologyStatus(), sensorParserStatus2)
    );
    expect(component.selectedSensors).toEqual([]);
    expect(component.count).toEqual(2);

    fixture.destroy();
  }));

  it('getParserType should return the Type of Parser', async(() => {
    let component: SensorParserListComponent = fixture.componentInstance;

    let sensorParserConfig1 = new SensorParserConfig();
    sensorParserConfig1.sensorTopic = 'squid';
    sensorParserConfig1.parserClassName =
      'org.apache.metron.parsers.GrokParser';
    let sensorParserConfig2 = new SensorParserConfig();
    sensorParserConfig2.sensorTopic = 'bro';
    sensorParserConfig2.parserClassName =
      'org.apache.metron.parsers.bro.BasicBroParser';

    expect(component.getParserType(sensorParserConfig1)).toEqual('Grok');
    expect(component.getParserType(sensorParserConfig2)).toEqual('Bro');

    fixture.destroy();
  }));

  it('navigateToSensorEdit should set selected sensor and change url', async(() => {
    let event = new Event('mouse');
    event.stopPropagation = jasmine.createSpy('stopPropagation');

    spyOn(router, 'navigateByUrl');

    let component: SensorParserListComponent = fixture.componentInstance;

    let sensorParserConfigHistory1 = new SensorParserConfigHistory();
    sensorParserConfigHistory1.sensorName = 'squid';
    component.navigateToSensorEdit(sensorParserConfigHistory1, event);

    let expectStr = router.navigateByUrl['calls'].argsFor(0);
    expect(expectStr).toEqual(['/sensors(dialog:sensors-config/squid)']);

    expect(event.stopPropagation).toHaveBeenCalled();

    fixture.destroy();
  }));

  it('addAddSensor should change the URL', async(() => {
    spyOn(router, 'navigateByUrl');

    let component: SensorParserListComponent = fixture.componentInstance;

    component.addAddSensor();

    let expectStr = router.navigateByUrl['calls'].argsFor(0);
    expect(expectStr).toEqual(['/sensors(dialog:sensors-config/new)']);

    fixture.destroy();
  }));

  it('onRowSelected should add add/remove items from the selected stack', async(() => {
    let component: SensorParserListComponent = fixture.componentInstance;
    let event = { target: { checked: true } };

    let sensorParserConfigHistory = new SensorParserConfigHistory();
    let sensorParserConfig = new SensorParserConfig();

    sensorParserConfig.sensorTopic = 'squid';
    sensorParserConfigHistory.config = sensorParserConfig;

    component.onRowSelected(sensorParserConfigHistory, event);

    expect(component.selectedSensors[0]).toEqual(sensorParserConfigHistory);

    event = { target: { checked: false } };

    component.onRowSelected(sensorParserConfigHistory, event);
    expect(component.selectedSensors).toEqual([]);

    fixture.destroy();
  }));

  it('onSelectDeselectAll should populate items into selected stack', async(() => {
    let component: SensorParserListComponent = fixture.componentInstance;

    let sensorParserConfig1 = new SensorParserConfig();
    let sensorParserConfig2 = new SensorParserConfig();
    let sensorParserConfigHistory1 = new SensorParserConfigHistory();
    let sensorParserConfigHistory2 = new SensorParserConfigHistory();

    sensorParserConfig1.sensorTopic = 'squid';
    sensorParserConfigHistory1.config = sensorParserConfig1;
    sensorParserConfig2.sensorTopic = 'bro';
    sensorParserConfigHistory2.config = sensorParserConfig2;

    component.sensors.push(sensorParserConfigHistory1);
    component.sensors.push(sensorParserConfigHistory2);

    let event = { target: { checked: true } };

    component.onSelectDeselectAll(event);

    expect(component.selectedSensors).toEqual([
      sensorParserConfigHistory1,
      sensorParserConfigHistory2
    ]);

    event = { target: { checked: false } };

    component.onSelectDeselectAll(event);

    expect(component.selectedSensors).toEqual([]);

    fixture.destroy();
  }));

  it('onSensorRowSelect should change the url and updated the selected items stack', async(() => {
    let sensorParserConfigHistory1 = new SensorParserConfigHistory();
    sensorParserConfigHistory1.sensorName = 'squid';

    let component: SensorParserListComponent = fixture.componentInstance;
    let event = {
      target: { type: 'div', parentElement: { firstChild: { type: 'div' } } }
    };

    component.selectedSensor = sensorParserConfigHistory1;
    component.onSensorRowSelect(sensorParserConfigHistory1, event);

    expect(component.selectedSensor).toEqual(null);

    component.onSensorRowSelect(sensorParserConfigHistory1, event);

    expect(component.selectedSensor).toEqual(sensorParserConfigHistory1);

    component.selectedSensor = sensorParserConfigHistory1;
    event = {
      target: {
        type: 'checkbox',
        parentElement: { firstChild: { type: 'div' } }
      }
    };

    component.onSensorRowSelect(sensorParserConfigHistory1, event);

    expect(component.selectedSensor).toEqual(sensorParserConfigHistory1);

    fixture.destroy();
  }));

  it('onSensorRowSelect should change the url and updated the selected items stack', async(() => {
    let component: SensorParserListComponent = fixture.componentInstance;

    let sensorParserConfigHistory = new SensorParserConfigHistory();
    let sensorParserConfig = new SensorParserConfig();

    sensorParserConfig.sensorTopic = 'squid';
    sensorParserConfigHistory.config = sensorParserConfig;

    component.toggleStartStopInProgress(sensorParserConfigHistory);
    expect(sensorParserConfig['startStopInProgress']).toEqual(true);

    component.toggleStartStopInProgress(sensorParserConfigHistory);
    expect(sensorParserConfig['startStopInProgress']).toEqual(false);
  }));

  it('onDeleteSensor should call the appropriate url', async(() => {
    spyOn(metronAlerts, 'showSuccessMessage');
    spyOn(metronDialog, 'showConfirmationMessage').and.callThrough();

    let event = new Event('mouse');
    event.stopPropagation = jasmine.createSpy('stopPropagation');

    let component: SensorParserListComponent = fixture.componentInstance;
    let sensorParserConfigHistory1 = new SensorParserConfigHistory();
    let sensorParserConfigHistory2 = new SensorParserConfigHistory();
    let sensorParserConfig1 = new SensorParserConfig();
    let sensorParserConfig2 = new SensorParserConfig();

    sensorParserConfigHistory1.sensorName = 'squid';
    sensorParserConfigHistory2.sensorName = 'bro';
    sensorParserConfigHistory1.config = sensorParserConfig1;
    sensorParserConfigHistory2.config = sensorParserConfig2;

    component.selectedSensors.push(sensorParserConfigHistory1);
    component.selectedSensors.push(sensorParserConfigHistory2);

    component.onDeleteSensor();

    expect(metronAlerts.showSuccessMessage).toHaveBeenCalled();

    component.deleteSensor(event, [sensorParserConfigHistory1]);

    expect(metronDialog.showConfirmationMessage).toHaveBeenCalled();
    expect(metronDialog.showConfirmationMessage['calls'].count()).toEqual(2);
    expect(metronDialog.showConfirmationMessage['calls'].count()).toEqual(2);
    expect(metronDialog.showConfirmationMessage['calls'].all()[0].args).toEqual(
      ['Are you sure you want to delete sensor(s) squid, bro ?']
    );
    expect(metronDialog.showConfirmationMessage['calls'].all()[1].args).toEqual(
      ['Are you sure you want to delete sensor(s) squid ?']
    );

    expect(event.stopPropagation).toHaveBeenCalled();

    fixture.destroy();
  }));

  it('onStopSensor should call the appropriate url', async(() => {
    let event = new Event('mouse');
    event.stopPropagation = jasmine.createSpy('stopPropagation');

    let sensorParserConfigHistory1 = new SensorParserConfigHistory();
    let sensorParserConfig1 = new SensorParserConfig();

    sensorParserConfig1.sensorTopic = 'squid';
    sensorParserConfigHistory1.config = sensorParserConfig1;

    let observableToReturn = Observable.create(observer => {
      observer.next({ status: 'success', message: 'Some Message' });
      observer.complete();
    });

    spyOn(metronAlerts, 'showSuccessMessage');
    spyOn(stormService, 'stopParser').and.returnValue(observableToReturn);

    let component: SensorParserListComponent = fixture.componentInstance;

    component.onStopSensor(sensorParserConfigHistory1, event);

    expect(stormService.stopParser).toHaveBeenCalled();
    expect(event.stopPropagation).toHaveBeenCalled();
    expect(metronAlerts.showSuccessMessage).toHaveBeenCalled();

    fixture.destroy();
  }));

  it('onStartSensor should call the appropriate url', async(() => {
    let event = new Event('mouse');
    event.stopPropagation = jasmine.createSpy('stopPropagation');

    let sensorParserConfigHistory1 = new SensorParserConfigHistory();
    let sensorParserConfig1 = new SensorParserConfig();

    sensorParserConfig1.sensorTopic = 'squid';
    sensorParserConfigHistory1.config = sensorParserConfig1;

    let observableToReturn = Observable.create(observer => {
      observer.next({ status: 'success', message: 'Some Message' });
      observer.complete();
    });

    spyOn(metronAlerts, 'showSuccessMessage');
    spyOn(stormService, 'startParser').and.returnValue(observableToReturn);

    let component: SensorParserListComponent = fixture.componentInstance;

    component.onStartSensor(sensorParserConfigHistory1, event);

    expect(stormService.startParser).toHaveBeenCalled();
    expect(event.stopPropagation).toHaveBeenCalled();
    expect(metronAlerts.showSuccessMessage).toHaveBeenCalled();

    fixture.destroy();
  }));

  it('onEnableSensor should call the appropriate url', async(() => {
    let event = new Event('mouse');
    event.stopPropagation = jasmine.createSpy('stopPropagation');

    let sensorParserConfigHistory1 = new SensorParserConfigHistory();
    let sensorParserConfig1 = new SensorParserConfig();

    sensorParserConfig1.sensorTopic = 'squid';
    sensorParserConfigHistory1.config = sensorParserConfig1;

    let observableToReturn = Observable.create(observer => {
      observer.next({ status: 'success', message: 'Some Message' });
      observer.complete();
    });

    spyOn(metronAlerts, 'showSuccessMessage');
    spyOn(stormService, 'activateParser').and.returnValue(observableToReturn);

    let component: SensorParserListComponent = fixture.componentInstance;

    component.onEnableSensor(sensorParserConfigHistory1, event);

    expect(stormService.activateParser).toHaveBeenCalled();
    expect(event.stopPropagation).toHaveBeenCalled();
    expect(metronAlerts.showSuccessMessage).toHaveBeenCalled();

    fixture.destroy();
  }));

  it('onDisableSensor should call the appropriate url', async(() => {
    let event = new Event('mouse');
    event.stopPropagation = jasmine.createSpy('stopPropagation');

    let sensorParserConfigHistory1 = new SensorParserConfigHistory();
    let sensorParserConfig1 = new SensorParserConfig();

    sensorParserConfig1.sensorTopic = 'squid';
    sensorParserConfigHistory1.config = sensorParserConfig1;

    let observableToReturn = Observable.create(observer => {
      observer.next({ status: 'success', message: 'Some Message' });
      observer.complete();
    });

    spyOn(metronAlerts, 'showSuccessMessage');
    spyOn(stormService, 'deactivateParser').and.returnValue(observableToReturn);

    let component: SensorParserListComponent = fixture.componentInstance;

    component.onDisableSensor(sensorParserConfigHistory1, event);

    expect(stormService.deactivateParser).toHaveBeenCalled();
    expect(event.stopPropagation).toHaveBeenCalled();
    expect(metronAlerts.showSuccessMessage).toHaveBeenCalled();

    fixture.destroy();
  }));

  it(
    'onStartSensors/onStopSensors should call start on all sensors that have status != ' +
      'Running and status != Running respectively',
    async(() => {
      let component: SensorParserListComponent = fixture.componentInstance;

      spyOn(component, 'onStartSensor');
      spyOn(component, 'onStopSensor');
      spyOn(component, 'onDisableSensor');
      spyOn(component, 'onEnableSensor');

      let sensorParserConfigHistory1 = new SensorParserConfigHistory();
      let sensorParserConfigHistory2 = new SensorParserConfigHistory();
      let sensorParserConfigHistory3 = new SensorParserConfigHistory();
      let sensorParserConfigHistory4 = new SensorParserConfigHistory();
      let sensorParserConfigHistory5 = new SensorParserConfigHistory();
      let sensorParserConfigHistory6 = new SensorParserConfigHistory();
      let sensorParserConfigHistory7 = new SensorParserConfigHistory();

      let sensorParserConfig1 = new SensorParserConfig();
      let sensorParserConfig2 = new SensorParserConfig();
      let sensorParserConfig3 = new SensorParserConfig();
      let sensorParserConfig4 = new SensorParserConfig();
      let sensorParserConfig5 = new SensorParserConfig();
      let sensorParserConfig6 = new SensorParserConfig();
      let sensorParserConfig7 = new SensorParserConfig();

      sensorParserConfig1.sensorTopic = 'squid';
      sensorParserConfigHistory1['status'] = 'Running';
      sensorParserConfigHistory1.config = sensorParserConfig1;

      sensorParserConfig2.sensorTopic = 'bro';
      sensorParserConfigHistory2['status'] = 'Stopped';
      sensorParserConfigHistory2.config = sensorParserConfig2;

      sensorParserConfig3.sensorTopic = 'test';
      sensorParserConfigHistory3['status'] = 'Stopped';
      sensorParserConfigHistory3.config = sensorParserConfig3;

      sensorParserConfig4.sensorTopic = 'test1';
      sensorParserConfigHistory4['status'] = 'Stopped';
      sensorParserConfigHistory4.config = sensorParserConfig4;

      sensorParserConfig5.sensorTopic = 'test2';
      sensorParserConfigHistory5['status'] = 'Running';
      sensorParserConfigHistory5.config = sensorParserConfig5;

      sensorParserConfig6.sensorTopic = 'test2';
      sensorParserConfigHistory6['status'] = 'Disabled';
      sensorParserConfigHistory6.config = sensorParserConfig6;

      sensorParserConfig7.sensorTopic = 'test3';
      sensorParserConfigHistory7['status'] = 'Disabled';
      sensorParserConfigHistory7.config = sensorParserConfig7;

      component.selectedSensors = [
        sensorParserConfigHistory1,
        sensorParserConfigHistory2,
        sensorParserConfigHistory3,
        sensorParserConfigHistory4,
        sensorParserConfigHistory5,
        sensorParserConfigHistory6,
        sensorParserConfigHistory7
      ];

      component.onStartSensors();
      expect(component.onStartSensor['calls'].count()).toEqual(3);

      component.onStopSensors();
      expect(component.onStopSensor['calls'].count()).toEqual(4);

      component.onDisableSensors();
      expect(component.onDisableSensor['calls'].count()).toEqual(2);

      component.onEnableSensors();
      expect(component.onEnableSensor['calls'].count()).toEqual(2);

      fixture.destroy();
    })
  );

  it('sort', async(() => {
    let component: SensorParserListComponent = fixture.componentInstance;

    component.sensors = [
      Object.assign(new SensorParserConfigHistory(), {
        sensorName: 'abc',
        config: {
          parserClassName: 'org.apache.metron.parsers.GrokParser',
          sensorTopic: 'abc'
        },
        createdBy: 'raghu',
        modifiedBy: 'abc',
        createdDate: '2016-11-25 09:09:12',
        modifiedByDate: '2016-11-25 09:09:12'
      }),
      Object.assign(new SensorParserConfigHistory(), {
        sensorName: 'plm',
        config: {
          parserClassName: 'org.apache.metron.parsers.Bro',
          sensorTopic: 'plm'
        },
        createdBy: 'raghu',
        modifiedBy: 'plm',
        createdDate: '2016-11-25 12:39:21',
        modifiedByDate: '2016-11-25 12:39:21'
      }),
      Object.assign(new SensorParserConfigHistory(), {
        sensorName: 'xyz',
        config: {
          parserClassName: 'org.apache.metron.parsers.GrokParser',
          sensorTopic: 'xyz'
        },
        createdBy: 'raghu',
        modifiedBy: 'xyz',
        createdDate: '2016-11-25 12:44:03',
        modifiedByDate: '2016-11-25 12:44:03'
      })
    ];

    component.onSort({ sortBy: 'sensorName', sortOrder: Sort.ASC });
    expect(component.sensors[0].sensorName).toEqual('abc');
    expect(component.sensors[1].sensorName).toEqual('plm');
    expect(component.sensors[2].sensorName).toEqual('xyz');

    component.onSort({ sortBy: 'sensorName', sortOrder: Sort.DSC });
    expect(component.sensors[0].sensorName).toEqual('xyz');
    expect(component.sensors[1].sensorName).toEqual('plm');
    expect(component.sensors[2].sensorName).toEqual('abc');

    component.onSort({ sortBy: 'parserClassName', sortOrder: Sort.ASC });
    expect(component.sensors[0].config.parserClassName).toEqual(
      'org.apache.metron.parsers.Bro'
    );
    expect(component.sensors[1].config.parserClassName).toEqual(
      'org.apache.metron.parsers.GrokParser'
    );
    expect(component.sensors[2].config.parserClassName).toEqual(
      'org.apache.metron.parsers.GrokParser'
    );

    component.onSort({ sortBy: 'parserClassName', sortOrder: Sort.DSC });
    expect(component.sensors[0].config.parserClassName).toEqual(
      'org.apache.metron.parsers.GrokParser'
    );
    expect(component.sensors[1].config.parserClassName).toEqual(
      'org.apache.metron.parsers.GrokParser'
    );
    expect(component.sensors[2].config.parserClassName).toEqual(
      'org.apache.metron.parsers.Bro'
    );

    component.onSort({ sortBy: 'modifiedBy', sortOrder: Sort.ASC });
    expect(component.sensors[0].modifiedBy).toEqual('abc');
    expect(component.sensors[1].modifiedBy).toEqual('plm');
    expect(component.sensors[2].modifiedBy).toEqual('xyz');

    component.onSort({ sortBy: 'modifiedBy', sortOrder: Sort.DSC });
    expect(component.sensors[0].modifiedBy).toEqual('xyz');
    expect(component.sensors[1].modifiedBy).toEqual('plm');
    expect(component.sensors[2].modifiedBy).toEqual('abc');
  }));

  it('sort', async(() => {
    let component: SensorParserListComponent = fixture.componentInstance;

    component.sensors = [
      Object.assign(new SensorParserConfigHistory(), {
        sensorName: 'abc',
        config: {
          parserClassName: 'org.apache.metron.parsers.GrokParser',
          sensorTopic: 'abc'
        },
        createdBy: 'raghu',
        modifiedBy: 'abc',
        createdDate: '2016-11-25 09:09:12',
        modifiedByDate: '2016-11-25 09:09:12'
      })
    ];

    component.sensorsStatus = [
      Object.assign(new TopologyStatus(), {
        name: 'abc',
        status: 'ACTIVE',
        latency: '10',
        throughput: '23'
      })
    ];

    component.updateSensorStatus();
    expect(component.sensors[0]['status']).toEqual('Running');
    expect(component.sensors[0]['latency']).toEqual('10ms');
    expect(component.sensors[0]['throughput']).toEqual('23kb/s');

    component.sensorsStatus[0].status = 'KILLED';
    component.updateSensorStatus();
    expect(component.sensors[0]['status']).toEqual('Stopped');
    expect(component.sensors[0]['latency']).toEqual('-');
    expect(component.sensors[0]['throughput']).toEqual('-');

    component.sensorsStatus[0].status = 'INACTIVE';
    component.updateSensorStatus();
    expect(component.sensors[0]['status']).toEqual('Disabled');
    expect(component.sensors[0]['latency']).toEqual('-');
    expect(component.sensors[0]['throughput']).toEqual('-');

    component.sensorsStatus = [];
    component.updateSensorStatus();
    expect(component.sensors[0]['status']).toEqual('Stopped');
    expect(component.sensors[0]['latency']).toEqual('-');
    expect(component.sensors[0]['throughput']).toEqual('-');
  }));
});
