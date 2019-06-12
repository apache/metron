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
import { ParserModel } from './parser.model';
import * as cloneDeep from 'clone-deep';

export class ParserGroupModel implements ParserModel {
  name: string;
  description: string;
  sensors: string[];

  constructor(rawJson: object) {
    if (rawJson['name']) {
      this.name = rawJson['name'];
    } else {
      throw new Error('Json response not contains name');
    }
    this.description = rawJson['description'] || '';
    this.sensors = rawJson['sensors'] || [];
  }

  clone(rawJson): ParserGroupModel {
    return new ParserGroupModel({
      ...cloneDeep(this),
      ...rawJson,
    });
  }

  getName(): string {
    return this.name;
  }

  setName(value: string) {
    this.name = value;
  }

  getDescription(): string {
    return this.description;
  }

  setDescription(value: string) {
    this.description = value;
  }

  getSensors(): string[] {
    return this.sensors;
  }

  setSensors(sensors: string[]) {
    this.sensors = sensors;
  }
}
