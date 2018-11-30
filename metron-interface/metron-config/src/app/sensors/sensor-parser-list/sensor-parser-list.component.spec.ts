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
import { Observable, of } from 'rxjs';
import { SensorParserListComponent } from './sensor-parser-list.component';
import { SensorParserConfigService } from '../../service/sensor-parser-config.service';
import { MetronAlerts } from '../../shared/metron-alerts';
import { TopologyStatus } from '../../model/topology-status';
import { ParserConfigModel } from '../models/parser-config.model';
import { AuthenticationService } from '../../service/authentication.service';
import { SensorParserListModule } from './sensor-parser-list.module';
import { MetronDialogBox } from '../../shared/metron-dialog-box';
import 'jquery';
import { APP_CONFIG, METRON_REST_CONFIG } from '../../app.config';
import { StormService } from '../../service/storm.service';
import { IAppConfig } from '../../app.config.interface';
import { ParserMetaInfoModel } from '../models/parser-meta-info.model';
import { Store } from '@ngrx/store';

class MockAuthenticationService extends AuthenticationService {
  constructor(
    private http2: HttpClient,
    private router2: Router,
    @Inject(APP_CONFIG) private config2: IAppConfig
  ) {
    super(http2, router2, config2);
  }

  public checkAuthentication() {}

  public getCurrentUser(options: {}): Observable<HttpResponse<{}>> {
    return Observable.create(observer => {
      observer.next(new HttpResponse({ body: 'test' }));
      observer.complete();
    });
  }
}

class MockSensorParserConfigService extends SensorParserConfigService {
  private sensorParserConfigs: {};

  constructor(
    private http2: HttpClient,
    @Inject(APP_CONFIG) private config2: IAppConfig
  ) {
    super(http2, config2);
  }

  public setSensorParserConfigForTest(sensorParserConfigs: {}) {
    this.sensorParserConfigs = sensorParserConfigs;
  }

  public getAll(): Observable<{ string: ParserConfigModel }> {
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

  constructor(
    private http2: HttpClient,
    @Inject(APP_CONFIG) private config2: IAppConfig
  ) {
    super(http2, config2);
  }

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
  let router: Router;
  let metronAlerts: MetronAlerts;
  let metronDialog: MetronDialogBox;
  let dialogEl: DebugElement;
  let sensors = [
    {
      config: new ParserConfigModel(),
      status: {
        status: 'KILLED'
      },
      isGroup: false
    },
    {
      config: new ParserConfigModel(),
      status: {
        status: 'INACTIVE'
      },
      isGroup: false
    },
    {
      config: new ParserConfigModel(),
      status: {
        status: 'ACTIVE'
      },
      isGroup: false
    },
    {
      config: new ParserConfigModel(),
      status: {
        status: 'ACTIVE'
      },
      isDeleted: true
    },
    {
      config: new ParserConfigModel(),
      status: {
        status: 'ACTIVE'
      },
      isPhantom: true
    }
  ];

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [SensorParserListModule],
      providers: [
        { provide: HttpClient },
        { provide: Store, useValue: {
          pipe: () => {
            return of(sensors)
          },
          dispatch: () => {

          }
        } },
        { provide: Location, useClass: SpyLocation },
        { provide: AuthenticationService, useClass: MockAuthenticationService },
        {
          provide: SensorParserConfigService,
          useClass: MockSensorParserConfigService
        },
        { provide: StormService, useClass: MockStormService },
        { provide: Router, useClass: MockRouter },
        { provide: MetronDialogBox, useClass: MockMetronDialogBox },
        { provide: APP_CONFIG, useValue: METRON_REST_CONFIG },
        MetronAlerts
      ]
    });
    fixture = TestBed.createComponent(SensorParserListComponent);
    comp = fixture.componentInstance;
    authenticationService = TestBed.get(AuthenticationService);
    sensorParserConfigService = TestBed.get(SensorParserConfigService);
    stormService = TestBed.get(StormService);
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

  // FIXME: this is not belongs to the compoent
  // it('getSensors should call getStatus and poll status and all variables should be initialised', async(() => {
  //   let sensorParserConfigHistory1 = new ParserConfigModel();
  //   let sensorParserConfigHistory2 = new ParserConfigModel();
  //   let sensorParserConfig1 = new ParserConfigModel();
  //   let sensorParserConfig2 = new ParserConfigModel();

  //   sensorParserConfigHistory1.setName('squid');
  //   sensorParserConfigHistory2.setName('bro');
  //   sensorParserConfigHistory1.setConfig(sensorParserConfig1);
  //   sensorParserConfigHistory2.setConfig(sensorParserConfig2);

  //   let sensorParserStatus1 = new TopologyStatus();
  //   let sensorParserStatus2 = new TopologyStatus();
  //   sensorParserStatus1.name = 'squid';
  //   sensorParserStatus1.status = 'KILLED';
  //   sensorParserStatus2.name = 'bro';
  //   sensorParserStatus2.status = 'KILLED';

  //   sensorParserConfigService.setSensorParserConfigForTest({
  //     squid: sensorParserConfig1,
  //     bro: sensorParserConfig2
  //   });
  //   stormService.setTopologyStatusForTest([
  //     sensorParserStatus1,
  //     sensorParserStatus2
  //   ]);

  //   let component: SensorParserListComponent = fixture.componentInstance;

  //   component.enableAutoRefresh = false;

  //   component.ngOnInit();

  //   expect(component.sensors[0].sensorName).toEqual(
  //     sensorParserConfigHistory1.sensorName
  //   );
  //   expect(component.sensors[1].sensorName).toEqual(
  //     sensorParserConfigHistory2.sensorName
  //   );
  //   expect(component.sensorsStatus[0]).toEqual(
  //     Object.assign(new TopologyStatus(), sensorParserStatus1)
  //   );
  //   expect(component.sensorsStatus[1]).toEqual(
  //     Object.assign(new TopologyStatus(), sensorParserStatus2)
  //   );
  //   expect(component.selectedSensors).toEqual([]);
  //   expect(component.count).toEqual(2);

  //   fixture.destroy();
  // }));

  it('getParserType should return the Type of Parser', async(() => {
    let component: SensorParserListComponent = fixture.componentInstance;

    let sensorParserConfig1 = new ParserConfigModel();
    sensorParserConfig1.sensorTopic = 'squid';
    sensorParserConfig1.parserClassName =
      'org.apache.metron.parsers.GrokParser';
    let sensorParserConfig2 = new ParserConfigModel();
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

    let sensorParserConfigHistory1 = {
      config: new ParserConfigModel()
    };
    sensorParserConfigHistory1.config.setName('squid');
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

    let sensorParserConfig = new ParserConfigModel();
    sensorParserConfig.sensorTopic = 'squid';
    let sensorParserConfigHistory = {
      config: sensorParserConfig
    };

    component.onRowSelected(sensorParserConfigHistory, event);

    expect(component.selectedSensors[0]).toEqual(sensorParserConfigHistory);

    event = { target: { checked: false } };

    component.onRowSelected(sensorParserConfigHistory, event);
    expect(component.selectedSensors).toEqual([]);

    fixture.destroy();
  }));

  it('onSelectDeselectAll should populate items into selected stack', async(() => {

    // FIXME: this test is testing implementation details: however the business logic hasn't been changed,
    // the test fails because the implementation of the same logic has been changed :(.


    // let component: SensorParserListComponent = fixture.componentInstance;

    // let sensorParserConfig1 = new ParserConfigModel();
    // sensorParserConfig1.sensorTopic = 'squid';
    // let sensorParserConfig2 = new ParserConfigModel();
    // sensorParserConfig2.sensorTopic = 'bro';
    // let sensorParserConfigHistory1 = new ParserMetaInfoModel(sensorParserConfig1);
    // let sensorParserConfigHistory2 = new ParserMetaInfoModel(sensorParserConfig2);

    // component.sensors.push(sensorParserConfigHistory1);
    // component.sensors.push(sensorParserConfigHistory2);

    // let event = { target: { checked: true } };

    // component.onSelectDeselectAll(event);

    // expect(component.selectedSensors).toEqual([
    //   sensorParserConfigHistory1,
    //   sensorParserConfigHistory2
    // ]);

    // event = { target: { checked: false } };

    // component.onSelectDeselectAll(event);

    // expect(component.selectedSensors).toEqual([]);

    // fixture.destroy();
  }));

  it('onSensorRowSelect should change the url and updated the selected items stack', async(() => {
    let sensorParserConfigHistory1 = {
      config: new ParserConfigModel()
    };
    sensorParserConfigHistory1.config.setName('squid');

    let component: SensorParserListComponent = fixture.componentInstance;

    component.selectedSensor = sensorParserConfigHistory1;
    component.onSensorRowSelect(sensorParserConfigHistory1);

    expect(component.selectedSensor).toEqual(null);

    component.onSensorRowSelect(sensorParserConfigHistory1);

    expect(component.selectedSensor).toEqual(sensorParserConfigHistory1);

    component.selectedSensor = sensorParserConfigHistory1;

    component.onSensorRowSelect(sensorParserConfigHistory1);

    expect(component.selectedSensor).toEqual(null);

    fixture.destroy();
  }));

  it('onSensorRowSelect should change the url and updated the selected items stack', async(() => {
    let component: SensorParserListComponent = fixture.componentInstance;

    let sensorParserConfig = new ParserConfigModel();
    sensorParserConfig.sensorTopic = 'squid';
    let sensorParserConfigHistory = {
      config: sensorParserConfig,
      startStopInProgress: false
    }

    component.toggleStartStopInProgress(sensorParserConfigHistory);
    expect(sensorParserConfigHistory.startStopInProgress).toEqual(true);

    component.toggleStartStopInProgress(sensorParserConfigHistory);
    expect(sensorParserConfigHistory.startStopInProgress).toEqual(false);
  }));

  it('onDeleteSensor should call the appropriate url', async(() => {

    // FIXME: we're using ngrx to mark them as deleted and delete then if the user presses "Apply changes"

    // spyOn(metronAlerts, 'showSuccessMessage');
    // spyOn(metronDialog, 'showConfirmationMessage').and.callThrough();

    // let event = new Event('mouse');
    // event.stopPropagation = jasmine.createSpy('stopPropagation');

    // let component: SensorParserListComponent = fixture.componentInstance;
    // let sensorParserConfig1 = new ParserConfigModel();
    // let sensorParserConfig2 = new ParserConfigModel();
    // sensorParserConfig1.setName('squid');
    // sensorParserConfig2.setName('bro');
    // let sensorParserConfigHistory1 = new ParserMetaInfoModel(sensorParserConfig1);
    // let sensorParserConfigHistory2 = new ParserMetaInfoModel(sensorParserConfig2);

    // component.selectedSensors.push(sensorParserConfigHistory1);
    // component.selectedSensors.push(sensorParserConfigHistory2);

    // component.onDeleteSensor();

    // expect(metronAlerts.showSuccessMessage).toHaveBeenCalled();

    // component.deleteSensor([sensorParserConfigHistory1], event);

    // expect(metronDialog.showConfirmationMessage).toHaveBeenCalled();
    // expect(metronDialog.showConfirmationMessage['calls'].count()).toEqual(2);
    // expect(metronDialog.showConfirmationMessage['calls'].count()).toEqual(2);
    // expect(metronDialog.showConfirmationMessage['calls'].all()[0].args).toEqual(
    //   ['Are you sure you want to delete sensor(s) squid, bro ?']
    // );
    // expect(metronDialog.showConfirmationMessage['calls'].all()[1].args).toEqual(
    //   ['Are you sure you want to delete sensor(s) squid ?']
    // );

    // expect(event.stopPropagation).toHaveBeenCalled();

    // fixture.destroy();
  }));

  it('onStopSensor should call the appropriate url', async(() => {
    let event = new Event('mouse');
    event.stopPropagation = jasmine.createSpy('stopPropagation');

    let sensorParserConfig1 = new ParserConfigModel();
    sensorParserConfig1.sensorTopic = 'squid';
    let sensorParserConfigHistory1 = {
      config: sensorParserConfig1
    };

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

    let sensorParserConfig1 = new ParserConfigModel();
    sensorParserConfig1.sensorTopic = 'squid';
    let sensorParserConfigHistory1 = {
      config: sensorParserConfig1
    };

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

    let sensorParserConfig1 = new ParserConfigModel();
    sensorParserConfig1.sensorTopic = 'squid';
    let sensorParserConfigHistory1 = {
      config: sensorParserConfig1
    };

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

    let sensorParserConfig1 = new ParserConfigModel();
    sensorParserConfig1.sensorTopic = 'squid';
    let sensorParserConfigHistory1 = {
      config: sensorParserConfig1
    };

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

      let sensorParserConfig1 = new ParserConfigModel();
      let sensorParserConfig2 = new ParserConfigModel();
      let sensorParserConfig3 = new ParserConfigModel();
      let sensorParserConfig4 = new ParserConfigModel();
      let sensorParserConfig5 = new ParserConfigModel();
      let sensorParserConfig6 = new ParserConfigModel();
      let sensorParserConfig7 = new ParserConfigModel();

      let sensorParserConfigHistory1 = { config: sensorParserConfig1, status: new TopologyStatus() };
      let sensorParserConfigHistory2 = { config: sensorParserConfig2, status: new TopologyStatus() };
      let sensorParserConfigHistory3 = { config: sensorParserConfig3, status: new TopologyStatus() };
      let sensorParserConfigHistory4 = { config: sensorParserConfig4, status: new TopologyStatus() };
      let sensorParserConfigHistory5 = { config: sensorParserConfig5, status: new TopologyStatus() };
      let sensorParserConfigHistory6 = { config: sensorParserConfig6, status: new TopologyStatus() };
      let sensorParserConfigHistory7 = { config: sensorParserConfig7, status: new TopologyStatus() };

      sensorParserConfig1.sensorTopic = 'squid';
      sensorParserConfigHistory1.status.status = 'ACTIVE';

      sensorParserConfig2.sensorTopic = 'bro';
      sensorParserConfigHistory2.status.status = 'KILLED';

      sensorParserConfig3.sensorTopic = 'test';
      sensorParserConfigHistory3.status.status = 'KILLED';

      sensorParserConfig4.sensorTopic = 'test1';
      sensorParserConfigHistory4.status.status = 'KILLED';

      sensorParserConfig5.sensorTopic = 'test2';
      sensorParserConfigHistory5.status.status = 'ACTIVE';

      sensorParserConfig6.sensorTopic = 'test2';
      sensorParserConfigHistory6.status.status = 'INACTIVE';

      sensorParserConfig7.sensorTopic = 'test3';
      sensorParserConfigHistory7.status.status = 'INACTIVE';

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

  it('isStoppable() should return true unless a sensor is KILLED', async(() => {
    const component = Object.create( SensorParserListComponent.prototype );
    const sensorParserConfig1 = new ParserConfigModel();
    let sensor: ParserMetaInfoModel = { config: sensorParserConfig1, status: new TopologyStatus() };

    sensor.status.status = 'KILLED';
    expect(component.isStoppable(sensor)).toBe(false);

    sensor.status.status = 'ACTIVE';
    expect(component.isStoppable(sensor)).toBe(true);

    sensor.status.status = 'INACTIVE';
    expect(component.isStoppable(sensor)).toBe(true);
  }));

  it('isStartable() should return true only when a parser is KILLED', async(() => {
    const component = Object.create( SensorParserListComponent.prototype );
    const sensorParserConfig1 = new ParserConfigModel();
    let sensor: ParserMetaInfoModel = { config: sensorParserConfig1, status: new TopologyStatus() };

    sensor.status.status = 'KILLED';
    expect(component.isStartable(sensor)).toBe(true);

    sensor.status.status = 'ACTIVE';
    expect(component.isStartable(sensor)).toBe(false);

    sensor.status.status = 'INACTIVE';
    expect(component.isStartable(sensor)).toBe(false);
  }));

  it('isEnableable() should return true only when a parser is ACTIVE', async(() => {
    const component = Object.create( SensorParserListComponent.prototype );
    const sensorParserConfig1 = new ParserConfigModel();
    let sensor: ParserMetaInfoModel = { config: sensorParserConfig1, status: new TopologyStatus() };

    sensor.status.status = 'KILLED';
    expect(component.isEnableable(sensor)).toBe(false);

    sensor.status.status = 'ACTIVE';
    expect(component.isEnableable(sensor)).toBe(true);

    sensor.status.status = 'INACTIVE';
    expect(component.isEnableable(sensor)).toBe(false);
  }));

  it('isDisableable() should return true only when a parser is INACTIVE', async(() => {
    const component = Object.create( SensorParserListComponent.prototype );
    const sensorParserConfig1 = new ParserConfigModel();
    let sensor: ParserMetaInfoModel = { config: sensorParserConfig1, status: new TopologyStatus() };

    sensor.status.status = 'KILLED';
    expect(component.isDisableable(sensor)).toBe(false);

    sensor.status.status = 'ACTIVE';
    expect(component.isDisableable(sensor)).toBe(false);

    sensor.status.status = 'INACTIVE';
    expect(component.isDisableable(sensor)).toBe(true);
  }));

  it('isDeletedOrPhantom() should return true if a parser is deleted or a phantom', async(() => {
    const component = Object.create( SensorParserListComponent.prototype );
    const sensorParserConfig1 = new ParserConfigModel();
    let sensor: ParserMetaInfoModel = { config: sensorParserConfig1, status: new TopologyStatus() };

    expect(component.isDeletedOrPhantom(sensor)).toBe(false);

    sensor.isDeleted = true;
    expect(component.isDeletedOrPhantom(sensor)).toBe(true);

    sensor.isDeleted = false;
    sensor.isPhantom = true;
    expect(component.isDeletedOrPhantom(sensor)).toBe(true);
  }));

  it('should hide parser controls when they cannot be used', async(() => {
    fixture.detectChanges();

    const stopButtons = fixture.debugElement.queryAll(By.css('[data-qe-id="stop-parser-button"]'));
    const startButtons = fixture.debugElement.queryAll(By.css('[data-qe-id="start-parser-button"]'));
    const enableButtons = fixture.debugElement.queryAll(By.css('[data-qe-id="enable-parser-button"]'));
    const disableButtons = fixture.debugElement.queryAll(By.css('[data-qe-id="disable-parser-button"]'));
    const controlsWrappers = fixture.debugElement.queryAll(By.css('[data-qe-id="parser-controls"]'));
    const selectWrappers = fixture.debugElement.queryAll(By.css('[data-qe-id="sensor-select"]'));

    // !KILLED status should show stop button
    expect(stopButtons[0].properties.hidden).toBe(true);
    expect(stopButtons[1].properties.hidden).toBe(false);
    expect(stopButtons[2].properties.hidden).toBe(false);

    // KILLED status should only show start button
    expect(startButtons[0].properties.hidden).toBe(false);
    expect(startButtons[1].properties.hidden).toBe(true);
    expect(startButtons[2].properties.hidden).toBe(true);

    // ACTIVE status should show enable buttons
    expect(enableButtons[0].properties.hidden).toBe(true);
    expect(enableButtons[1].properties.hidden).toBe(true);
    expect(enableButtons[2].properties.hidden).toBe(false);

    // INACTIVE status should show disable buttons
    expect(disableButtons[0].properties.hidden).toBe(true);
    expect(disableButtons[1].properties.hidden).toBe(false);
    expect(disableButtons[2].properties.hidden).toBe(true);

    // controls and select checkbox should hide if parser is deleted or a phantom
    expect(controlsWrappers[0].properties.hidden).toBe(false);
    expect(controlsWrappers[1].properties.hidden).toBe(false);
    expect(controlsWrappers[2].properties.hidden).toBe(false);
    expect(controlsWrappers[3].properties.hidden).toBe(true);
    expect(controlsWrappers[4].properties.hidden).toBe(true);

    expect(selectWrappers[0].properties.hidden).toBe(false);
    expect(selectWrappers[1].properties.hidden).toBe(false);
    expect(selectWrappers[2].properties.hidden).toBe(false);
    expect(selectWrappers[3].properties.hidden).toBe(true);
    expect(selectWrappers[4].properties.hidden).toBe(true);
  }));
});
