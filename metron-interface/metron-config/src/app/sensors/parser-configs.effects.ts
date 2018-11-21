import { Effect, Actions, ofType } from '@ngrx/effects'
import { Observable, forkJoin } from 'rxjs';
import { Action } from '@ngrx/store';
import { Injectable } from '@angular/core';
import { mergeMap, map, switchMap } from 'rxjs/operators';
import { SensorParserConfigService } from 'app/service/sensor-parser-config.service';
import { ParserConfigModel } from './models/parser-config.model';
import { SensorParserConfigHistory } from 'app/model/sensor-parser-config-history';
import * as ParsersActions from './parser-configs.actions';
import { StormService } from '../service/storm.service';
import { TopologyStatus } from '../model/topology-status';
import { ParserMetaInfoModel } from './models/parser-meta-info.model';
import { ParserGroupModel } from './models/parser-group.model';

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
          });
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

  constructor(
    private parserService: SensorParserConfigService,
    private stormService: StormService,
    private actions$: Actions
  ) {}
}
