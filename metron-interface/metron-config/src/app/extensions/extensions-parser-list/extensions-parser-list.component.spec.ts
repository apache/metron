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

import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {SpyLocation} from '@angular/common/testing';
import {Http, ResponseOptions, RequestOptions, Response} from '@angular/http';

import {DebugElement, Inject} from '@angular/core';
import {By} from '@angular/platform-browser';
import {Router, NavigationStart} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {ExtensionsParserListComponent} from './extensions-parser-list.component';
import {ParserExtensionService} from '../../service/parser-extension.service';
import {MetronAlerts} from '../../shared/metron-alerts';
import {TopologyStatus} from '../../model/topology-status';
import {ParserExtensionConfig} from '../../model/parser-extension-config';
import {AuthenticationService} from '../../service/authentication.service';
import {ExtensionsParserListModule} from './extensions-parser-list.module';
import {MetronDialogBox} from '../../shared/metron-dialog-box';
import {Sort} from '../../util/enums';
import 'jquery';
import {APP_CONFIG, METRON_REST_CONFIG} from '../../app.config';
import {StormService} from '../../service/storm.service';
import {IAppConfig} from '../../app.config.interface';

class MockAuthenticationService extends AuthenticationService {

  constructor(private http2: Http, private router2: Router, @Inject(APP_CONFIG) private config2: IAppConfig) {
    super(http2, router2, config2);
  }

  public checkAuthentication() {
  }

  public getCurrentUser(options: RequestOptions): Observable<Response> {
    return Observable.create(observer => {
      observer.next(new Response(new ResponseOptions({body: 'test'})));
      observer.complete();
    });
  }
}

class MockParserExtensionService extends ParserExtensionService {
  private parserExtensionConfigs: ParserExtensionConfig[];

  constructor(private http2: Http, @Inject(APP_CONFIG) private config2: IAppConfig) {
    super(http2, config2);
  }

  public post(extensionTgz: string, contents: FormData): Observable<Response> {
    return Observable.create(observer => {
      observer.next(new Response(new ResponseOptions({status:201})))
      observer.complete();
    });
  }

  public setParserExtensionConfigForTest(parserExtensionConfigs: ParserExtensionConfig[]) {
    this.parserExtensionConfigs = parserExtensionConfigs;
  }

  public getAll(): Observable<ParserExtensionConfig[]> {
    return Observable.create(observer => {
      observer.next(this.parserExtensionConfigs);
      observer.complete();
    });
  }

  public delete(parserExtension: string) :Observable<Response> {
    return Observable.create(observer => {
      observer.next( new Response(new ResponseOptions({status: 201})));
      observer.complete();
    });
  }

  public deleteMany(sensors: ParserExtensionConfig[]): Observable<{success: Array<string>, failure: Array<string>}> {
    let result: {success: Array<string>, failure: Array<string>} = {success: [], failure: []};
    let observable = Observable.create((observer => {
      for (let i = 0; i < sensors.length; i++) {
        result.success.push(sensors[i].extensionIdentifier);
      }
      observer.next(result);
      observer.complete();
    }));
    return observable;
  }
}

class MockRouter {
  events: Observable<Event> = Observable.create(observer => {
    observer.next(new NavigationStart(1, '/sensors'));
    observer.complete();
  });

  navigateByUrl(url: string) {
  }
}

class MockMetronDialogBox {

  public showConfirmationMessage(message: string) {
    return Observable.create(observer => {
      observer.next(true);
      observer.complete();
    });
  }
}

describe('Component: ExtensionsParserList', () => {

  let comp: ExtensionsParserListComponent;
  let fixture: ComponentFixture<ExtensionsParserListComponent>;
  let authenticationService: MockAuthenticationService;
  let parserExtensionService: MockParserExtensionService
  let router: Router;
  let metronAlerts: MetronAlerts;
  let metronDialog: MetronDialogBox;
  let dialogEl: DebugElement;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      imports: [ExtensionsParserListModule],
      providers: [
        {provide: Http},
        {provide: Location, useClass: SpyLocation},
        {provide: AuthenticationService, useClass: MockAuthenticationService},
        {provide: ParserExtensionService, useClass: MockParserExtensionService},
        {provide: Router, useClass: MockRouter},
        {provide: MetronDialogBox, useClass: MockMetronDialogBox},
        {provide: APP_CONFIG, useValue: METRON_REST_CONFIG},
        MetronAlerts
      ]
    }).compileComponents()
      .then(() => {
        fixture = TestBed.createComponent(ExtensionsParserListComponent);
        comp = fixture.componentInstance;
        authenticationService = fixture.debugElement.injector.get(AuthenticationService);
        parserExtensionService = fixture.debugElement.injector.get(ParserExtensionService);
        router = fixture.debugElement.injector.get(Router);
        metronAlerts = fixture.debugElement.injector.get(MetronAlerts);
        metronDialog = fixture.debugElement.injector.get(MetronDialogBox);
        dialogEl = fixture.debugElement.query(By.css('.primary'));
      });

  }));

  it('should create an instance', async(() => {

    let component: ExtensionsParserListComponent = fixture.componentInstance;
    expect(component).toBeDefined();
    fixture.destroy();

  }));

  it('all variables should be initialised', async(() => {
    let sensorParserConfig1 = new ParserExtensionConfig();
    let sensorParserConfig2 = new ParserExtensionConfig();

    sensorParserConfig1.extensionIdentifier = 'squid';
    sensorParserConfig2.extensionIdentifier = 'bro';

    parserExtensionService.setParserExtensionConfigForTest([sensorParserConfig1, sensorParserConfig2]);

    let component: ExtensionsParserListComponent = fixture.componentInstance;

    component.enableAutoRefresh = false;

    component.ngOnInit();

    expect(component.parserExtensions[0]).toEqual(sensorParserConfig1);
    expect(component.parserExtensions[1]).toEqual(sensorParserConfig2);
    expect(component.selectedExtensions).toEqual([]);
    expect(component.count).toEqual(2);

    fixture.destroy();
  }));

  it('addAddSensor should change the URL', async(() => {
    spyOn(router, 'navigateByUrl');

    let component: ExtensionsParserListComponent = fixture.componentInstance;

    component.addAddParserExtension();

    let expectStr = router.navigateByUrl['calls'].argsFor(0);
    expect(expectStr).toEqual(['/extensions(dialog:extensions-install)']);

    fixture.destroy();
  }));


  it('onRowSelected should add add/remove items from the selected stack', async(() => {

    let component: ExtensionsParserListComponent = fixture.componentInstance;
    let event = {target: {checked: true}};

    let sensorParserConfig = new ParserExtensionConfig();

    sensorParserConfig.extensionIdentifier = 'squid';

    component.onRowSelected(sensorParserConfig, event);

    expect(component.selectedExtensions[0]).toEqual(sensorParserConfig);

    event = {target: {checked: false}};

    component.onRowSelected(sensorParserConfig, event);
    expect(component.selectedExtensions).toEqual([]);

    fixture.destroy();
  }));

  it('onSelectDeselectAll should populate items into selected stack', async(() => {

    let component: ExtensionsParserListComponent = fixture.componentInstance;

    let sensorParserConfig1 = new ParserExtensionConfig();
    let sensorParserConfig2 = new ParserExtensionConfig();

    sensorParserConfig1.extensionIdentifier = 'squid';
    sensorParserConfig2.extensionIdentifier = 'bro';

    component.parserExtensions.push(sensorParserConfig1);
    component.parserExtensions.push(sensorParserConfig2);

    let event = {target: {checked: true}};

    component.onSelectDeselectAll(event);

    expect(component.selectedExtensions).toEqual([sensorParserConfig1, sensorParserConfig2]);

    event = {target: {checked: false}};

    component.onSelectDeselectAll(event);

    expect(component.selectedExtensions).toEqual([]);

    fixture.destroy();
  }));

  it('onSensorRowSelect should change the url and updated the selected items stack', async(() => {

    let sensorParserConfig1 = new ParserExtensionConfig();
    sensorParserConfig1.extensionIdentifier = 'squid';

    let component: ExtensionsParserListComponent = fixture.componentInstance;
    let event = {target: {type: 'div', parentElement: {firstChild: {type: 'div'}}}};

    parserExtensionService.setSeletedExtension(sensorParserConfig1);
    component.onParserExtensionRowSelect(sensorParserConfig1, event);

    expect(parserExtensionService.getSelectedExtension()).toEqual(null);

    component.onParserExtensionRowSelect(sensorParserConfig1, event);

    expect(parserExtensionService.getSelectedExtension()).toEqual(sensorParserConfig1);

    parserExtensionService.setSeletedExtension(sensorParserConfig1);
    event = {target: {type: 'checkbox', parentElement: {firstChild: {type: 'div'}}}};

    component.onParserExtensionRowSelect(sensorParserConfig1, event);

    expect(parserExtensionService.getSelectedExtension()).toEqual(sensorParserConfig1);

    fixture.destroy();
  }));

  it('onDeleteSensor should call the appropriate url',  async(() => {

    spyOn(metronAlerts, 'showSuccessMessage');
    spyOn(metronDialog, 'showConfirmationMessage').and.callThrough();

    let event = new Event('mouse');
    event.stopPropagation = jasmine.createSpy('stopPropagation');

    let component: ExtensionsParserListComponent =  fixture.componentInstance;
    let sensorParserConfig1 = new ParserExtensionConfig();
    let sensorParserConfig2 = new ParserExtensionConfig();

    sensorParserConfig1.extensionIdentifier = 'squid';
    sensorParserConfig2.extensionIdentifier = 'bro';

    component.selectedExtensions.push(sensorParserConfig1);
    component.selectedExtensions.push(sensorParserConfig2);

    component.onDeleteParserExtension();

    expect(metronAlerts.showSuccessMessage).toHaveBeenCalled();

    component.deleteParserExtension(event, [sensorParserConfig1]);

    expect(metronDialog.showConfirmationMessage).toHaveBeenCalled();
    expect(metronDialog.showConfirmationMessage['calls'].count()).toEqual(2);
    expect(metronDialog.showConfirmationMessage['calls'].count()).toEqual(2);
    expect(metronDialog.showConfirmationMessage['calls'].all()[0].args).toEqual(['Are you sure you want to delete extension(s) squid, bro ?']);
    expect(metronDialog.showConfirmationMessage['calls'].all()[1].args).toEqual(['Are you sure you want to delete extension(s) squid ?']);

    expect(event.stopPropagation).toHaveBeenCalled();

    fixture.destroy();

  }));

  it('sort', async(() => {
    let component: ExtensionsParserListComponent = fixture.componentInstance;

    component.parserExtensions = [
      Object.assign(new ParserExtensionConfig(), {
        'extensionIdentifier': "squid",
        'extensionBundleID': 'abc',
      }),
      Object.assign(new ParserExtensionConfig(), {
        'extensionIdentifier': 'bro',
        'extensionBundleID': 'plm',
      }),
      Object.assign(new ParserExtensionConfig(), {
        'extensionIdentifier': 'asa',
        'extensionBundleID': 'xyz',
      })
    ];

    component.onSort({sortBy: 'extensionIdentifier', sortOrder: Sort.ASC});
    expect(component.parserExtensions[0].extensionIdentifier).toEqual('asa');
    expect(component.parserExtensions[1].extensionIdentifier).toEqual('bro');
    expect(component.parserExtensions[2].extensionIdentifier).toEqual('squid');

    component.onSort({sortBy: 'extensionIdentifier', sortOrder: Sort.DSC});
    expect(component.parserExtensions[0].extensionIdentifier).toEqual('squid');
    expect(component.parserExtensions[1].extensionIdentifier).toEqual('bro');
    expect(component.parserExtensions[2].extensionIdentifier).toEqual('asa');

    component.onSort({sortBy: 'extensionBundleID', sortOrder: Sort.ASC});
    expect(component.parserExtensions[0].extensionBundleID).toEqual('abc');
    expect(component.parserExtensions[1].extensionBundleID).toEqual('plm');
    expect(component.parserExtensions[2].extensionBundleID).toEqual('xyz');

    component.onSort({sortBy: 'extensionBundleID', sortOrder: Sort.DSC});
    expect(component.parserExtensions[0].extensionBundleID).toEqual('xyz');
    expect(component.parserExtensions[1].extensionBundleID).toEqual('plm');
    expect(component.parserExtensions[2].extensionBundleID).toEqual('abc');
  }));

});
