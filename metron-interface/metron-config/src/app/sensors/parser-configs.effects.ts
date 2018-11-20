import { Effect, Actions, ofType } from '@ngrx/effects'
import { Observable, forkJoin } from 'rxjs';
import { Action } from '@ngrx/store';
import { Injectable } from '@angular/core';
import { mergeMap, map, switchMap } from 'rxjs/operators';
import { SensorParserConfigService } from 'app/service/sensor-parser-config.service';
import { SensorParserConfig } from 'app/model/sensor-parser-config';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';
import * as ParsersActions from './parser-configs.actions';
import { StormService } from '../service/storm.service';

@Injectable()
export class ParserConfigEffects {

  @Effect()
  loadData$: Observable<Action> = this.actions$.pipe(
    ofType(ParsersActions.ParserConfigsActions.LoadStart),
    mergeMap((action) => {
      return forkJoin(
        this.parserService.getAllConfig(),
        this.parserService.getAllGroups(),
        this.stormService.getAll(),
      ).pipe(
          map(([ configs, groups, statuses ]) => {
          const configsArray: SensorParserConfigHistory[] = Object.keys(configs).map((sensorName) => {
            const sensorParserConfigHistory = new SensorParserConfigHistory();
            sensorParserConfigHistory.sensorName = sensorName;
            sensorParserConfigHistory.setConfig(configs[sensorName]);
            return sensorParserConfigHistory;
          });
          return new ParsersActions.LoadSuccess({
            parsers: configsArray,
            groups: groups,
            statuses: statuses,
          } as ParsersActions.LoadSuccesActionPayload);
        })
      )
    })
  );

  constructor(
    private parserService: SensorParserConfigService,
    private stormService: StormService,
    private actions$: Actions
  ) {}
}
