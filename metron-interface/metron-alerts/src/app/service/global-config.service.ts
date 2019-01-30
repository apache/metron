
import {catchError, map} from 'rxjs/operators';
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
import { Observable } from 'rxjs';
import {HttpUtil} from '../utils/httpUtil';
import { AppConfigService } from './app-config.service';

@Injectable()
export class GlobalConfigService {
  url = this.appConfigService.getApiRoot() + '/global/config';

  private globalConfig = {};

  constructor(private http: HttpClient, private appConfigService: AppConfigService) {}

  public get(): Observable<{}> {
    return this.http.get(this.url).pipe(
      map((res): any => {
        let body = res;
        let globalConfig = this.setDefaults(body);
        return globalConfig || {};
      }),
      catchError(HttpUtil.handleError));
  }

  private setDefaults(globalConfig) {
    let missingSourceTypeField = !globalConfig['source.type.field'];
    let missingThreatScoreField = !globalConfig['threat.triage.score.field'];
    if(missingSourceTypeField || missingThreatScoreField) {
      let sourceTypeField = missingSourceTypeField?'source:type':globalConfig['source.type.field'];
      let threatScoreField = missingThreatScoreField?'threat:triage:score':globalConfig['threat.triage.score.field'];
      return Object.assign({}, globalConfig,
                          {'source.type.field': sourceTypeField
                          , 'threat.triage.score.field' : threatScoreField
                          }
                          );
    } else {
      return globalConfig;
    }
  }

}
