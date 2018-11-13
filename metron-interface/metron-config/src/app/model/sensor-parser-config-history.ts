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
import {SensorParserConfig} from './sensor-parser-config';
import { ParserConfigListItem } from 'app/sensors/sensor-aggregate/parser-config-item';

export class SensorParserConfigHistory implements ParserConfigListItem {
  sensorName: string;
  createdBy: string;
  modifiedBy: string;
  createdDate: string;
  modifiedByDate: string;
  config: SensorParserConfig;
  status: string;
  latency: string;
  throughput: string;

  constructor() {
    this.config = new SensorParserConfig();
  }

  getName() {
    return this.sensorName;
  }
  setName(value: string) {
    this.sensorName = value;
  }

  getConfig() {
    return this.config;
  }
  setConfig(config) {
    this.config = new SensorParserConfig(config);
  }

  getStatus(): string {
    return this.status;
  }
  setStatus(value: string): void {
    this.status = value;
  }

  // FIXME find a place for this data, seems odd
  getHistory(): { latency: string, throughput: string, modifiedByDate: string, modifiedBy: string } {
    return { latency: this.latency, throughput: this.throughput, modifiedByDate: this.modifiedByDate, modifiedBy: this.modifiedBy };
  }
  setHistory(value: { latency: string, throughput: string, modifiedByDate: string, modifiedBy: string }): void {
    this.latency = value.latency;
    this.throughput = value.throughput;
    this.modifiedByDate = value.modifiedByDate;
    this.modifiedBy = value.modifiedBy;
  }

  toJson(): string {
    return JSON.stringify(this.config);
  }

  clone(): SensorParserConfigHistory {
    const clone = new SensorParserConfigHistory();

    clone.sensorName = this.sensorName;
    clone.createdBy = this.createdBy;
    clone.modifiedBy = this.modifiedBy;
    clone.createdDate = this.createdDate;
    clone.modifiedByDate = this.modifiedByDate;
    clone.config = this.config.clone();
    clone.status = this.status;
    clone.latency = this.latency;
    clone.throughput = this.throughput;

    return clone;
  }
}
