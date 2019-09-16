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
import { AutoPollingService } from './auto-polling.service';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { SearchService } from 'app/service/search.service';
import { Subject, of, throwError } from 'rxjs';
import { QueryBuilder } from '../query-builder';
import { SearchResponse } from 'app/model/search-response';
import { SearchRequest } from 'app/model/search-request';
import { Spy } from 'jasmine-core';
import { DialogService } from 'app/service/dialog.service';
import { RestError } from 'app/model/rest-error';
import { DialogType } from 'app/model/dialog-type';

class QueryBuilderFake {
  private _filter = '';
  query: '*'

  addOrUpdateFilter() {};

  setFilter(filter: string): void {
    this._filter = filter;
  };

  get searchRequest(): SearchRequest {
    return {
      query: this._filter,
      fields: [],
      size: 2,
      indices: [],
      from: 0,
      sort: [],
      facetFields: [],
    };
  };
}

describe('AutoPollingService', () => {

  let autoPollingService: AutoPollingService;
  let searchServiceFake: SearchService;

  function getIntervalInMS(): number {
    return autoPollingService.getInterval() * 1000;
  }

  beforeEach(() => {
    localStorage.getItem = () => null;
    localStorage.setItem = () => {};

    TestBed.configureTestingModule({
      providers: [
        AutoPollingService,
        { provide: DialogService, useClass: () => {} },
        { provide: SearchService, useClass: () => { return {
          search: () => of(new SearchResponse()),
        } } },
        { provide: QueryBuilder, useClass: QueryBuilderFake },
      ]
    });

    autoPollingService = TestBed.get(AutoPollingService);
    searchServiceFake = TestBed.get(SearchService);
  });

  afterEach(() => {
    autoPollingService.onDestroy();
  });


  describe('polling basics', () => {
    it('should mark polling as active after start', () => {
      autoPollingService.start();
      expect(autoPollingService.getIsPollingActive()).toBe(true);
    });

    it('should mark polling as inactive after stop', () => {
      autoPollingService.start();
      expect(autoPollingService.getIsPollingActive()).toBe(true);

      autoPollingService.stop();
      expect(autoPollingService.getIsPollingActive()).toBe(false);
    });

    it('should send an initial request on start', () => {
      spyOn(searchServiceFake, 'search').and.callThrough();

      autoPollingService.start();

      expect(searchServiceFake.search).toHaveBeenCalled();
    });

    it('should broadcast response to initial request via data subject', () => {
      autoPollingService.data.subscribe((result) => {
        expect(result).toEqual(new SearchResponse());
      });

      autoPollingService.start();
    });

    it('should start polling when start called', fakeAsync(() => {
      spyOn(searchServiceFake, 'search').and.callThrough();

      autoPollingService.start();

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);

      autoPollingService.stop();
    }));

    it('should broadcast polling response via data subject', fakeAsync(() => {
      const searchObservableFake = new Subject<SearchResponse>();
      const pollResponseFake = new SearchResponse();

      autoPollingService.start();
      // The reason am mocking the searchService.search here is to not interfere
      // with the initial request triggered right after the start
      searchServiceFake.search = () => searchObservableFake;

      autoPollingService.data.subscribe((result) => {
        expect(result).toBe(pollResponseFake);
        autoPollingService.stop();
      });

      tick(autoPollingService.getInterval() * 1000);

      searchObservableFake.next(pollResponseFake);
    }));

    it('should polling and broadcasting based on the interval', fakeAsync(() => {
      const searchObservableFake = new Subject<SearchResponse>();
      const broadcastObserverSpy = jasmine.createSpy('broadcastObserverSpy');
      const testInterval = 2;

      autoPollingService.setInterval(testInterval);
      autoPollingService.start();

      // The reason am mocking the searchService.search here is to not interfere
      // with the initial request triggered right after the start
      searchServiceFake.search = () => searchObservableFake;
      spyOn(searchServiceFake, 'search').and.callThrough();

      autoPollingService.data.subscribe(broadcastObserverSpy);

      tick(testInterval * 1000);
      expect(searchServiceFake.search).toHaveBeenCalledTimes(1);

      searchObservableFake.next({ total: 2 } as SearchResponse);
      expect(broadcastObserverSpy).toHaveBeenCalledTimes(1);
      expect(broadcastObserverSpy.calls.argsFor(0)[0]).toEqual({ total: 2 });

      tick(testInterval * 1000);
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);

      searchObservableFake.next({ total: 3 } as SearchResponse);
      expect(broadcastObserverSpy).toHaveBeenCalledTimes(2);
      expect(broadcastObserverSpy.calls.argsFor(1)[0]).toEqual({ total: 3 });

      autoPollingService.stop();
    }));

    it('interval change should impact the polling even when it is active', fakeAsync(() => {
      autoPollingService.start();

      // The reason am mocking the searchService.search here is to not interfere
      // with the initial request triggered right after the start
      spyOn(searchServiceFake, 'search').and.callThrough();

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(1);

      autoPollingService.setInterval(9);

      tick(4000);
      expect(searchServiceFake.search).toHaveBeenCalledTimes(1);

      tick(5000);
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);

      autoPollingService.setInterval(2);

      tick(1000);
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);

      tick(1000);
      expect(searchServiceFake.search).toHaveBeenCalledTimes(3);

      autoPollingService.stop();
    }));

    it('should stop polling when stop triggered', fakeAsync(() => {
      const searchObservableFake = new Subject<SearchResponse>();
      const broadcastObserverSpy = jasmine.createSpy('broadcastObserverSpy');

      autoPollingService.start();

      // The reason am mocking the searchService.search here is to not interfere
      // with the initial request triggered right after the start
      searchServiceFake.search = () => searchObservableFake;
      spyOn(searchServiceFake, 'search').and.callThrough();

      autoPollingService.data.subscribe(broadcastObserverSpy);

      tick(getIntervalInMS());
      searchObservableFake.next({ total: 3 } as SearchResponse);
      expect(searchServiceFake.search).toHaveBeenCalledTimes(1);

      autoPollingService.stop();

      tick(getIntervalInMS() * 4);
      expect(searchServiceFake.search).toHaveBeenCalledTimes(1);
    }));

    it('should use the latest query from query builder', fakeAsync(() => {
      const queryBuilderFake = TestBed.get(QueryBuilder);
      spyOn(searchServiceFake, 'search').and.callThrough();

      queryBuilderFake.setFilter('testFieldAA:testValueAA');
      autoPollingService.start();
      expect((searchServiceFake.search as Spy).calls.argsFor(0)[0].query).toBe('testFieldAA:testValueAA');

      queryBuilderFake.setFilter('testFieldBB:testValueBB');
      tick(getIntervalInMS());
      expect((searchServiceFake.search as Spy).calls.argsFor(1)[0].query).toBe('testFieldBB:testValueBB');

      queryBuilderFake.setFilter('*');
      tick(getIntervalInMS());
      expect((searchServiceFake.search as Spy).calls.argsFor(2)[0].query).toBe('*');

      autoPollingService.stop();
    }));

    it('should show notification on http error', fakeAsync(() => {
      const fakeDialogService = TestBed.get(DialogService);
      fakeDialogService.launchDialog = () => {};
      spyOn(fakeDialogService, 'launchDialog');

      autoPollingService.start();

      spyOn(searchServiceFake, 'search').and.returnValue(throwError(new RestError()));

      tick(getIntervalInMS());

      expect(fakeDialogService.launchDialog).toHaveBeenCalledWith(
        'Server were unable to apply query string. Evaluate query string and restart polling.',
        DialogType.Error
      );

      autoPollingService.stop();
    }));
  });

  describe('polling suppression - to prevent collision with other features', () => {
    it('should suspend polling even if it is started', fakeAsync(() => {
      spyOn(searchServiceFake, 'search').and.callThrough();

      autoPollingService.start();
      expect(searchServiceFake.search).toHaveBeenCalledTimes(1);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(3);

      autoPollingService.setSuppression(true);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(3);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(3);

      autoPollingService.stop();
    }));

    it('should continue polling when freed from suppression if it is started ', fakeAsync(() => {
      spyOn(searchServiceFake, 'search').and.callThrough();

      autoPollingService.start();
      expect(searchServiceFake.search).toHaveBeenCalledTimes(1);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(3);

      autoPollingService.setSuppression(true);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(3);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(3);

      autoPollingService.setSuppression(false);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(4);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(5);

      autoPollingService.stop();
    }));

    it('should have no impact when polling stopped', fakeAsync(() => {
      spyOn(searchServiceFake, 'search').and.callThrough();

      autoPollingService.start();
      expect(searchServiceFake.search).toHaveBeenCalledTimes(1);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);

      autoPollingService.stop();
      autoPollingService.setSuppression(true);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);

      autoPollingService.setSuppression(false);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);
    }));
  });

  describe('request congestion handling - when refresh interval faster than response time', () => {
    it('should skip new poll request when there is congestion', fakeAsync(() => {
      const searchObservableFake = new Subject<SearchResponse>();

      searchServiceFake.search = () => searchObservableFake;
      spyOn(searchServiceFake, 'search').and.callThrough();

      autoPollingService.start();

      searchObservableFake.next({ total: 2 } as SearchResponse);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);
      expect(autoPollingService.getIsCongestion()).toBe(false);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);
      expect(autoPollingService.getIsCongestion()).toBe(true);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);
      expect(autoPollingService.getIsCongestion()).toBe(true);

      autoPollingService.stop();
    }));

    it('should continue polling when congestion resolves', fakeAsync(() => {
      const searchObservableFake = new Subject<SearchResponse>();

      searchServiceFake.search = () => searchObservableFake;
      spyOn(searchServiceFake, 'search').and.callThrough();

      autoPollingService.start();

      searchObservableFake.next({ total: 2 } as SearchResponse);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);
      expect(autoPollingService.getIsCongestion()).toBe(false);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);
      expect(autoPollingService.getIsCongestion()).toBe(true);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);
      expect(autoPollingService.getIsCongestion()).toBe(true);

      searchObservableFake.next({ total: 2 } as SearchResponse);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(3);
      expect(autoPollingService.getIsCongestion()).toBe(false);

      autoPollingService.stop();
    }));
  });

  describe('cancellation by manual request', () => {

    it('should be able to drop current response and continue polling', fakeAsync(() => {
      const broadcastObserverSpy = jasmine.createSpy('broadcastObserverSpy');
      const searchObservableFake = new Subject<SearchResponse>();

      autoPollingService.start();

      searchServiceFake.search = () => searchObservableFake;
      spyOn(searchServiceFake, 'search').and.callThrough();

      autoPollingService.data.subscribe(broadcastObserverSpy);

      tick(getIntervalInMS());

      expect(searchServiceFake.search).toHaveBeenCalledTimes(1);
      searchObservableFake.next({ total: 2 } as SearchResponse);
      expect(broadcastObserverSpy).toHaveBeenCalledTimes(1);

      tick(getIntervalInMS() / 2);
      autoPollingService.dropNextAndContinue();
      tick(getIntervalInMS() / 2);

      expect(searchServiceFake.search).toHaveBeenCalledTimes(1);
      searchObservableFake.next({ total: 3 } as SearchResponse);
      expect(broadcastObserverSpy).toHaveBeenCalledTimes(1);

      tick(getIntervalInMS());

      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);
      searchObservableFake.next({ total: 4 } as SearchResponse);
      expect(broadcastObserverSpy).toHaveBeenCalledTimes(2);

      autoPollingService.stop();
    }));

  });

  describe('polling state persisting and restoring', () => {

    it('should persist polling state on start', () => {
      spyOn(localStorage, 'setItem');
      autoPollingService.start();
      expect(localStorage.setItem).toHaveBeenCalledWith('autoPolling', '{"isActive":true,"refreshInterval":10}');
    });

    it('should persist polling state on stop', () => {
      spyOn(localStorage, 'setItem');
      autoPollingService.stop();
      expect(localStorage.setItem).toHaveBeenCalledWith('autoPolling', '{"isActive":false,"refreshInterval":10}');
    });

    it('should persist polling state on interval change', () => {
      spyOn(localStorage, 'setItem');
      autoPollingService.setInterval(4);
      expect(localStorage.setItem).toHaveBeenCalledWith('autoPolling', '{"isActive":false,"refreshInterval":4}');
    });

    it('should restore polling state on construction', () => {
      const queryBuilderFake = TestBed.get(QueryBuilder);
      const dialogServiceFake = TestBed.get(QueryBuilder);

      spyOn(localStorage, 'getItem').and.returnValue('{"isActive":true,"refreshInterval":443}');

      const localAutoPollingSvc = new AutoPollingService(searchServiceFake, queryBuilderFake, dialogServiceFake);

      expect(localStorage.getItem).toHaveBeenCalledWith('autoPolling');
      expect(localAutoPollingSvc.getIsPollingActive()).toBe(true);
      expect(localAutoPollingSvc.getInterval()).toBe(443);
    });

    it('should start polling on construction when persisted isActive==true', fakeAsync(() => {
      const queryBuilderFake = TestBed.get(QueryBuilder);
      const dialogServiceFake = TestBed.get(QueryBuilder);

      spyOn(searchServiceFake, 'search').and.callThrough();
      spyOn(localStorage, 'getItem').and.returnValue('{"isActive":true,"refreshInterval":10}');

      const localAutoPollingSvc = new AutoPollingService(searchServiceFake, queryBuilderFake, dialogServiceFake);

      expect(searchServiceFake.search).toHaveBeenCalledTimes(1);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);

      tick(getIntervalInMS());
      expect(searchServiceFake.search).toHaveBeenCalledTimes(3);

      localAutoPollingSvc.stop();
    }));

    it('should start polling on construction with the persisted interval', fakeAsync(() => {
      const queryBuilderFake = TestBed.get(QueryBuilder);
      const dialogServiceFake = TestBed.get(QueryBuilder);

      spyOn(searchServiceFake, 'search').and.callThrough();
      spyOn(localStorage, 'getItem').and.returnValue('{"isActive":true,"refreshInterval":4}');

      const localAutoPollingSvc = new AutoPollingService(searchServiceFake, queryBuilderFake, dialogServiceFake);

      expect(searchServiceFake.search).toHaveBeenCalledTimes(1);

      tick(4000);
      expect(searchServiceFake.search).toHaveBeenCalledTimes(2);

      tick(4000);
      expect(searchServiceFake.search).toHaveBeenCalledTimes(3);

      localAutoPollingSvc.stop();
    }));
  });

});
