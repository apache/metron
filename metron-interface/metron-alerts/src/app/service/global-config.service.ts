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
import {Injectable, Inject} from '@angular/core';
import {Http, Headers, RequestOptions, Response, ResponseOptions} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import {HttpUtil} from '../utils/httpUtil';

@Injectable()
export class GlobalConfigService {
  url = 'api/v1/global/config';
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};

  private globalConfig = {};

  constructor(private http: Http) {
    this.globalConfig['solr.collection'] = 'metron';
    this.globalConfig['storm.indexingWorkers'] = 1;
    this.globalConfig['storm.indexingExecutors'] = 2;
    this.globalConfig['hdfs.boltBatchSize'] = 5000;
    this.globalConfig['hdfs.boltFieldDelimiter'] = '|';
    this.globalConfig['hdfs.boltFileRotationSize'] = 5;
    this.globalConfig['hdfs.boltCompressionCodecClass'] = 'org.apache.hadoop.io.compress.SnappyCodec';
    this.globalConfig['hdfs.indexOutput'] = '/tmp/metron/enriched';
    this.globalConfig['kafkaWriter.topic'] = 'outputTopic';
    this.globalConfig['kafkaWriter.keySerializer'] = 'org.apache.kafka.common.serialization.StringSerializer';
    this.globalConfig['kafkaWriter.valueSerializer'] = 'org.apache.kafka.common.serialization.StringSerializer';
    this.globalConfig['kafkaWriter.requestRequiredAcks'] = 1;
    this.globalConfig['solrWriter.indexName'] = 'alfaalfa';
    this.globalConfig['solrWriter.shards'] = 1;
    this.globalConfig['solrWriter.replicationFactor'] = 1;
    this.globalConfig['solrWriter.batchSize'] = 50;
    this.globalConfig['sourceType'] = 'source:type';
  }

  public post(globalConfig: {}): Observable<{}> {
    return this.http.post(this.url, globalConfig, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

  public get(): Observable<{}> {
    return this.http.get(this.url , new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map((res: Response): any => {
        let body = res.json();
        if (!body['sourceType'] ) {
          let newBody = {};
          newBody = Object.assign({}, body, {sourceType: 'source:type'});
          return newBody;
        }
        return body || {};
      })
      .catch(HttpUtil.handleError);
  }

  public delete(): Observable<Response> {
    let responseOptions = new ResponseOptions();
    responseOptions.status = 200;
    let response = new Response(responseOptions);
    return Observable.create(observer => {
      observer.next(response);
      observer.complete();
    });
  }

  private setDefaultSourceType(globalConfig) {
    let sourceType: {} = {};
    if(!globalConfig['source.type']) {
      sourceType = Object.assign({}, globalConfig, {sourceType: 'source:type'});
      return sourceType;
    }
  }

}
