import { Effect, Actions, ofType } from '@ngrx/effects'
import { Observable } from 'rxjs';
import { Action } from '@ngrx/store';
import { Injectable } from '@angular/core';
import { mergeMap, map } from 'rxjs/operators';
import { SensorParserConfigService } from 'app/service/sensor-parser-config.service';
import { SensorParserConfig } from 'app/model/sensor-parser-config';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';
import { ParserLoadingSuccess, ParserConfigsActions } from './parser-configs.actions';

@Injectable()
export class ParserConfigEffects {

  @Effect()
  initialized$: Observable<Action> = this.actions$.pipe(
    ofType(ParserConfigsActions.LoadParserStart),
    mergeMap(() => {
      return this.parserService.getAllConfig().pipe(
        map((results: { string: SensorParserConfig }) => {
          const resultArray: SensorParserConfigHistory[] = Object.keys(results).map((sensorName) => {
            const sensorParserConfigHistory = new SensorParserConfigHistory();
            sensorParserConfigHistory.sensorName = sensorName;
            sensorParserConfigHistory.setConfig(results[sensorName]);
            return sensorParserConfigHistory;
          });

          return new ParserLoadingSuccess(resultArray);
        })
      )
    })
  );

  constructor(private parserService: SensorParserConfigService, private actions$: Actions) {}
}
