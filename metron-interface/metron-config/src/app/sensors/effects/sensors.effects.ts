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
import { Effect, Actions, ofType } from '@ngrx/effects'
import { Observable, forkJoin } from 'rxjs';
import { Action, Store, select } from '@ngrx/store';
import { Injectable } from '@angular/core';
import { mergeMap, map, switchMap, withLatestFrom } from 'rxjs/operators';
import { SensorParserConfigService } from 'app/service/sensor-parser-config.service';
import { ParserConfigModel } from '../models/parser-config.model';
import * as fromActions from '../actions';
import { StormService } from '../../service/storm.service';
import { TopologyStatus } from '../../model/topology-status';
import { ParserMetaInfoModel } from '../models/parser-meta-info.model';
import { ParserGroupModel } from '../models/parser-group.model';
import * as fromReducers from '../reducers';

@Injectable()
export class SensorsEffects {

  @Effect()
  loadData$: Observable<Action> = this.actions$.pipe(
    ofType(fromActions.SensorsActionTypes.LoadStart),
    mergeMap(() => {
      return forkJoin(
        this.parserService.getAllConfig(),
        this.parserService.getAllGroups(),
        this.stormService.getAll(),
      ).pipe(
          map(([ configs, groups, statuses ]) => {
          const configsArray: ParserMetaInfoModel[] = Object.keys(configs).map((name) => {
            const metaInfo = new ParserMetaInfoModel(new ParserConfigModel(configs[name]));
            return metaInfo;
          });
          const groupsArray: ParserMetaInfoModel[] =  groups.map((group) => {
            const metaInfo = new ParserMetaInfoModel(new ParserGroupModel(group));
            metaInfo.setIsGroup(true);
            return metaInfo;
          });
          return new fromActions.LoadSuccess({
            parsers: configsArray,
            groups: groupsArray,
            statuses: statuses,
          } as fromActions.LoadSuccesActionPayload);
        })
      )
    })
  );

  @Effect()
  startPolling$: Observable<Action> = this.actions$.pipe(
    ofType(fromActions.SensorsActionTypes.StartPolling),
    switchMap(() => {
      return this.stormService.pollGetAll()
        .pipe(
          map((statuses: TopologyStatus[]) => {
            return new fromActions.PollStatusSuccess({statuses})
          })
        )
    })
  );

  @Effect()
  applyChanges: Observable<Action> = this.actions$.pipe(
    ofType(fromActions.SensorsActionTypes.ApplyChanges),
    withLatestFrom(this.store.pipe(select(fromReducers.getSensorsState))),
    mergeMap(([ , state ]) => {
      return forkJoin(
        this.parserService.syncConfigs(state.parsers.items),
        this.parserService.syncGroups(state.groups.items),
      ).pipe(
        map(() => {
          return new fromActions.LoadStart();
      }));
    })
  )

  constructor(
    private parserService: SensorParserConfigService,
    private stormService: StormService,
    private actions$: Actions,
    private store: Store<fromReducers.State>
  ) {}
}
