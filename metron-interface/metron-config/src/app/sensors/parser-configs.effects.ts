import { Effect, Actions, ofType } from '@ngrx/effects'
import { Observable, forkJoin } from 'rxjs';
import { Action, Store, select } from '@ngrx/store';
import { Injectable } from '@angular/core';
import { mergeMap, map, switchMap, withLatestFrom } from 'rxjs/operators';
import { SensorParserConfigService } from 'app/service/sensor-parser-config.service';
import { ParserConfigModel } from './models/parser-config.model';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';
import * as ParsersActions from './parser-configs.actions';
import { StormService } from '../service/storm.service';
import { TopologyStatus } from '../model/topology-status';
import { ParserMetaInfoModel } from './models/parser-meta-info.model';
import { ParserGroupModel } from './models/parser-group.model';
import { SensorState } from './reducers';

@Injectable()
export class ParserConfigEffects {

  private sensorState$: Observable<SensorState>

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
          const configsArray: ParserMetaInfoModel[] = Object.keys(configs).map((name) => {
            const metaInfo = new ParserMetaInfoModel(new ParserConfigModel(configs[name]));
            return metaInfo;
          });
          const groupsArray: ParserMetaInfoModel[] =  groups.map((group) => {
            const metaInfo = new ParserMetaInfoModel(new ParserGroupModel(group));
            metaInfo.setIsGroup(true);
            return metaInfo;
          });
          return new ParsersActions.LoadSuccess({
            parsers: configsArray,
            groups: groupsArray,
            statuses: statuses,
          } as ParsersActions.LoadSuccesActionPayload);
        })
      )
    })
  );

  @Effect()
  startPolling$: Observable<Action> = this.actions$.pipe(
    ofType(ParsersActions.ParserConfigsActions.StartPolling),
    switchMap((action) => {
      return this.stormService.pollGetAll()
        .pipe(
          map((statuses: TopologyStatus[]) => {
            return new ParsersActions.PollStatusSuccess({statuses})
          })
        )
    })
  );

  @Effect()
  applyChanges: Observable<Action> = this.actions$.pipe(
    ofType(ParsersActions.ParserConfigsActions.ApplyChanges),
    withLatestFrom(this.store.select('sensors')),
    mergeMap(([ action, state ]) => {
      return forkJoin(
        this.parserService.syncConfigs(state.parsers.items),
        this.parserService.syncGroups(state.groups.items),
      ).pipe(
        map(() => {
          return new ParsersActions.LoadStart();
      }));
    })
  )

  constructor(
    private parserService: SensorParserConfigService,
    private stormService: StormService,
    private actions$: Actions,
    private store: Store<SensorState>
  ) {}
}
