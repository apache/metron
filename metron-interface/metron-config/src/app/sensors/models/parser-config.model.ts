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
import {FieldTransformer} from '../../model/field-transformer';
import { ParserModel } from './parser.model';
import * as cloneDeep from 'clone-deep';

export class ParserConfigModel implements ParserModel {

  id: string;
  cacheConfig: Object;
  errorTopic: any;
  errorWriterClassName: string;
  errorWriterNumTasks: number;
  errorWriterParallelism: number;
  fieldTransformations: FieldTransformer[];
  filterClassName: string;
  mergeMetadata: boolean;
  numAckers: number;
  numWorkers: number;
  outputTopic: any;
  parserClassName: string;
  parserConfig: any;
  parserNumTasks: number;
  parserParallelism: number;
  rawMessageStrategy: string;
  rawMessageStrategyConfig: {};
  readMetadata: boolean;
  securityProtocol: any;
  sensorTopic: string;
  spoutConfig: {};
  spoutNumTasks: number;
  spoutParallelism: number;
  stormConfig: {};
  writerClassName: string;
  invalidWriterClassName: string;
  startStopInProgress: boolean;
  group: string;
  description: string;

  constructor(id: string, config: any = {}) {

    this.id = id;

    Object.keys(config).forEach(key => {
      this[key] = config[key];
    });

    this.group = config.group || '';
    this.parserConfig = config.parserConfig || {};
    this.fieldTransformations = config.fieldTransformations || [];
    this.spoutConfig = config.spoutConfig || {};
    this.stormConfig = config.stormConfig || {};
  }

  clone(params = {}): ParserConfigModel {
    const clone = {
      ...cloneDeep(this),
      ...params
    };

    return new ParserConfigModel(clone.id, clone);
  }

  getName(): string {
    return this.id;
  }

  setName(value: string) {
    this.id = value;
  }

  getDescription(): string {
    return '';
  }
}
