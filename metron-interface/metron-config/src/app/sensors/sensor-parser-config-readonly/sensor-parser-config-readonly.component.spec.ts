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
import { Observable } from 'rxjs';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Inject } from '@angular/core';
import { SensorParserConfigHistory } from '../../model/sensor-parser-config-history';
import { HttpClient } from '@angular/common/http';
import { SensorParserConfigReadonlyComponent } from './sensor-parser-config-readonly.component';
import { SensorParserConfigService } from '../../service/sensor-parser-config.service';
import { KafkaService } from '../../service/kafka.service';
import { TopologyStatus } from '../../model/topology-status';
import { SensorParserConfig } from '../../model/sensor-parser-config';
import { KafkaTopic } from '../../model/kafka-topic';
import { AuthenticationService } from '../../service/authentication.service';
import { SensorParserConfigHistoryService } from '../../service/sensor-parser-config-history.service';
import { StormService } from '../../service/storm.service';
import { MetronAlerts } from '../../shared/metron-alerts';
import { FieldTransformer } from '../../model/field-transformer';
import { SensorParserConfigReadonlyModule } from './sensor-parser-config-readonly.module';
import { SensorEnrichmentConfigService } from '../../service/sensor-enrichment-config.service';
import {
  SensorEnrichmentConfig,
  EnrichmentConfig,
  ThreatIntelConfig
} from '../../model/sensor-enrichment-config';
import { HdfsService } from '../../service/hdfs.service';
import { GrokValidationService } from '../../service/grok-validation.service';
import { RiskLevelRule } from '../../model/risk-level-rule';
import {AppConfigService} from '../../service/app-config.service';
import {MockAppConfigService} from '../../service/mock.app-config.service';

class MockRouter {
  navigateByUrl(url: string) {}
}

class MockActivatedRoute {
  private name: string;
  params: Observable<Params>;

  setNameForTest(name: string) {
    this.name = name;
    this.params = Observable.create(observer => {
      observer.next({ id: this.name });
      observer.complete();
    });
  }
}

class MockAuthenticationService extends AuthenticationService {

  public getCurrentUser(options): Observable<{}> {
    let response: { body: 'user' };
    return Observable.create(observer => {
      observer.next(response);
      observer.complete();
    });
  }
}

class MockSensorParserConfigHistoryService extends SensorParserConfigHistoryService {
  private sensorParserConfigHistory: SensorParserConfigHistory;

  public setForTest(sensorParserConfigHistory: SensorParserConfigHistory) {
    this.sensorParserConfigHistory = sensorParserConfigHistory;
  }

  public get(name: string): Observable<SensorParserConfigHistory> {
    return Observable.create(observer => {
      observer.next(this.sensorParserConfigHistory);
      observer.complete();
    });
  }
}

class MockSensorParserConfigService extends SensorParserConfigService {
}

class MockStormService extends StormService {
  private topologyStatus: TopologyStatus;

  public setForTest(topologyStatus: TopologyStatus) {
    this.topologyStatus = topologyStatus;
  }

  public getStatus(name: string): Observable<TopologyStatus> {
    return Observable.create(observer => {
      observer.next(this.topologyStatus);
      observer.complete();
    });
  }
}

class MockGrokValidationService extends GrokValidationService {

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

class MockKafkaService extends KafkaService {
  private kafkaTopic: KafkaTopic;

  public setForTest(kafkaTopic: KafkaTopic) {
    this.kafkaTopic = kafkaTopic;
  }

  public get(name: string): Observable<KafkaTopic> {
    return Observable.create(observer => {
      observer.next(this.kafkaTopic);
      observer.complete();
    });
  }

  public sample(name: string): Observable<string> {
    return Observable.create(observer => {
      observer.next(JSON.stringify({ data: 'data1', data2: 'data3' }));
      observer.complete();
    });
  }
}

class MockHdfsService extends HdfsService {
  private fileList: string[];
  private contents: string;

  public setContents(contents: string) {
    this.contents = contents;
  }

  public list(path: string): Observable<string[]> {
    if (this.fileList === null) {
      return Observable.throw('Error');
    }
    return Observable.create(observer => {
      observer.next(this.fileList);
      observer.complete();
    });
  }

  public read(path: string): Observable<string> {
    if (this.contents === null) {
      return Observable.throw('Error');
    }
    return Observable.create(observer => {
      observer.next(this.contents);
      observer.complete();
    });
  }

  public post(path: string, contents: string): Observable<{}> {
    return Observable.create(observer => {
      observer.next({});
      observer.complete();
    });
  }

  public deleteFile(path: string): Observable<{}> {
    return Observable.create(observer => {
      observer.next({});
      observer.complete();
    });
  }
}

class MockSensorEnrichmentConfigService {
  private sensorEnrichmentConfig: SensorEnrichmentConfig;

  setForTest(sensorEnrichmentConfig: SensorEnrichmentConfig) {
    this.sensorEnrichmentConfig = sensorEnrichmentConfig;
  }

  public get(name: string): Observable<SensorEnrichmentConfig> {
    return Observable.create(observer => {
      observer.next(this.sensorEnrichmentConfig);
      observer.complete();
    });
  }

  public getAvailable(): Observable<string[]> {
    return Observable.create(observer => {
      observer.next(['geo', 'host', 'whois']);
      observer.complete();
    });
  }
}

describe('Component: SensorParserConfigReadonly', () => {
  let component: SensorParserConfigReadonlyComponent;
  let fixture: ComponentFixture<SensorParserConfigReadonlyComponent>;
  let sensorParserConfigHistoryService: MockSensorParserConfigHistoryService;
  let sensorEnrichmentConfigService: MockSensorEnrichmentConfigService;
  let sensorParserConfigService: SensorParserConfigService;
  let kafkaService: MockKafkaService;
  let hdfsService: MockHdfsService;
  let grokValidationService: MockGrokValidationService;
  let stormService: MockStormService;
  let alerts: MetronAlerts;
  let authenticationService: AuthenticationService;
  let router: MockRouter;
  let activatedRoute: MockActivatedRoute;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [SensorParserConfigReadonlyModule],
      providers: [
        { provide: HttpClient },
        { provide: ActivatedRoute, useClass: MockActivatedRoute },
        {
          provide: SensorEnrichmentConfigService,
          useClass: MockSensorEnrichmentConfigService
        },
        {
          provide: SensorParserConfigHistoryService,
          useClass: MockSensorParserConfigHistoryService
        },
        {
          provide: SensorParserConfigService,
          useClass: MockSensorParserConfigService
        },
        { provide: StormService, useClass: MockStormService },
        { provide: KafkaService, useClass: MockKafkaService },
        { provide: HdfsService, useClass: MockHdfsService },
        { provide: GrokValidationService, useClass: MockGrokValidationService },
        { provide: Router, useClass: MockRouter },
        { provide: AppConfigService, useClass: MockAppConfigService },
        MetronAlerts
      ]
    });
    fixture = TestBed.createComponent(SensorParserConfigReadonlyComponent);
    component = fixture.componentInstance;
    activatedRoute = TestBed.get(ActivatedRoute);
    hdfsService = TestBed.get(HdfsService);
    sensorParserConfigHistoryService = TestBed.get(
      SensorParserConfigHistoryService
    );
    sensorEnrichmentConfigService = TestBed.get(SensorEnrichmentConfigService);
    sensorParserConfigService = TestBed.get(SensorParserConfigService);
    stormService = TestBed.get(StormService);
    kafkaService = TestBed.get(KafkaService);
    grokValidationService = TestBed.get(GrokValidationService);
    router = TestBed.get(Router);
    alerts = TestBed.get(MetronAlerts);
  }));

  it('should create an instance', async(() => {
    expect(component).toBeDefined();
  }));

  it('should have metadata defined ', async(() => {
    // the expected value refers to the number of fields that should be visible in the readonly view
    expect(component.editViewMetaData.length).toEqual(32);
  }));

  it('should have sensorsService with parserName and grokPattern defined and kafkaService defined', async(() => {
    let sensorParserInfo = new SensorParserConfigHistory();
    let sensorParserConfig = new SensorParserConfig();
    let kafkaTopic = new KafkaTopic();
    let topologyStatus = new TopologyStatus();

    sensorParserConfig.sensorTopic = 'bro';
    sensorParserConfig.parserClassName = 'org.apache.metron.parsers.GrokParser';
    sensorParserConfig.parserConfig = {
      grokPattern: 'SQUID_DELIMITED squid grok statement'
    };
    sensorParserInfo.config = sensorParserConfig;

    kafkaTopic.name = 'bro';
    kafkaTopic.numPartitions = 1;
    kafkaTopic.replicationFactor = 1;

    topologyStatus.name = 'bro';
    topologyStatus.latency = 10.1;
    topologyStatus.throughput = 15.2;

    let broEnrichment = {
      fieldMap: {
        geo: ['ip_dst_addr'],
        host: ['ip_dst_addr'],
        whois: [],
        stellar: { config: { group1: {} } }
      },
      fieldToTypeMap: {},
      config: {}
    };
    let broThreatIntel = {
      threatIntel: {
        fieldMap: { hbaseThreatIntel: ['ip_dst_addr'] },
        fieldToTypeMap: { ip_dst_addr: ['malicious_ip'] }
      }
    };
    let broEnrichments = new SensorEnrichmentConfig();
    broEnrichments.enrichment = Object.assign(
      new EnrichmentConfig(),
      broEnrichment
    );
    broEnrichments.threatIntel = Object.assign(
      new ThreatIntelConfig(),
      broThreatIntel
    );

    sensorEnrichmentConfigService.setForTest(broEnrichments);
    sensorParserConfigHistoryService.setForTest(sensorParserInfo);
    kafkaService.setForTest(kafkaTopic);
    stormService.setForTest(topologyStatus);

    activatedRoute.setNameForTest('bro');

    component.ngOnInit();
    expect(component.startStopInProgress).toEqual(false);
    expect(component.sensorParserConfigHistory).toEqual(
      Object.assign(new SensorParserConfigHistory(), sensorParserInfo)
    );
    expect(component.kafkaTopic).toEqual(kafkaTopic);
    expect(component.sensorEnrichmentConfig).toEqual(broEnrichments);
  }));

  it('getSensorStatusService should initialise the state variable to appropriate values ', async(() => {
    let sensorParserStatus = new TopologyStatus();
    sensorParserStatus.name = 'bro';
    sensorParserStatus.latency = 10.1;
    sensorParserStatus.status = null;
    sensorParserStatus.throughput = 15.2;

    stormService.setForTest(sensorParserStatus);

    component.getSensorStatusService();
    expect(component.getTopologyStatus('status')).toEqual('Stopped');
    expect(component.getTopologyStatus('sensorStatus')).toEqual('-');

    sensorParserStatus.status = 'ACTIVE';
    component.getSensorStatusService();
    stormService.setForTest(sensorParserStatus);
    expect(component.getTopologyStatus('status')).toEqual('Running');
    expect(component.getTopologyStatus('sensorStatus')).toEqual('Enabled');

    sensorParserStatus.status = 'KILLED';
    component.getSensorStatusService();
    stormService.setForTest(sensorParserStatus);
    expect(component.getTopologyStatus('status')).toEqual('Stopped');
    expect(component.getTopologyStatus('sensorStatus')).toEqual('-');

    sensorParserStatus.status = 'INACTIVE';
    component.getSensorStatusService();
    stormService.setForTest(sensorParserStatus);
    expect(component.getTopologyStatus('status')).toEqual('Disabled');
    expect(component.getTopologyStatus('sensorStatus')).toEqual('Disabled');
  }));

  it('setGrokStatement should set the variables appropriately ', async(() => {
    let grokStatement = 'SQUID_DELIMITED squid grok statement';
    hdfsService.setContents(grokStatement);
    let sensorParserInfo = new SensorParserConfigHistory();
    let sensorParserConfig = new SensorParserConfig();
    sensorParserConfig.parserConfig = {};

    sensorParserConfig.parserConfig['grokPath'] = '/squid/grok/path';
    sensorParserInfo.config = sensorParserConfig;

    component.sensorParserConfigHistory = sensorParserInfo;
    component.setGrokStatement();

    expect(component.grokStatement).toEqual(grokStatement);
  }));

  it('setTransformsConfigKeys/getTransformsOutput should return the keys of the transforms config  ', async(() => {
    let sensorParserInfo = new SensorParserConfigHistory();
    let sensorParserConfig = new SensorParserConfig();
    let fieldTransformer1 = new FieldTransformer();
    let fieldTransformer2 = new FieldTransformer();

    fieldTransformer1.config = { a: 'abc', x: 'xyz' };
    fieldTransformer1.output = ['a', 'b', 'c'];
    fieldTransformer2.config = { x: 'klm', b: 'def' };
    fieldTransformer2.output = ['a', 'b', 'c'];
    sensorParserConfig.fieldTransformations = [
      fieldTransformer1,
      fieldTransformer2
    ];
    sensorParserInfo.config = sensorParserConfig;

    component.setTransformsConfigKeys();
    let transformsOutput = component.getTransformsOutput();

    expect(component.transformsConfigKeys.length).toEqual(0);
    expect(component.transformsConfigKeys).toEqual([]);
    expect(component.transformsConfigMap).toEqual({});
    expect(transformsOutput).toEqual('-');

    component.sensorParserConfigHistory = sensorParserInfo;
    component.setTransformsConfigKeys();
    transformsOutput = component.getTransformsOutput();

    expect(component.transformsConfigKeys.length).toEqual(3);
    expect(component.transformsConfigKeys).toEqual(['a', 'b', 'x']);
    expect(component.transformsConfigMap).toEqual({
      a: ['abc'],
      b: ['def'],
      x: ['xyz', 'klm']
    });
    expect(transformsOutput).toEqual('a, b, c');
  }));

  it('goBack should navigate to sensors page', async(() => {
    router.navigateByUrl = jasmine.createSpy('navigateByUrl');

    component.goBack();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/sensors');
  }));

  it('onEditSensor should navigate to sensor edit', async(() => {
    router.navigateByUrl = jasmine.createSpy('navigateByUrl');

    component.selectedSensorName = 'abc';

    component.onEditSensor();
    expect(router.navigateByUrl).toHaveBeenCalledWith(
      '/sensors(dialog:sensors-config/abc)'
    );
  }));

  it('should set sensorEnrichmentConfig and aggregationConfigKeys to be initialised', async(() => {
    let threatIntel = {
      fieldMap: {
        hbaseThreatIntel: ['ip_dst_addr', 'ip_src_addr', 'action']
      },
      fieldToTypeMap: {
        ip_dst_addr: ['malicious_ip'],
        ip_src_addr: ['malicious_ip'],
        action: ['malicious_ip']
      },
      config: {},
      triageConfig: {
        riskLevelRules: [
          {
            rule: "IN_SUBNET(ip_dst_addr, '192.168.0.0/24')",
            score: 3,
            name: 'test1',
            comment: 'This is a comment'
          },
          {
            rule: "user.type in [ 'admin', 'power' ] and asset.type == 'web'",
            score: 3,
            name: 'test2',
            comment: 'This is another comment'
          }
        ],
        aggregator: 'MAX',
        aggregationConfig: {}
      }
    };
    let expected: RiskLevelRule[] = [
      {
        rule: "IN_SUBNET(ip_dst_addr, '192.168.0.0/24')",
        score: 3,
        name: 'test1',
        comment: 'This is a comment'
      },
      {
        rule: "user.type in [ 'admin', 'power' ] and asset.type == 'web'",
        score: 3,
        name: 'test2',
        comment: 'This is another comment'
      }
    ];

    let sensorEnrichmentConfig = new SensorEnrichmentConfig();
    sensorEnrichmentConfig.threatIntel = Object.assign(
      new ThreatIntelConfig(),
      threatIntel
    );
    sensorEnrichmentConfigService.setForTest(sensorEnrichmentConfig);

    component.getEnrichmentData();

    expect(component.sensorEnrichmentConfig).toEqual(sensorEnrichmentConfig);
    expect(component.rules).toEqual(expected);
  }));

  let setDataForSensorOperation = function() {
    let sensorParserInfo = new SensorParserConfigHistory();
    let sensorParserConfig = new SensorParserConfig();
    let kafkaTopic = new KafkaTopic();
    let topologyStatus = new TopologyStatus();

    sensorParserConfig.sensorTopic = 'bro';
    sensorParserConfig.parserClassName = 'org.apache.metron.parsers.GrokParser';
    sensorParserConfig.parserConfig = {
      grokPattern: 'SQUID_DELIMITED squid grok statement'
    };
    sensorParserInfo.config = sensorParserConfig;

    kafkaTopic.name = 'bro';
    kafkaTopic.numPartitions = 1;
    kafkaTopic.replicationFactor = 1;

    topologyStatus.name = 'bro';
    topologyStatus.latency = 10.1;
    topologyStatus.throughput = 15.2;

    let broEnrichment = {
      fieldMap: {
        geo: ['ip_dst_addr'],
        host: ['ip_dst_addr'],
        whois: [],
        stellar: { config: { group1: {} } }
      },
      fieldToTypeMap: {},
      config: {}
    };
    let broThreatIntel = {
      threatIntel: {
        fieldMap: { hbaseThreatIntel: ['ip_dst_addr'] },
        fieldToTypeMap: { ip_dst_addr: ['malicious_ip'] }
      }
    };
    let broEnrichments = new SensorEnrichmentConfig();
    broEnrichments.enrichment = Object.assign(
      new EnrichmentConfig(),
      broEnrichment
    );
    broEnrichments.threatIntel = Object.assign(
      new ThreatIntelConfig(),
      broThreatIntel
    );

    kafkaService.setForTest(kafkaTopic);
    stormService.setForTest(topologyStatus);
    sensorEnrichmentConfigService.setForTest(broEnrichments);
    sensorParserConfigHistoryService.setForTest(sensorParserInfo);
  };

  it('onStartSensor should  start sensor', async(() => {
    spyOn(stormService, 'startParser').and.returnValue(
      Observable.create(observer => {
        observer.next({});
        observer.complete();
      })
    );

    alerts.showSuccessMessage = jasmine.createSpy('showSuccessMessage');
    setDataForSensorOperation();

    component.selectedSensorName = 'abc';

    component.onStartSensor();

    expect(stormService.startParser).toHaveBeenCalledWith('abc');
    expect(alerts.showSuccessMessage).toHaveBeenCalledWith(
      'Started sensor abc'
    );
  }));

  it('onStopSensor should stop the sensor', async(() => {
    spyOn(stormService, 'stopParser').and.returnValue(
      Observable.create(observer => {
        observer.next({});
        observer.complete();
      })
    );

    alerts.showSuccessMessage = jasmine.createSpy('showSuccessMessage');
    setDataForSensorOperation();

    component.selectedSensorName = 'abc';

    component.onStopSensor();

    expect(stormService.stopParser).toHaveBeenCalledWith('abc');
    expect(alerts.showSuccessMessage).toHaveBeenCalledWith(
      'Stopped sensor abc'
    );
  }));

  it('onEnableSensor should enable sensor', async(() => {
    spyOn(stormService, 'activateParser').and.returnValue(
      Observable.create(observer => {
        observer.next({});
        observer.complete();
      })
    );

    alerts.showSuccessMessage = jasmine.createSpy('showSuccessMessage');
    setDataForSensorOperation();

    component.selectedSensorName = 'abc';

    component.onEnableSensor();

    expect(stormService.activateParser).toHaveBeenCalledWith('abc');
    expect(alerts.showSuccessMessage).toHaveBeenCalledWith(
      'Enabled sensor abc'
    );
  }));

  it('onDisableSensor should disable the sensor', async(() => {
    spyOn(stormService, 'deactivateParser').and.returnValue(
      Observable.create(observer => {
        observer.next({});
        observer.complete();
      })
    );

    alerts.showSuccessMessage = jasmine.createSpy('showSuccessMessage');
    setDataForSensorOperation();

    component.selectedSensorName = 'abc';

    component.onDisableSensor();

    expect(stormService.deactivateParser).toHaveBeenCalledWith('abc');
    expect(alerts.showSuccessMessage).toHaveBeenCalledWith(
      'Disabled sensor abc'
    );
  }));

  it('onDeleteSensor should delete the sensor', async(() => {
    spyOn(
      sensorParserConfigService,
      'deleteSensorParserConfig'
    ).and.returnValue(
      Observable.create(observer => {
        observer.next({});
        observer.complete();
      })
    );

    alerts.showSuccessMessage = jasmine.createSpy('showSuccessMessage');
    router.navigateByUrl = jasmine.createSpy('navigateByUrl');
    setDataForSensorOperation();

    component.selectedSensorName = 'abc';

    component.onDeleteSensor();

    expect(
      sensorParserConfigService.deleteSensorParserConfig
    ).toHaveBeenCalledWith('abc');
    expect(alerts.showSuccessMessage).toHaveBeenCalledWith(
      'Deleted sensor abc'
    );
    expect(router.navigateByUrl).toHaveBeenCalledWith('/sensors');
  }));

  it('toggleStartStopInProgress should toggle the variable for showing progressbar', async(() => {
    expect(component.startStopInProgress).toEqual(false);

    component.startStopInProgress = true;
    expect(component.startStopInProgress).toEqual(true);

    component.startStopInProgress = false;
    expect(component.startStopInProgress).toEqual(false);
  }));

  it('should toggleTransformLink', async(() => {
    expect(component.transformLinkText).toEqual('show more');

    component.toggleTransformLink();
    expect(component.transformLinkText).toEqual('show less');

    component.toggleTransformLink();
    expect(component.transformLinkText).toEqual('show more');
  }));

  it('should toggleThreatTriageLink', async(() => {
    expect(component.threatTriageLinkText).toEqual('show more');

    component.toggleThreatTriageLink();
    expect(component.threatTriageLinkText).toEqual('show less');

    component.toggleThreatTriageLink();
    expect(component.threatTriageLinkText).toEqual('show more');
  }));

  it('should hide start', async(() => {
    component.topologyStatus.status = 'ACTIVE';
    expect(component.isStartHidden()).toEqual(true);

    component.topologyStatus.status = 'INACTIVE';
    expect(component.isStartHidden()).toEqual(true);

    component.topologyStatus.status = 'Stopped';
    expect(component.isStartHidden()).toEqual(false);

    component.topologyStatus.status = 'KILLED';
    expect(component.isStartHidden()).toEqual(false);
  }));

  it('should hide stop', async(() => {
    component.topologyStatus.status = 'ACTIVE';
    expect(component.isStopHidden()).toEqual(false);

    component.topologyStatus.status = 'INACTIVE';
    expect(component.isStopHidden()).toEqual(false);

    component.topologyStatus.status = 'Stopped';
    expect(component.isStopHidden()).toEqual(true);

    component.topologyStatus.status = 'KILLED';
    expect(component.isStopHidden()).toEqual(true);
  }));

  it('should hide enable', async(() => {
    component.topologyStatus.status = 'ACTIVE';
    expect(component.isEnableHidden()).toEqual(true);

    component.topologyStatus.status = 'INACTIVE';
    expect(component.isEnableHidden()).toEqual(false);

    component.topologyStatus.status = 'Stopped';
    expect(component.isEnableHidden()).toEqual(true);

    component.topologyStatus.status = 'KILLED';
    expect(component.isEnableHidden()).toEqual(true);
  }));

  it('should hide disable', async(() => {
    component.topologyStatus.status = 'ACTIVE';
    expect(component.isDisableHidden()).toEqual(false);

    component.topologyStatus.status = 'INACTIVE';
    expect(component.isDisableHidden()).toEqual(true);

    component.topologyStatus.status = 'Stopped';
    expect(component.isDisableHidden()).toEqual(true);

    component.topologyStatus.status = 'KILLED';
    expect(component.isDisableHidden()).toEqual(true);
  }));
});
