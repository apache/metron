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
import { SensorStormSettingsComponent } from './sensor-storm-settings.component';
import { SharedModule } from '../../shared/shared.module';
import { SimpleChanges, SimpleChange } from '@angular/core';
import { SensorParserConfig } from '../../model/sensor-parser-config';
import { SensorStormSettingsModule } from './sensor-storm-settings.module';

describe('Component: SensorStormSettingsComponent', () => {
  let fixture: ComponentFixture<SensorStormSettingsComponent>;
  let component: SensorStormSettingsComponent;
  let sensorParserConfig: SensorParserConfig = new SensorParserConfig();
  sensorParserConfig.sensorTopic = 'bro';
  sensorParserConfig.parserClassName =
    'org.apache.metron.parsers.bro.BasicBroParser';
  sensorParserConfig.parserConfig = {};
  sensorParserConfig.numWorkers = 2;
  sensorParserConfig.numAckers = 2;
  sensorParserConfig.spoutParallelism = 2;
  sensorParserConfig.spoutNumTasks = 2;
  sensorParserConfig.parserParallelism = 2;
  sensorParserConfig.parserNumTasks = 2;
  sensorParserConfig.errorWriterParallelism = 2;
  sensorParserConfig.errorWriterNumTasks = 2;
  sensorParserConfig.spoutConfig = { spoutConfigProp: 'spoutConfigValue1' };
  sensorParserConfig.stormConfig = { stormConfigProp: 'stormConfigValue1' };

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [SharedModule, SensorStormSettingsModule]
    });

    fixture = TestBed.createComponent(SensorStormSettingsComponent);
    component = fixture.componentInstance;
  }));

  it('should create an instance', () => {
    expect(component).toBeDefined();
  });

  it('should create an instance', () => {
    spyOn(component, 'init');
    let changes: SimpleChanges = {
      showStormSettings: new SimpleChange(false, true, true)
    };

    component.ngOnChanges(changes);
    expect(component.init).toHaveBeenCalled();

    changes = { showStormSettings: new SimpleChange(true, false, false) };
    component.ngOnChanges(changes);
    expect(component.init['calls'].count()).toEqual(1);

    fixture.destroy();
  });

  it('should initialise the fields', () => {
    component.init();
    expect(component.newSensorParserConfig).toEqual(new SensorParserConfig());

    component.sensorParserConfig = sensorParserConfig;
    component.init();
    expect(component.newSensorParserConfig).toEqual(sensorParserConfig);
    expect(component.newSpoutConfig).toEqual(
      '{\n\t"spoutConfigProp": "spoutConfigValue1"\n}'
    );
    expect(component.newStormConfig).toEqual(
      '{\n\t"stormConfigProp": "stormConfigValue1"\n}'
    );

    fixture.destroy();
  });

  it('should save the fields', () => {
    spyOn(component.hideStormSettings, 'emit');
    spyOn(component.onStormSettingsChanged, 'emit');
    component.sensorParserConfig = sensorParserConfig;
    component.init();
    component.newSensorParserConfig.numWorkers = 3;
    component.newSensorParserConfig.numAckers = 3;
    component.newSensorParserConfig.spoutParallelism = 3;
    component.newSensorParserConfig.spoutNumTasks = 3;
    component.newSensorParserConfig.parserParallelism = 3;
    component.newSensorParserConfig.parserNumTasks = 3;
    component.newSensorParserConfig.errorWriterParallelism = 3;
    component.newSensorParserConfig.errorWriterNumTasks = 3;
    component.newSpoutConfig = '{"spoutConfigProp": "spoutConfigValue2"}';
    component.newStormConfig = '{"stormConfigProp": "stormConfigValue2"}';
    component.onSave();
    expect(component.sensorParserConfig.numWorkers).toEqual(3);
    expect(component.sensorParserConfig.numAckers).toEqual(3);
    expect(component.sensorParserConfig.spoutParallelism).toEqual(3);
    expect(component.sensorParserConfig.spoutNumTasks).toEqual(3);
    expect(component.sensorParserConfig.parserParallelism).toEqual(3);
    expect(component.sensorParserConfig.parserNumTasks).toEqual(3);
    expect(component.sensorParserConfig.errorWriterParallelism).toEqual(3);
    expect(component.sensorParserConfig.errorWriterNumTasks).toEqual(3);
    expect(component.sensorParserConfig.spoutConfig).toEqual({
      spoutConfigProp: 'spoutConfigValue2'
    });
    expect(component.sensorParserConfig.stormConfig).toEqual({
      stormConfigProp: 'stormConfigValue2'
    });
    expect(component.hideStormSettings.emit).toHaveBeenCalled();
    expect(component.onStormSettingsChanged.emit).toHaveBeenCalled();
  });

  it('hasSpoutConfigChanged should properly detect changes', () => {
    let sensorParserConfigWithSpoutConfig = new SensorParserConfig();
    sensorParserConfigWithSpoutConfig.spoutConfig = {};
    component.sensorParserConfig = sensorParserConfigWithSpoutConfig;
    component.newSpoutConfig = '{}';
    expect(component.hasSpoutConfigChanged()).toEqual(false);

    sensorParserConfigWithSpoutConfig.spoutConfig = { field: 'value' };
    component.sensorParserConfig = sensorParserConfigWithSpoutConfig;
    component.newSpoutConfig = '{ "field"  :  "value" }';
    expect(component.hasSpoutConfigChanged()).toEqual(false);

    sensorParserConfigWithSpoutConfig.spoutConfig = { field: 'value' };
    component.sensorParserConfig = sensorParserConfigWithSpoutConfig;
    component.newSpoutConfig = '{"field": "value2"}';
    expect(component.hasSpoutConfigChanged()).toEqual(true);

    component.newSpoutConfig = '{"field": "value2", }';
    expect(component.hasSpoutConfigChanged()).toEqual(true);
  });

  it('hasStormConfigChanged should properly detect changes', () => {
    let sensorParserConfigWithStormConfig = new SensorParserConfig();
    sensorParserConfigWithStormConfig.stormConfig = {};
    component.sensorParserConfig = sensorParserConfigWithStormConfig;
    component.newStormConfig = '{}';
    expect(component.hasStormConfigChanged()).toEqual(false);

    sensorParserConfigWithStormConfig.stormConfig = { field: 'value' };
    component.sensorParserConfig = sensorParserConfigWithStormConfig;
    component.newStormConfig = '{ "field"  :  "value" }';
    expect(component.hasStormConfigChanged()).toEqual(false);

    sensorParserConfigWithStormConfig.stormConfig = { field: 'value' };
    component.sensorParserConfig = sensorParserConfigWithStormConfig;
    component.newStormConfig = '{"field": "value2"}';
    expect(component.hasStormConfigChanged()).toEqual(true);

    component.newSpoutConfig = '{"field": "value2", }';
    expect(component.hasStormConfigChanged()).toEqual(true);
  });

  it('should hide panel', () => {
    spyOn(component.hideStormSettings, 'emit');

    component.onCancel();

    expect(component.hideStormSettings.emit).toHaveBeenCalled();

    fixture.destroy();
  });
});
