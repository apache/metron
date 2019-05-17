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
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, from, of } from 'rxjs';
import { catchError, map, take, mergeMap, finalize, filter, reduce } from 'rxjs/operators';
import { ParserConfigModel } from '../sensors/models/parser-config.model';
import { HttpUtil } from '../util/httpUtil';
import { ParseMessageRequest } from '../model/parse-message-request';
import { RestError } from '../model/rest-error';
import { ParserGroupModel } from '../sensors/models/parser-group.model';
import { ParserModel } from 'app/sensors/models/parser.model';
import { ParserMetaInfoModel } from '../sensors/models/parser-meta-info.model';
import { AppConfigService } from './app-config.service';

@Injectable()
export class SensorParserConfigService {

  dataChangedSource = new Subject<string[]>();
  dataChanged$ = this.dataChangedSource.asObservable();

  constructor(
    private http: HttpClient,
    private appConfigService: AppConfigService
  ) {}

  private getParserConfigSvcUrl(): string {
    return this.appConfigService.getApiRoot() + '/sensor/parser/config';
  }

  private getParserGroupSvcUrl(): string {
    return this.appConfigService.getApiRoot() + '/sensor/parser/group';
  }

  public getAllGroups(): Observable<ParserGroupModel[] | RestError> {
    function extractParserGroups(raw) {
      return Object.keys(raw).map((groupName) => {
        return new ParserGroupModel({
          ...raw[groupName]
        })
      });
    }
    return this.http.get(this.getParserGroupSvcUrl()).pipe(
      map(extractParserGroups),
      catchError(HttpUtil.handleError)
    );
  }

  public getGroup(name: string): Observable<RestError | ParserGroupModel> {
    return this.http.get(`${this.getParserGroupSvcUrl()}/${name}`).pipe(
      map(group => new ParserGroupModel(group)),
      catchError(HttpUtil.handleError)
    );
  }

  public saveGroup(name: string, group: ParserGroupModel | ParserModel): Observable<RestError | ParserGroupModel> {
    return this.http.post(`${this.getParserGroupSvcUrl()}`, group).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public deleteGroup(groupName: string): Observable<{ groupName: string, isSuccess: boolean }> {
    return this.http.delete(`${this.getParserGroupSvcUrl()}/${groupName}`).pipe(
      map((result) => { return { groupName, isSuccess: true } }),
      catchError((error) => { return of({ groupName, isSuccess: false }) })
    );
  }

  public deleteGroups(
    groupNames: string[]
  ): Observable<{ success: Array<string>; failure: Array<string> }> {
    let result: { success: Array<string>; failure: Array<string> } = {
      success: [],
      failure: []
    };
    let observable = Observable.create(observer => {
      let completed = () => {
        if (observer) {
          observer.next(result);
          observer.complete();
        }
        this.dataChangedSource.next(groupNames);
      };
      from(groupNames).pipe(
        mergeMap(this.deleteGroup.bind(this)),
        take(groupNames.length),
        map((deleteResult: { groupName: string, isSuccess: boolean}) => {
          (deleteResult.isSuccess ? result.success : result.failure).push(deleteResult.groupName);
        }),
        finalize(completed)
        ).subscribe();
    });

    return observable;
  }

  syncConfigs(configs: ParserMetaInfoModel[]): Observable<{}> {
    return this.sync(configs, this.saveConfig, this.deleteConfig);
  }

  syncGroups(groups: ParserMetaInfoModel[]): Observable<{}> {
    return this.sync(groups, this.saveGroup, this.deleteGroup);
  }

  private sync(
    items: ParserMetaInfoModel[],
    saveFn: Function, deleteFn: Function
  ) {
    return from(items).pipe(
      filter(item => !!(item.isDeleted || item.isDirty || item.isPhantom)),
      mergeMap((changedItem: ParserMetaInfoModel) => {
        if (changedItem.isDeleted) {
          return deleteFn.call(this, changedItem.config.getName());
        } else {
          return saveFn.call(this, changedItem.config.getName(), changedItem.config);
        }
      }),
      catchError(HttpUtil.handleError),
      reduce((acc, value) => {
        return acc.concat(value);
      }, [])
    )
  }

  public getConfig(name: string): Observable<ParserConfigModel> {
    return this.http.get(this.getParserConfigSvcUrl() + '/' + name).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public getAllConfig(): Observable<{}> {
    return this.http.get(this.getParserConfigSvcUrl()).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public saveConfig(
    name: string,
    sensorParserConfig: ParserModel
  ): Observable<ParserConfigModel> {
    return this.http
      .post(this.getParserConfigSvcUrl() + '/' + name, JSON.stringify(sensorParserConfig))
      .pipe(
        map(HttpUtil.extractData),
        catchError(HttpUtil.handleError)
      );
  }

  public deleteConfig(
    name: string
  ): Observable<Object | RestError> {
    return this.http
      .delete(this.getParserConfigSvcUrl() + '/' + name)
      .pipe(catchError(HttpUtil.handleError));
  }

  public deleteConfigs(
    sensorNames: string[]
  ): Observable<{ success: Array<string>; failure: Array<string> }> {
    let result: { success: Array<string>; failure: Array<string> } = {
      success: [],
      failure: []
    };
    let observable = Observable.create(observer => {
      let completed = () => {
        if (observer) {
          observer.next(result);
          observer.complete();
        }

        this.dataChangedSource.next(sensorNames);
      };
      for (let i = 0; i < sensorNames.length; i++) {
        this.deleteConfig(sensorNames[i]).subscribe(
          results => {
            result.success.push(sensorNames[i]);
            if (
              result.success.length + result.failure.length ===
              sensorNames.length
            ) {
              completed();
            }
          },
          error => {
            result.failure.push(sensorNames[i]);
            if (
              result.success.length + result.failure.length ===
              sensorNames.length
            ) {
              completed();
            }
          }
        );
      }
    });

    return observable;
  }

  public getAvailableParsers(): Observable<{}> {
    return this.http.get(this.getParserConfigSvcUrl() + '/list/available').pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }

  public parseMessage(
    parseMessageRequest: ParseMessageRequest
  ): Observable<{}> {
    return this.http.post(this.getParserConfigSvcUrl() + '/parseMessage', parseMessageRequest).pipe(
      map(HttpUtil.extractData),
      catchError(HttpUtil.handleError)
    );
  }
}
