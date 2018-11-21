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
import {ParserConfigModel} from '../sensors/models/parser-config.model';
export class SensorParserConfigHistory {
  sensorName: string;
  createdBy: string;
  modifiedBy: string;
  createdDate: string;
  modifiedByDate: string;
  config: ParserConfigModel;
  status: string;
  latency: string;
  throughput: string;

  constructor() {
    this.config = new ParserConfigModel();
  }

  setConfig(config) {
    this.config = new ParserConfigModel(config);
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
