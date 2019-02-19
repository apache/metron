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
import { Injectable, Inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { KafkaTopic } from '../model/kafka-topic';
import { HttpUtil } from '../util/httpUtil';
import { RestError } from '../model/rest-error';
import {AppConfigService} from './app-config.service';

@Injectable()
export class KafkaService {
  url = this.appConfigService.getApiRoot() + '/kafka/topic';

  constructor(
    private http: HttpClient,
    private appConfigService: AppConfigService
  ) {}

  public post(kafkaTopic: KafkaTopic): Observable<KafkaTopic> {
    return this.http.post(this.url, JSON.stringify(kafkaTopic)).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public get(name: string): Observable<KafkaTopic> {
    return this.http.get(this.url + '/' + name).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public list(): Observable<KafkaTopic[]> {
    return this.http.get(this.url).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public sample(name: string): Observable<string | RestError> {
    return this.http.get(this.url + '/' + name + '/sample', {responseType: 'text'}).pipe(
      map(HttpUtil.extractString),
      catchError(HttpUtil.handleError)
    );
  }
}
