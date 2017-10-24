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
import {Component, Input, EventEmitter, Output, OnChanges, SimpleChanges} from '@angular/core';
import {SensorParserConfig} from '../../model/sensor-parser-config';

declare var ace: any;

@Component({
  selector: 'metron-config-sensor-storm-settings',
  templateUrl: './sensor-storm-settings.component.html',
  styleUrls: ['./sensor-storm-settings.component.scss']
})

export class SensorStormSettingsComponent implements OnChanges {

  @Input() showStormSettings: boolean;
  @Input() sensorParserConfig: SensorParserConfig;

  @Output() hideStormSettings: EventEmitter<boolean> = new EventEmitter<boolean>();
  @Output() onStormSettingsChanged: EventEmitter<boolean> = new EventEmitter<boolean>();

  newSensorParserConfig: SensorParserConfig = new SensorParserConfig();
  newSpoutConfig: string = '{}';
  newStormConfig: string = '{}';

  ngOnChanges(changes: SimpleChanges) {
    if (changes['showStormSettings'] && changes['showStormSettings'].currentValue) {
      this.init();
    }
  }

  init(): void {
    if (this.sensorParserConfig) {
      this.newSensorParserConfig = Object.assign(new SensorParserConfig(), this.sensorParserConfig);
      this.newSpoutConfig = JSON.stringify(this.sensorParserConfig.spoutConfig, null, '\t');
      this.newStormConfig = JSON.stringify(this.sensorParserConfig.stormConfig, null, '\t');
    }
  }

  onSave() {
    this.sensorParserConfig.numWorkers = this.newSensorParserConfig.numWorkers;
    this.sensorParserConfig.numAckers = this.newSensorParserConfig.numAckers;
    this.sensorParserConfig.spoutParallelism = this.newSensorParserConfig.spoutParallelism;
    this.sensorParserConfig.spoutNumTasks = this.newSensorParserConfig.spoutNumTasks;
    this.sensorParserConfig.parserParallelism = this.newSensorParserConfig.parserParallelism;
    this.sensorParserConfig.parserNumTasks = this.newSensorParserConfig.parserNumTasks;
    this.sensorParserConfig.errorWriterParallelism = this.newSensorParserConfig.errorWriterParallelism;
    this.sensorParserConfig.errorWriterNumTasks = this.newSensorParserConfig.errorWriterNumTasks;
    this.sensorParserConfig.spoutConfig = JSON.parse(this.newSpoutConfig);
    this.sensorParserConfig.stormConfig = JSON.parse(this.newStormConfig);
    this.hideStormSettings.emit(true);
    this.onStormSettingsChanged.emit(true);
  }

  onCancel(): void {
    this.hideStormSettings.emit(true);
  }

  hasSpoutConfigChanged(): boolean {
    try {
      // serialize/deserialize to ignore formatting differences
      return JSON.stringify(JSON.parse(this.newSpoutConfig), null, '\t') !==
          JSON.stringify(this.sensorParserConfig.spoutConfig, null, '\t');
    } catch (err) {
      // malformed json means it is being edited
      return true;
    }
  }

  hasStormConfigChanged(): boolean {
    try {
      // serialize/deserialize to ignore formatting differences
      return JSON.stringify(JSON.parse(this.newStormConfig), null, '\t') !==
          JSON.stringify(this.sensorParserConfig.stormConfig, null, '\t');
    } catch (err) {
      // malformed json means it is being edited
      return true;
    }
  }
}
