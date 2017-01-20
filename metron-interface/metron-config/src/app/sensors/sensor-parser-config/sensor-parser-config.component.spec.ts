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
import {Inject} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {Router, ActivatedRoute, Params} from '@angular/router';
import {Http, RequestOptions, Response, ResponseOptions} from '@angular/http';
import {SensorParserConfigComponent, Pane, KafkaStatus} from './sensor-parser-config.component';
import {TransformationValidationService} from '../../service/transformation-validation.service';
import {SensorParserConfigService} from '../../service/sensor-parser-config.service';
import {KafkaService} from '../../service/kafka.service';
import {KafkaTopic} from '../../model/kafka-topic';
import {GrokValidationService} from '../../service/grok-validation.service';
import {MetronAlerts} from '../../shared/metron-alerts';
import {SensorParserConfig} from '../../model/sensor-parser-config';
import {SensorEnrichments} from '../../model/sensor-enrichments';
import {ParseMessageRequest} from '../../model/parse-message-request';
import {TransformationValidation} from '../../model/transformation-validation';
import {AuthenticationService} from '../../service/authentication.service';
import {FieldTransformer} from '../../model/field-transformer';
import {SensorParserConfigModule} from './sensor-parser-config.module';
import {SensorEnrichmentConfigService} from '../../service/sensor-enrichment-config.service';
import {SensorEnrichmentConfig, EnrichmentConfig, ThreatIntelConfig} from '../../model/sensor-enrichment-config';
import {APP_CONFIG, METRON_REST_CONFIG} from '../../app.config';
import {IAppConfig} from '../../app.config.interface';
import {SensorIndexingConfigService} from '../../service/sensor-indexing-config.service';
import {SensorIndexingConfig} from '../../model/sensor-indexing-config';
import '../../rxjs-operators';
import 'rxjs/add/observable/of';


class MockRouter {
  navigateByUrl(url: string) {}
}

class MockActivatedRoute {
  private name: string;
  params: Observable<Params>;

  setNameForTest(name: string) {
    this.name = name;
    this.params = Observable.create(observer => {
      observer.next({id: this.name});
      observer.complete();
    });
  }
}

class MockSensorParserConfigService extends SensorParserConfigService {
  private sensorParserConfigPost: SensorParserConfig;
  private sensorParserConfig: SensorParserConfig;
  private parsedMessage: any;

  constructor(private http2: Http, @Inject(APP_CONFIG) private config2: IAppConfig) {
    super(http2, config2);
  }

  public post(sensorParserConfig: SensorParserConfig): Observable<SensorParserConfig> {
    this.sensorParserConfigPost = sensorParserConfig;
    return Observable.create(observer => {
      observer.next({});
      observer.complete();
    });
  }

  public get(name: string): Observable<SensorParserConfig> {
    return Observable.create(observer => {
      observer.next(this.sensorParserConfig);
      observer.complete();
    });
  }

  public getAvailableParsers(): Observable<{}> {
    return Observable.create(observer => {
      observer.next({
        'Bro': 'org.apache.metron.parsers.bro.BasicBroParser',
        'Grok': 'org.apache.metron.parsers.GrokParser'
      });
      observer.complete();
    });
  }

  public parseMessage(parseMessageRequest: ParseMessageRequest): Observable<{}> {
    return Observable.create(observer => {
      observer.next(this.parsedMessage);
      observer.complete();
    });
  }

  public setSensorParserConfig(result: any) {
    this.sensorParserConfig = result;
  }


  public getSensorParserConfigPost(): SensorParserConfig {
    return this.sensorParserConfigPost;
  }

  public setParsedMessage(parsedMessage: any) {
    this.parsedMessage = parsedMessage;
  }
}

class MockSensorIndexingConfigService extends SensorIndexingConfigService {
  private sensorIndexingConfigPost: SensorIndexingConfig;
  private sensorIndexingConfig: SensorIndexingConfig;

  constructor(private http2: Http, @Inject(APP_CONFIG) private config2: IAppConfig) {
    super(http2, config2);
  }

  public post(name: string, sensorIndexingConfig: SensorIndexingConfig): Observable<SensorIndexingConfig> {
    this.sensorIndexingConfigPost = sensorIndexingConfig;
    return Observable.create(observer => {
      observer.next({});
      observer.complete();
    });
  }

  public get(name: string): Observable<SensorIndexingConfig> {
    return Observable.create(observer => {
      observer.next(this.sensorIndexingConfig);
      observer.complete();
    });
  }

  public setSensorIndexingConfig(result: any) {
    this.sensorIndexingConfig = result;
  }


  public getSensorIndexingConfigPost(): SensorIndexingConfig {
    return this.sensorIndexingConfigPost;
  }
}

class MockKafkaService extends KafkaService {
  private kafkaTopic: KafkaTopic;
  private kafkaTopicForPost: KafkaTopic;
  private sampleData = {'key1': 'value1', 'key2': 'value2'};

  constructor(private http2: Http, @Inject(APP_CONFIG) private config2: IAppConfig) {
    super(http2, config2);
  }

  public setForTest(kafkaTopic: KafkaTopic) {
    this.kafkaTopic = kafkaTopic;
  }

  public setForSample(sampleData?: any) {
    this.sampleData = sampleData;
  }

  public sample(name: string): Observable<string> {
    if (this.sampleData === null) {
      return Observable.throw('Error');
    }
    return Observable.create(observer => {
      observer.next(JSON.stringify(this.sampleData));
      observer.complete();
    });
  }

  public get(name: string): Observable<KafkaTopic> {
    if (this.kafkaTopic === null) {
      return Observable.throw('Error');
    }
    return Observable.create(observer => {
      observer.next(this.kafkaTopic);
      observer.complete();
    });
  }

  public post(k: KafkaTopic): Observable<KafkaTopic> {
    this.kafkaTopicForPost = k;
    console.log('called post MockKafkaService: ' + this.kafkaTopicForPost);
    return Observable.create(observer => {
      observer.next({});
      observer.complete();
    });
  }

  public getKafkaTopicForPost(): KafkaTopic {
    console.log('Called get MockKafkaService: ' + this.kafkaTopicForPost);
    return this.kafkaTopicForPost;
  }
}

class MockGrokValidationService extends GrokValidationService {

  constructor(private http2: Http, @Inject(APP_CONFIG) private config2: IAppConfig) {
    super(http2, config2);
  }

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

class MockAuthenticationService extends AuthenticationService {

  constructor(private http2: Http, private router2: Router, @Inject(APP_CONFIG) private config2: IAppConfig) {
    super(http2, router2, config2);
  }

  public getCurrentUser(options: RequestOptions): Observable<Response> {
    let responseOptions: ResponseOptions = new ResponseOptions();
    responseOptions.body = 'user';
    let response: Response = new Response(responseOptions);
    return Observable.of(response);
  };
}

class MockTransformationValidationService extends TransformationValidationService {

  private transformationValidationResult: any;
  private transformationValidationForValidate: TransformationValidation;

  constructor(private http2: Http, @Inject(APP_CONFIG) private config2: IAppConfig) {
    super(http2, config2);
  }

  public setTransformationValidationResultForTest(transformationValidationResult: any): void {
    this.transformationValidationResult = transformationValidationResult;
  }

  public getTransformationValidationForValidate(): TransformationValidation {
    return this.transformationValidationForValidate;
  }

  public validate(t: TransformationValidation): Observable<{}> {
    this.transformationValidationForValidate = t;
    return Observable.create(observer => {
      observer.next(this.transformationValidationResult);
      observer.complete();
    });
  }
}

export class MockSensorEnrichmentConfigService {

  private broEnrichments = new SensorEnrichmentConfig();
  private squidEnrichments = new SensorEnrichmentConfig();

  constructor() {
    let broEnrichment = {'fieldMap': {
                          'geo': ['ip_dst_addr'],
                          'host': ['ip_dst_addr'],
                          'whois': [],
                          'stellar': { 'config': { 'group1': {} }}
                          },
                        'fieldToTypeMap': {}, 'config': {}
                        };
    let broThreatIntel = {'threatIntel': {
                            'fieldMap': { 'hbaseThreatIntel': ['ip_dst_addr'] },
                            'fieldToTypeMap': { 'ip_dst_addr': ['malicious_ip'] }
                            }
                          };
    let squidEnrichments = {'fieldMap': {
                              'geo': ['ip_dst_addr'],
                              'host': ['ip_dst_addr'],
                              'whois': [],
                              'stellar': { 'config': { 'group1': {} }}
                              },
                              'fieldToTypeMap': {}, 'config': {}
                            };
    let squidThreatIntel = {'threatIntel': {
                              'fieldMap': { 'hbaseThreatIntel': ['ip_dst_addr'] },
                              'fieldToTypeMap': { 'ip_dst_addr': ['malicious_ip'] }
                              }
                            };

    this.broEnrichments = new SensorEnrichmentConfig();
    this.broEnrichments.enrichment = Object.assign(new EnrichmentConfig(),  broEnrichment);
    this.broEnrichments.threatIntel = Object.assign(new ThreatIntelConfig(), broThreatIntel);
    this.squidEnrichments = new SensorEnrichmentConfig();
    this.squidEnrichments.enrichment = Object.assign(new EnrichmentConfig(),  squidEnrichments);
    this.squidEnrichments.threatIntel = Object.assign(new ThreatIntelConfig(), squidThreatIntel);
  }

  public get(name: string): Observable<SensorEnrichments> {
    if (name === 'bro') {
      return Observable.create(observer => {
        observer.next(this.broEnrichments);
        observer.complete();
      });
    } else {
      return Observable.create(observer => {
        observer.next(this.squidEnrichments);
        observer.complete();
      });
    }
  }

  public post(sensorEnrichments: SensorEnrichments): Observable<SensorEnrichments> {
    return  Observable.create(observer => {
      observer.next(sensorEnrichments);
      observer.complete();
    });
  }
}

describe('Component: SensorParserConfig', () => {

  let comp: SensorParserConfigComponent;
  let fixture: ComponentFixture<SensorParserConfigComponent>;
  let sensorParserConfigService: MockSensorParserConfigService;
  let sensorIndexingConfigService: MockSensorIndexingConfigService;
  let transformationValidationService: MockTransformationValidationService;
  let kafkaService: MockKafkaService;
  let grokValidationService: MockGrokValidationService;
  let activatedRoute: MockActivatedRoute;
  let metronAlerts: MetronAlerts;
  let router: MockRouter;
  let sensorEnrichmentsService: MockSensorEnrichmentConfigService;

  let squidSensorData: any = {
    'parserClassName': 'org.apache.metron.parsers.GrokParser',
    'sensorTopic': 'squid',
    'parserConfig': {
      'grokPath': '/patterns/squid',
      'patternLabel': 'SQUID_DELIMITED',
      'timestampField': 'timestamp'
    },
    'fieldTransformations': [
      {
        'input': [],
        'output': ['full_hostname', 'domain_without_subdomains', 'hostname'],
        'transformation': 'STELLAR',
        'config': {
          'full_hostname': 'URL_TO_HOST(url)',
          'domain_without_subdomains': 'DOMAIN_REMOVE_SUBDOMAINS(full_hostname)'
        }
      }
    ],
  };

  let squidIndexingConfig = {
    'index': 'squid',
    'batchSize': 5
  };

  let squidKafkaData: any = {
    'name': 'squid',
    'numPartitions': 1,
    'replicationFactor': 1
  };

  let broKafkaData: any = {
    'name': 'squid',
    'numPartitions': 4,
    'replicationFactor': 4
  };

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      imports: [SensorParserConfigModule],
      providers: [
        MetronAlerts,
        {provide: Http},
        {provide: SensorParserConfigService, useClass: MockSensorParserConfigService},
        {provide: SensorIndexingConfigService, useClass: MockSensorIndexingConfigService},
        {provide: KafkaService, useClass: MockKafkaService},
        {provide: GrokValidationService, useClass: MockGrokValidationService},
        {provide: TransformationValidationService, useClass: MockTransformationValidationService},
        {provide: ActivatedRoute, useClass: MockActivatedRoute},
        {provide: Router, useClass: MockRouter},
        {provide: AuthenticationService, useClass: MockAuthenticationService},
        {provide: SensorEnrichmentConfigService, useClass: MockSensorEnrichmentConfigService},
        {provide: APP_CONFIG, useValue: METRON_REST_CONFIG}
      ]
    }).compileComponents()
      .then(() => {
        fixture = TestBed.createComponent(SensorParserConfigComponent);
        comp = fixture.componentInstance;
        sensorParserConfigService = fixture.debugElement.injector.get(SensorParserConfigService);
        sensorIndexingConfigService = fixture.debugElement.injector.get(SensorIndexingConfigService);
        transformationValidationService = fixture.debugElement.injector.get(TransformationValidationService);
        kafkaService = fixture.debugElement.injector.get(KafkaService);
        grokValidationService = fixture.debugElement.injector.get(GrokValidationService);
        sensorEnrichmentsService = fixture.debugElement.injector.get(SensorEnrichmentConfigService);
        activatedRoute = fixture.debugElement.injector.get(ActivatedRoute);
        metronAlerts = fixture.debugElement.injector.get(MetronAlerts);
        router = fixture.debugElement.injector.get(Router);
      });

  }));

  it('should create an instance of SensorParserConfigComponent', async(() => {

    let component: SensorParserConfigComponent = fixture.componentInstance;
    expect(component).toBeDefined();

    fixture.destroy();
  }));

  it('should create edit forms for SensorParserConfigComponent', async(() => {
    activatedRoute.setNameForTest('squid');
    sensorParserConfigService.setSensorParserConfig(Object.assign(new SensorParserConfig(), squidSensorData));
    sensorIndexingConfigService.setSensorIndexingConfig(Object.assign(new SensorIndexingConfig(), squidIndexingConfig));
    let kafkaTopic = Object.assign(new KafkaTopic(), squidKafkaData);
    kafkaService.setForTest(kafkaTopic);

    let component: SensorParserConfigComponent = fixture.componentInstance;
    component.ngOnInit();

    expect(Object.keys(component.sensorConfigForm.controls).length).toEqual(8);
    expect(Object.keys(component.transformsValidationForm.controls).length).toEqual(2);
    expect(component.availableParsers).toEqual({
      'Bro': 'org.apache.metron.parsers.bro.BasicBroParser',
      'Grok': 'org.apache.metron.parsers.GrokParser'
    });
    expect(component.availableParserNames).toEqual(['Bro', 'Grok']);

    fixture.destroy();
  }));

  it('should create new forms for SensorParserConfigComponent', async(() => {
    activatedRoute.setNameForTest('new');
    let testTopic: KafkaTopic = Object.assign(new KafkaTopic(), squidKafkaData);
    kafkaService.setForTest(testTopic);

    let component: SensorParserConfigComponent = fixture.componentInstance;
    component.ngOnInit();

    expect(Object.keys(component.sensorConfigForm.controls).length).toEqual(8);
    expect(Object.keys(component.transformsValidationForm.controls).length).toEqual(2);
    component.sensorParserConfig.sensorTopic = 'squid';
    component.onSetSensorName();

    fixture.destroy();
  }));

  it('should return Transforms information with formatting ', async(() => {
    activatedRoute.setNameForTest('squid');
    kafkaService.setForTest(Object.assign(new KafkaTopic(), broKafkaData));
    sensorParserConfigService.setSensorParserConfig(Object.assign(new SensorParserConfig(), squidSensorData));
    sensorIndexingConfigService.setSensorIndexingConfig(Object.assign(new SensorIndexingConfig(), squidIndexingConfig));

    let component: SensorParserConfigComponent = fixture.componentInstance;
    component.ngOnInit();
    expect(component.getTransforms()).toEqual('3 Transformations Applied');

    fixture.destroy();
  }));

  it('should call window history back', async(() => {
    activatedRoute.setNameForTest('new');
    let component: SensorParserConfigComponent = fixture.componentInstance;

    router.navigateByUrl = jasmine.createSpy('navigateByUrl');
    component.goBack();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/sensors');

  }));

  it('should save sensor configuration', async(() => {
    let fieldTransformer = Object.assign(new FieldTransformer(), {
      'input': [],
      'output': ['url_host'],
      'transformation': 'MTL',
      'config': {'url_host': 'TO_LOWER(URL_TO_HOST(url))'}
    });
    let sensorParserConfigSave: SensorParserConfig = new SensorParserConfig();
    sensorParserConfigSave.sensorTopic = 'squid';
    sensorParserConfigSave.parserClassName = 'org.apache.metron.parsers.GrokParser';
    sensorParserConfigSave.parserConfig = {};
    sensorParserConfigSave.parserConfig['grokStatement'] = '%{NUMBER:timestamp}';
    sensorParserConfigSave.fieldTransformations = [fieldTransformer];
    activatedRoute.setNameForTest('new');

    let sensorParserConfigServiceStatus = true;
    spyOn(sensorParserConfigService, 'post').and.callFake(() => {
      if (sensorParserConfigServiceStatus) {
        return Observable.create(observer => {
          observer.next(sensorParserConfigSave);
          observer.complete();
        });
      } else {
        return Observable.throw('Error');
      }

    });

    let sensorEnrichmentsServiceStatus = true;
    spyOn(sensorEnrichmentsService, 'post').and.callFake((sensorEnrichments: SensorEnrichments) => {
      if (sensorEnrichmentsServiceStatus) {
        return  Observable.create(observer => {
          observer.next(sensorEnrichments);
          observer.complete();
        });
      } else {
        return Observable.throw('Error');
      }
    });

    spyOn(metronAlerts, 'showSuccessMessage');
    spyOn(metronAlerts, 'showErrorMessage');

    let component: SensorParserConfigComponent = fixture.componentInstance;

    component.sensorParserConfig.sensorTopic = 'squid';
    component.sensorParserConfig.parserClassName = 'org.apache.metron.parsers.GrokParser';
    component.sensorParserConfig.fieldTransformations = [fieldTransformer];
    component.sensorParserConfig.parserConfig['grokStatement'] = '%{NUMBER:timestamp}';

    expect(component.sensorParserConfig.parserConfig['grokStatement']).toEqual('%{NUMBER:timestamp}');

    component.onSave();
    expect(sensorParserConfigService.post).toHaveBeenCalledWith(sensorParserConfigSave);
    expect(metronAlerts.showSuccessMessage).toHaveBeenCalledWith('Created Sensor squid');
    expect(sensorEnrichmentsService.post).toHaveBeenCalled();

    sensorParserConfigServiceStatus = false;
    component.onSave();
    expect(metronAlerts.showErrorMessage['calls'].mostRecent().args[0]).toEqual('Unable to save sensor config: Error');

    sensorParserConfigServiceStatus = true;
    sensorEnrichmentsServiceStatus = false;
    component.onSave();
    let expected = 'Created Sensor parser config but unable to save enrichment configuration: Error';
    expect(metronAlerts.showErrorMessage['calls'].mostRecent().args[0]).toEqual(expected);

    fixture.destroy();
  }));

  it('should getTransformationCount', async(() => {
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

    let component: SensorParserConfigComponent = fixture.componentInstance;

    expect(component.getTransformationCount()).toEqual(0);

    fixture.componentInstance.sensorParserConfig.fieldTransformations = transforms;
    expect(component.getTransformationCount()).toEqual(3);

    fixture.componentInstance.sensorParserConfig.fieldTransformations = [transforms[0]];
    expect(component.getTransformationCount()).toEqual(0);
    fixture.destroy();
  }));

  it('should getEnrichmentCount', async(() => {
    let component: SensorParserConfigComponent = fixture.componentInstance;

    component.sensorEnrichmentConfig.enrichment.fieldMap['geo'] = ['ip_src_addr', 'ip_dst_addr'];
    component.sensorEnrichmentConfig.enrichment.fieldToTypeMap['hbaseenrichment'] = ['ip_src_addr', 'ip_dst_addr'];

    expect(component.getEnrichmentCount()).toEqual(4);

    fixture.destroy();
  }));

  it('should getThreatIntelCount', async(() => {
    let component: SensorParserConfigComponent = fixture.componentInstance;

    component.sensorEnrichmentConfig.threatIntel.fieldToTypeMap['hbaseenrichment'] = ['ip_src_addr', 'ip_dst_addr'];

    expect(component.getThreatIntelCount()).toEqual(2);

    fixture.destroy();
  }));

  it('should showPane', async(() => {
    let component: SensorParserConfigComponent = fixture.componentInstance;

    component.showPane(Pane.GROK);
    expect(component.showGrokValidator).toEqual(true);
    expect(component.showFieldSchema).toEqual(false);
    expect(component.showRawJson).toEqual(false);

    component.showPane(Pane.FIELDSCHEMA);
    expect(component.showGrokValidator).toEqual(false);
    expect(component.showFieldSchema).toEqual(true);
    expect(component.showRawJson).toEqual(false);

    component.showPane(Pane.RAWJSON);
    expect(component.showGrokValidator).toEqual(false);
    expect(component.showFieldSchema).toEqual(false);
    expect(component.showRawJson).toEqual(true);

    fixture.destroy();
  }));

  it('should hidePane', async(() => {
    let component: SensorParserConfigComponent = fixture.componentInstance;

    component.hidePane(Pane.GROK);
    expect(component.showGrokValidator).toEqual(false);
    expect(component.showFieldSchema).toEqual(false);
    expect(component.showRawJson).toEqual(false);

    component.hidePane(Pane.FIELDSCHEMA);
    expect(component.showGrokValidator).toEqual(false);
    expect(component.showFieldSchema).toEqual(false);
    expect(component.showRawJson).toEqual(false);

    component.hidePane(Pane.RAWJSON);
    expect(component.showGrokValidator).toEqual(false);
    expect(component.showFieldSchema).toEqual(false);
    expect(component.showRawJson).toEqual(false);

    fixture.destroy();
  }));

  it('should hidePane', async(() => {
    let component: SensorParserConfigComponent = fixture.componentInstance;
    spyOn(component.sensorFieldSchema, 'createFieldSchemaRows');

    component.onRawJsonChanged();

    expect(component.sensorFieldSchema.createFieldSchemaRows).toHaveBeenCalled();

    fixture.destroy();
  }));

  it('should set patternLabel', async(() => {
    let component: SensorParserConfigComponent = fixture.componentInstance;
    component.sensorParserConfig = new SensorParserConfig();
    component.sensorParserConfig.sensorTopic = 'Topic1';
    component.sensorParserConfig.parserClassName = 'org.apache.metron.parsers.GrokParser';
    component.onParserTypeChange();
    expect(component.sensorParserConfig.parserConfig['patternLabel']).toEqual('TOPIC1');

    component.sensorParserConfig.parserConfig['patternLabel'] = null;
    component.onParserTypeChange();
    expect(component.sensorParserConfig.parserConfig['patternLabel']).toEqual('TOPIC1');

    component.onParserTypeChange();
    expect(component.sensorParserConfig.parserConfig['patternLabel']).toEqual('TOPIC1');

    fixture.destroy();
  }));

  it('should check isGrokParser', async(() => {
    let component: SensorParserConfigComponent = fixture.componentInstance;
    component.sensorParserConfig = new SensorParserConfig();

    component.sensorParserConfig.parserClassName = 'org.apache.metron.parsers.GrokParser';
    expect(component.isGrokParser()).toEqual(true);

    component.sensorParserConfig.parserClassName = 'org.apache.metron.parsers.GrokParserTest';
    expect(component.isGrokParser()).toEqual(false);

    fixture.destroy();
  }));

  it('should return  getKafkaStatus', async(() => {
    let component: SensorParserConfigComponent = fixture.componentInstance;
    component.sensorParserConfig = new SensorParserConfig();

    component.getKafkaStatus();
    expect(component.currentKafkaStatus).toEqual(null);

    component.sensorParserConfig.sensorTopic = '';
    component.getKafkaStatus();
    expect(component.currentKafkaStatus).toEqual(null);

    let kafkaTopic = Object.assign(new KafkaTopic(), squidKafkaData);
    kafkaService.setForTest(kafkaTopic);
    component.sensorParserConfig.sensorTopic = 'SQUID';
    component.getKafkaStatus();
    expect(component.currentKafkaStatus).toEqual(KafkaStatus.EMITTING);

    kafkaService.setForSample();
    kafkaService.setForTest(kafkaTopic);
    component.sensorParserConfig.sensorTopic = 'SQUID';
    component.getKafkaStatus();
    expect(component.currentKafkaStatus).toEqual(KafkaStatus.NOT_EMITTING);

    kafkaService.setForTest(null);
    component.sensorParserConfig.sensorTopic = 'SQUID';
    component.getKafkaStatus();
    expect(component.currentKafkaStatus).toEqual(KafkaStatus.NO_TOPIC);

    kafkaService.setForSample(null);
    kafkaService.setForTest(kafkaTopic);
    component.sensorParserConfig.sensorTopic = 'SQUID';
    component.getKafkaStatus();
    expect(component.currentKafkaStatus).toEqual(KafkaStatus.NOT_EMITTING);


    fixture.destroy();
  }));

});
