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
import { TestBed } from '@angular/core/testing';
import { StoreModule, Store, combineReducers } from '@ngrx/store';
import { SensorsEffects } from './sensors.effects';
import { SensorsModule } from '../sensors.module';
import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import * as fromReducers from '../reducers';
import { EffectsModule } from '@ngrx/effects';
import * as fromActions from '../actions';
import { ParserMetaInfoModel } from '../models/parser-meta-info.model';
import { ParserGroupModel } from '../models/parser-group.model';
import { ParserConfigModel } from '../models/parser-config.model';


describe('parser-config.actions.ts', () => {
  let store: Store<fromReducers.State>;

  function fillStoreWithTestData() {
    store.dispatch(new fromActions.LoadSuccess({
      parsers: [
        { config: new ParserGroupModel({ name: 'TestGroup01', description: '' }) },
        { config: new ParserGroupModel({ name: 'TestGroup02', description: '' }) },
      ],
      groups: [
        { config: new ParserConfigModel({ sensorTopic: 'TestConfig01' }) },
        { config: new ParserConfigModel({ sensorTopic: 'TestConfig02' }) },
      ],
      statuses: []
    }));
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        SensorsModule,
        StoreModule.forRoot({ sensors: combineReducers(fromReducers.reducers) }),
        EffectsModule.forRoot([]),
        HttpClientTestingModule
      ],
      providers: [
        SensorsEffects,
        HttpClient,
      ]
    });

    store = TestBed.get(Store);

    fillStoreWithTestData()
  });

  it('Should POST /sensor/parser/group on action ApplyChanges', () => {
    store.dispatch(new fromActions.ApplyChanges());
  })

  it('Should POST /sensor/parser/config on action ApplyChanges', () => {

  })

});
