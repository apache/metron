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
import {Injectable, Inject}     from '@angular/core';
import {Http, Headers, RequestOptions, Response, URLSearchParams} from '@angular/http';
import {Observable}     from 'rxjs/Observable';
import {HttpUtil} from '../util/httpUtil';
import {Subject}    from 'rxjs/Subject';
import {IAppConfig} from '../app.config.interface';
import {APP_CONFIG} from '../app.config';
import {ParserExtensionConfig} from "../model/parser-extension-config";

@Injectable()
export class ParserExtensionService {
  url = this.config.apiEndpoint + '/ext/parsers';
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};
  postHeaders = {'X-Requested-With': 'XMLHttpRequest'};
  selectedParserExtensionConfig: ParserExtensionConfig;

  dataChangedSource = new Subject<ParserExtensionConfig[]>();
  dataChanged$ = this.dataChangedSource.asObservable();

  constructor(private http: Http, @Inject(APP_CONFIG) private config: IAppConfig) {

  }

  public post(extensionTgz: string, formData: FormData): Observable<Response> {
    let params: URLSearchParams = new URLSearchParams();
    return this.http.post(this.url, formData, new RequestOptions({headers: new Headers(this.postHeaders), search: params }))
    .catch(HttpUtil.handleError);
  }

  public get(name: string): Observable<ParserExtensionConfig> {
    return this.http.get(this.url + '/' + name, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

  public getAll(): Observable<ParserExtensionConfig[]> {
    return this.http.get(this.url, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .map(HttpUtil.extractData)
      .catch(HttpUtil.handleError);
  }

  public delete(name: string): Observable<Response> {
    return this.http.delete(this.url + '/' + name, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
      .catch(HttpUtil.handleError);
  }

  public deleteMany(parserExtensions: ParserExtensionConfig[]): Observable<{success: Array<string>, failure: Array<string>}> {
    let result: {success: Array<string>, failure: Array<string>} = {success: [], failure: []};
    let observable = Observable.create((observer => {

      let completed = () => {
        if (observer) {
          observer.next(result);
          observer.complete();
        }

        this.dataChangedSource.next(parserExtensions);
      };

      for (let i = 0; i < parserExtensions.length; i++) {
        this.delete(parserExtensions[i].extensionIdentifier).subscribe(results => {
          result.success.push(parserExtensions[i].extensionIdentifier);
          if (result.success.length + result.failure.length === parserExtensions.length) {
            completed();
          }
        }, error => {
          result.failure.push(parserExtensions[i].extensionIdentifier);
          if (result.success.length + result.failure.length === parserExtensions.length) {
            completed();
          }
        });
      }

    }));

    return observable;
  }

  public setSeletedExtension(parserExtension: ParserExtensionConfig): void {
    this.selectedParserExtensionConfig = parserExtension;
  }

  public getSelectedExtension(): ParserExtensionConfig {
    return this.selectedParserExtensionConfig;
  }

}
