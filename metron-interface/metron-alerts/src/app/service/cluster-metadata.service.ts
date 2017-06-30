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
import {Observable} from 'rxjs/Rx';
import {Http, Headers, RequestOptions} from '@angular/http';
import {HttpUtil} from '../utils/httpUtil';
import {IAppConfig} from '../app.config.interface';
import {APP_CONFIG} from '../app.config';
import {MetadataUtil} from '../utils/metadata-utils';
import {ColumnMetadata} from '../model/column-metadata';

@Injectable()
export class ClusterMetaDataService {
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};
  url = '_cluster/state';

  defaultColumnMetadata = [
    new ColumnMetadata('_id', 'string'),
    new ColumnMetadata('timestamp', 'date'),
    new ColumnMetadata('source:type', 'string'),
    new ColumnMetadata('ip_src_addr', 'ip'),
    new ColumnMetadata('enrichments:geo:ip_dst_addr:country', 'string'),
    new ColumnMetadata('ip_dst_addr', 'ip'),
    new ColumnMetadata('host', 'string'),
    new ColumnMetadata('alert_status', 'string')
  ];

  constructor(private http: Http, @Inject(APP_CONFIG) private config: IAppConfig) {
  }

  getDefaultColumns(): Observable<ColumnMetadata[]> {
    return Observable.create(observer => {
      observer.next(JSON.parse(JSON.stringify(this.defaultColumnMetadata)));
      observer.complete();
    });
  }

  getColumnMetaData(): Observable<ColumnMetadata[]> {
    return this.http.get(this.url, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .map(MetadataUtil.extractData)
      .catch(HttpUtil.handleError);
  }
}
