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
import { TestBed, inject, getTestBed } from '@angular/core/testing';

import { ShowHideService } from './show-hide.service';
import { QueryBuilder, FilteringMode } from 'app/alerts/alerts-list/query-builder';

import { Spy } from 'jasmine-core';
import { Filter } from 'app/model/filter';
import { UserSettingsService } from 'app/service/user-settings.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AppConfigService } from 'app/service/app-config.service';

class QueryBuilderMock {
  addOrUpdateFilter = () => {};
  removeFilter = () => {};
  getFilteringMode = () => {};
}

describe('ShowHideService', () => {
  let queryBuilderMock: QueryBuilderMock;
  let userSettingsService: UserSettingsService;

  beforeEach(() => {

    spyOn(ShowHideService.prototype, 'setFilterFor').and.callThrough();
    spyOn(UserSettingsService.prototype, 'get').and.callThrough();
    spyOn(UserSettingsService.prototype, 'save').and.callThrough();

    TestBed.configureTestingModule({
      imports: [ HttpClientTestingModule ],
      providers: [
        {
          provide: AppConfigService,
          useValue: {
            getApiRoot() { return ''; }
          }
        },
        UserSettingsService,
        ShowHideService,
        { provide: QueryBuilder, useClass: QueryBuilderMock },
      ]
    });

    queryBuilderMock = getTestBed().get(QueryBuilder);
    userSettingsService = getTestBed().get(UserSettingsService);
  });

  it('should be created', inject([ShowHideService], (service: ShowHideService) => {
    expect(service).toBeTruthy();
  }));

  it('should have QueryBuilder injected', inject([ShowHideService], (service: ShowHideService) => {
    expect(service.queryBuilder).toBeTruthy();
  }));

  it('should get persisted state', inject([ShowHideService], (service: ShowHideService) => {
    expect(userSettingsService.get).toHaveBeenCalledWith(service.HIDE_RESOLVE_STORAGE_KEY);
    expect(userSettingsService.get).toHaveBeenCalledWith(service.HIDE_DISMISS_STORAGE_KEY);
  }));

  it('should set initial filter state', inject([ShowHideService], (service: ShowHideService) => {
    expect((service.setFilterFor as Spy).calls.argsFor(0)).toEqual(['RESOLVE', false]);
    expect((service.setFilterFor as Spy).calls.argsFor(1)).toEqual(['DISMISS', false]);
  }));

  it('should set value to hideDismissed ', inject([ShowHideService], (service: ShowHideService) => {
    expect(service.hideDismissed).toBe(false);
  }));

  it('should set value to hideResolved', inject([ShowHideService], (service: ShowHideService) => {
    expect(service.hideResolved).toBe(false);
  }));

  it('should save state on change for RESOLVE', inject([ShowHideService], (service: ShowHideService) => {
    service.setFilterFor('RESOLVE', true);

    expect(userSettingsService.save).toHaveBeenCalledWith({
      [service.HIDE_RESOLVE_STORAGE_KEY]: true
    });
  }));

  it('should save state for DISMISS', inject([ShowHideService], (service: ShowHideService) => {
    service.setFilterFor('DISMISS', true);

    expect(userSettingsService.save).toHaveBeenCalledWith({
      [service.HIDE_DISMISS_STORAGE_KEY]: true
    });
  }));

  it('should be able to add RESOLVE filter to QueryBuilder', inject([ShowHideService], (service: ShowHideService) => {
    spyOn(queryBuilderMock, 'addOrUpdateFilter');
    spyOn(queryBuilderMock, 'removeFilter');

    service.setFilterFor('RESOLVE', true);
    expect(queryBuilderMock.addOrUpdateFilter).toHaveBeenCalledWith(new Filter('-alert_status', 'RESOLVE', false));
    expect(queryBuilderMock.removeFilter).not.toHaveBeenCalled();
  }));

  it('should be able to remove RESOLVE filter to QueryBuilder', inject([ShowHideService], (service: ShowHideService) => {
    spyOn(queryBuilderMock, 'addOrUpdateFilter');
    spyOn(queryBuilderMock, 'removeFilter');

    service.setFilterFor('RESOLVE', false);
    expect(queryBuilderMock.removeFilter).toHaveBeenCalledWith(new Filter('-alert_status', 'RESOLVE', false));
    expect(queryBuilderMock.addOrUpdateFilter).not.toHaveBeenCalled();
  }));

  it('should be able to add DISMISS filter to QueryBuilder', inject([ShowHideService], (service: ShowHideService) => {
    spyOn(queryBuilderMock, 'addOrUpdateFilter');
    spyOn(queryBuilderMock, 'removeFilter');

    service.setFilterFor('DISMISS', true);
    expect(queryBuilderMock.addOrUpdateFilter).toHaveBeenCalledWith(new Filter('-alert_status', 'DISMISS', false));
    expect(queryBuilderMock.removeFilter).not.toHaveBeenCalled();
  }));

  it('should be able to remove DISMISS filter to QueryBuilder', inject([ShowHideService], (service: ShowHideService) => {
    spyOn(queryBuilderMock, 'addOrUpdateFilter');
    spyOn(queryBuilderMock, 'removeFilter');

    service.setFilterFor('DISMISS', false);
    expect(queryBuilderMock.removeFilter).toHaveBeenCalledWith(new Filter('-alert_status', 'DISMISS', false));
    expect(queryBuilderMock.addOrUpdateFilter).not.toHaveBeenCalled();
  }));

  it('is available should return false if query builder in in manual mode', inject([ShowHideService], (service: ShowHideService) => {
    queryBuilderMock.getFilteringMode = () => FilteringMode.MANUAL;
    expect(service.isAvailable()).toBe(false);

    queryBuilderMock.getFilteringMode = () => FilteringMode.BUILDER;
    expect(service.isAvailable()).toBe(true);
  }));
});
