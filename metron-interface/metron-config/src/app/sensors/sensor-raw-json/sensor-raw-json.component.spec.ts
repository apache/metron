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

import { async, TestBed, ComponentFixture } from '@angular/core/testing';
import { SensorRawJsonComponent } from './sensor-raw-json.component';
import { SharedModule } from '../../shared/shared.module';
import { SimpleChanges, SimpleChange } from '@angular/core';
import { SensorParserConfig } from '../../model/sensor-parser-config';
import {
  SensorEnrichmentConfig,
  EnrichmentConfig,
  ThreatIntelConfig
} from '../../model/sensor-enrichment-config';
import { SensorRawJsonModule } from './sensor-raw-json.module';
import { IndexingConfigurations } from '../../model/sensor-indexing-config';

describe('Component: SensorRawJsonComponent', () => {
  let fixture: ComponentFixture<SensorRawJsonComponent>;
  let component: SensorRawJsonComponent;
  let sensorParserConfigString =
    '{"parserClassName":"org.apache.metron.parsers.bro.BasicBroParser","sensorTopic":"bro",' +
    '"parserConfig": {},"fieldTransformations":[]}';
  let sensorParserConfig: SensorParserConfig = new SensorParserConfig();
  sensorParserConfig.sensorTopic = 'bro';
  sensorParserConfig.parserClassName =
    'org.apache.metron.parsers.bro.BasicBroParser';
  sensorParserConfig.parserConfig = {};

  let sensorParserConfigWithClassNameString = `{"parserClassName":"org.apache.metron.parsers.bro.BasicBroParser","sensorTopic":"bro",
        "parserConfig": {},"fieldTransformations":[], "writerClassName": "org.example.writerClassName",
        "errorWriterClassName": "org.example.errorWriterClassName",
        "filterClassName": "org.example.filterClassName", "invalidWriterClassName": "org.example.invalidWriterClassName"}`;
  let sensorParserConfigWithClassName = Object.assign(
    new SensorParserConfig(),
    sensorParserConfig
  );
  sensorParserConfigWithClassName.writerClassName =
    'org.example.writerClassName';
  sensorParserConfigWithClassName.errorWriterClassName =
    'org.example.errorWriterClassName';
  sensorParserConfigWithClassName.filterClassName =
    'org.example.filterClassName';
  sensorParserConfigWithClassName.invalidWriterClassName =
    'org.example.invalidWriterClassName';

  let sensorEnrichmentConfigString =
    '{"enrichment" : {"fieldMap": ' +
    '{"geo": ["ip_dst_addr", "ip_src_addr"],"host": ["host"]}},"threatIntel": {"fieldMap": {"hbaseThreatIntel":' +
    ' ["ip_src_addr", "ip_dst_addr"]},"fieldToTypeMap": {"ip_src_addr" : ["malicious_ip"],"ip_dst_addr" : ["malicious_ip"]}}}';
  let sensorEnrichmentConfig = new SensorEnrichmentConfig();
  sensorEnrichmentConfig.enrichment = Object.assign(new EnrichmentConfig(), {
    fieldMap: {
      geo: ['ip_dst_addr', 'ip_src_addr'],
      host: ['host']
    }
  });
  sensorEnrichmentConfig.threatIntel = Object.assign(new ThreatIntelConfig(), {
    fieldMap: {
      hbaseThreatIntel: ['ip_src_addr', 'ip_dst_addr']
    },
    fieldToTypeMap: {
      ip_src_addr: ['malicious_ip'],
      ip_dst_addr: ['malicious_ip']
    }
  });

  let sensorEnrichmentConfigWithConfigString = `{"configuration": "some-configuration",
         "enrichment" : {"fieldMap": {"geo": ["ip_dst_addr", "ip_src_addr"],"host": ["host"]}},
         "threatIntel": {"fieldMap": {"hbaseThreatIntel":["ip_src_addr", "ip_dst_addr"]},
         "fieldToTypeMap": {"ip_src_addr" : ["malicious_ip"],"ip_dst_addr" : ["malicious_ip"]}}}`;
  let sensorEnrichmentConfigWithConfig = Object.assign(
    new SensorEnrichmentConfig(),
    sensorEnrichmentConfig
  );
  sensorEnrichmentConfigWithConfig.configuration = 'some-configuration';

  let sensorIndexingConfigString = `{"hdfs": {"index": "bro","batchSize": 5,"enabled":true},
    "elasticsearch": {"index": "bro","batchSize": 5,"enabled":true},
    "solr": {"index": "bro","batchSize": 5,"enabled":true}}`;
  let sensorIndexingConfig = new IndexingConfigurations();
  sensorIndexingConfig.hdfs.index = 'bro';
  sensorIndexingConfig.hdfs.batchSize = 5;
  sensorIndexingConfig.hdfs.enabled = true;
  sensorIndexingConfig.elasticsearch.index = 'bro';
  sensorIndexingConfig.elasticsearch.batchSize = 5;
  sensorIndexingConfig.elasticsearch.enabled = true;
  sensorIndexingConfig.solr.index = 'bro';
  sensorIndexingConfig.solr.batchSize = 5;
  sensorIndexingConfig.solr.enabled = true;

  let sensorIndexingConfigChangedString = `{"hdfs": {"index": "squid","batchSize": 1,"enabled":true},
    "elasticsearch": {"index": "squid","batchSize": 1,"enabled":true},
    "solr": {"index": "squid","batchSize": 1,"enabled":true}}`;
  let sensorIndexingConfigChanged = new IndexingConfigurations();
  sensorIndexingConfigChanged.hdfs.index = 'squid';
  sensorIndexingConfigChanged.hdfs.batchSize = 1;
  sensorIndexingConfigChanged.hdfs.enabled = true;
  sensorIndexingConfigChanged.elasticsearch.index = 'squid';
  sensorIndexingConfigChanged.elasticsearch.batchSize = 1;
  sensorIndexingConfigChanged.elasticsearch.enabled = true;
  sensorIndexingConfigChanged.solr.index = 'squid';
  sensorIndexingConfigChanged.solr.batchSize = 1;
  sensorIndexingConfigChanged.solr.enabled = true;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [SharedModule, SensorRawJsonModule]
    });
    fixture = TestBed.createComponent(SensorRawJsonComponent);
    component = fixture.componentInstance;
  }));

  it('should create an instance', () => {
    expect(component).toBeDefined();
  });

  it('should create an instance', () => {
    spyOn(component, 'init');
    let changes: SimpleChanges = {
      showRawJson: new SimpleChange(false, true, true)
    };

    component.ngOnChanges(changes);
    expect(component.init).toHaveBeenCalled();

    changes = { showRawJson: new SimpleChange(true, false, false) };
    component.ngOnChanges(changes);
    expect(component.init['calls'].count()).toEqual(1);

    fixture.destroy();
  });

  it('should initialise the fields', () => {
    component.init();
    expect(component.newSensorParserConfig).toEqual(undefined);
    expect(component.newSensorEnrichmentConfig).toEqual(undefined);
    expect(component.newIndexingConfigurations).toEqual(undefined);

    component.sensorParserConfig = sensorParserConfig;
    component.sensorEnrichmentConfig = sensorEnrichmentConfig;
    component.indexingConfigurations = sensorIndexingConfig;
    component.init();
    expect(component.newSensorParserConfig).toEqual(
      JSON.stringify(sensorParserConfig, null, '\t')
    );
    expect(component.newSensorEnrichmentConfig).toEqual(
      JSON.stringify(sensorEnrichmentConfig, null, '\t')
    );
    expect(component.newIndexingConfigurations).toEqual(
      JSON.stringify(sensorIndexingConfig, null, '\t')
    );

    fixture.destroy();
  });

  it('should save the fields', () => {
    spyOn(component.hideRawJson, 'emit');
    spyOn(component.onRawJsonChanged, 'emit');
    component.sensorParserConfig = new SensorParserConfig();
    component.sensorEnrichmentConfig = new SensorEnrichmentConfig();
    component.indexingConfigurations = new IndexingConfigurations();

    component.newSensorParserConfig = sensorParserConfigString;
    component.newSensorEnrichmentConfig = sensorEnrichmentConfigString;
    component.newIndexingConfigurations = sensorIndexingConfigString;
    component.onSave();
    expect(component.sensorParserConfig).toEqual(sensorParserConfig);
    expect(component.sensorEnrichmentConfig).toEqual(sensorEnrichmentConfig);
    expect(component.indexingConfigurations).toEqual(sensorIndexingConfig);
    expect(component.hideRawJson.emit).toHaveBeenCalled();
    expect(component.onRawJsonChanged.emit).toHaveBeenCalled();

    component.newSensorParserConfig = sensorParserConfigWithClassNameString;
    component.newSensorEnrichmentConfig = sensorEnrichmentConfigWithConfigString;
    component.newIndexingConfigurations = sensorIndexingConfigChangedString;
    component.onSave();
    expect(component.sensorParserConfig).toEqual(
      sensorParserConfigWithClassName
    );
    expect(component.sensorEnrichmentConfig).toEqual(
      sensorEnrichmentConfigWithConfig
    );
    expect(component.indexingConfigurations).toEqual(
      sensorIndexingConfigChanged
    );
    expect(component.hideRawJson.emit['calls'].count()).toEqual(2);
    expect(component.onRawJsonChanged.emit['calls'].count()).toEqual(2);

    fixture.destroy();
  });

  it('should hide panel', () => {
    spyOn(component.hideRawJson, 'emit');

    component.onCancel();

    expect(component.hideRawJson.emit).toHaveBeenCalled();

    fixture.destroy();
  });
});
