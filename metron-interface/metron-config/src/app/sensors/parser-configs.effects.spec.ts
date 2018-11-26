import { TestBed } from '@angular/core/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { Observable } from 'rxjs';
import { Action, StoreModule, Store, ActionReducerMap, combineReducers } from '@ngrx/store';
import { ParserConfigEffects } from './parser-configs.effects';
import { SensorsModule } from './sensors.module';
import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import * as fromFeature from './reducers';
import { EffectsModule } from '@ngrx/effects';
import { InjectionToken } from '@angular/core';
import * as ParserActions from './parser-configs.actions';
import { ParserMetaInfoModel } from './models/parser-meta-info.model';
import { ParserGroupModel } from './models/parser-group.model';
import { ParserConfigModel } from './models/parser-config.model';


describe('parser-config.actions.ts', () => {
  let effects: ParserConfigEffects;
  let actions: Observable<Action>;
  let store: Store<fromFeature.SensorState>;

  function fillStoreWithTestData() {
    store.dispatch(new ParserActions.LoadSuccess({
      parsers: [
        new ParserMetaInfoModel(new ParserGroupModel({ name: 'TestGroup01', description: '' })),
        new ParserMetaInfoModel(new ParserGroupModel({ name: 'TestGroup02', description: '' })),
      ],
      groups: [
        new ParserMetaInfoModel(new ParserConfigModel({ sensorTopic: 'TestConfig01' })),
        new ParserMetaInfoModel(new ParserConfigModel({ sensorTopic: 'TestConfig02' })),
      ],
      statuses: []
    }));
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        SensorsModule,
        StoreModule.forRoot({ sensors: combineReducers(fromFeature.reducers) }),
        EffectsModule.forRoot([]),
        HttpClientTestingModule
      ],
      providers: [
        ParserConfigEffects,
        HttpClient,
      ]
    });

    store = TestBed.get(Store);
    effects = TestBed.get(ParserConfigEffects);

    fillStoreWithTestData()
  });

  it('Should POST /sensor/parser/group on action ApplyChanges', () => {
    store.dispatch(new ParserActions.ApplyChanges());
  })

  it('Should POST /sensor/parser/config on action ApplyChanges', () => {

  })

});
