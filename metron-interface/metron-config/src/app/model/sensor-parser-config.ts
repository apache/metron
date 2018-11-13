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
import {FieldTransformer} from './field-transformer';
export class SensorParserConfig {
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
  parserConfig: {};
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

  constructor(config: any = {}) {

    Object.keys(config).forEach(key => {
      this[key] = config[key];
    });

    this.parserConfig = config.parserConfig || {};
    this.fieldTransformations = config.fieldTransformations || [];
    this.spoutConfig = config.spoutConfig || {};
    this.stormConfig = config.stormConfig || {};
  }

  clone() {
    const clone = new SensorParserConfig();

    clone.parserClassName = this.parserClassName;
    clone.filterClassName = this.filterClassName;
    clone.sensorTopic = this.sensorTopic;
    clone.writerClassName = this.writerClassName;
    clone.errorWriterClassName = this.errorWriterClassName;
    clone.invalidWriterClassName = this.invalidWriterClassName;
    clone.parserConfig = this.parserConfig;
    clone.fieldTransformations = this.fieldTransformations;
    clone.numWorkers = this.numWorkers;
    clone.numAckers = this.numAckers;
    clone.spoutParallelism = this.spoutParallelism;
    clone.spoutNumTasks = this.spoutNumTasks;
    clone.parserParallelism = this.parserParallelism;
    clone.parserNumTasks = this.parserNumTasks;
    clone.errorWriterParallelism = this.errorWriterParallelism;
    clone.errorWriterNumTasks = this.errorWriterNumTasks;
    clone.spoutConfig = this.spoutConfig;
    clone.stormConfig = this.stormConfig;
    clone.startStopInProgress = this.startStopInProgress;
    clone.group = this.group;

    return clone;
  }
}
