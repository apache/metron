import { Effect, Actions, ofType } from '@ngrx/effects'
import { Observable } from 'rxjs';
import { Action } from '@ngrx/store';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ParserConfigsListActions } from './sensor-parser-list.actions';
import { mergeMap, map } from 'rxjs/operators';
import { SensorParserConfigService } from 'app/service/sensor-parser-config.service';
import { SensorParserConfig } from 'app/model/sensor-parser-config';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';
import { ParserLoadSuccess } from '../parser-configs.actions';

@Injectable()
export class SensorParserListEffects {

  @Effect()
  initialized$: Observable<Action> = this.actions$.pipe(
    ofType(ParserConfigsListActions.Initialized),
    mergeMap(() => {
      return this.parserService.getAllConfig().pipe(
        map((results: { string: SensorParserConfig }) => {
          const resultArray: SensorParserConfigHistory[] = Object.keys(results).map((sensorName) => {
            const sensorParserConfigHistory = new SensorParserConfigHistory();
            sensorParserConfigHistory.sensorName = sensorName;
            sensorParserConfigHistory.setConfig(results[sensorName]);
            return sensorParserConfigHistory;
          });

          return new ParserLoadSuccess(resultArray);
        })
      )
    })
  );

  constructor(private parserService: SensorParserConfigService, private actions$: Actions) {}
}
