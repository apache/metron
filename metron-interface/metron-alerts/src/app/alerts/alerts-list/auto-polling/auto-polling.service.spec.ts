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
import { Subject, of } from 'rxjs';
import { QueryBuilder } from '../query-builder';
import { SearchResponse } from 'app/model/search-response';

fdescribe('AutoPollingService', () => {

  let autoPollingService: AutoPollingService;
  let searchService: SearchService;

  function getIntervalInMS(): number {
    return autoPollingService.getInterval() * 1000;
  }

  beforeEach(() => {
    localStorage.getItem = () => null;
    localStorage.setItem = () => {};

    TestBed.configureTestingModule({
      providers: [
        AutoPollingService,
        { provide: SearchService, useClass: () => { return {
          search: () => of(new SearchResponse()),
        } } },
        { provide: QueryBuilder, useClass: () => { return {
          addOrUpdateFilter: () => {},
          query: '*'
        } } },
      ]
    });

    autoPollingService = TestBed.get(AutoPollingService);
    searchService = TestBed.get(SearchService);
  });

  afterEach(() => {
    autoPollingService.onDestroy();
  });


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
    spyOn(searchService, 'search').and.callThrough();

    autoPollingService.start();

    expect(searchService.search).toHaveBeenCalled();
  });

  it('should broadcast response to initial request via data subject', () => {
    autoPollingService.data.subscribe((result) => {
      expect(result).toEqual(new SearchResponse());
    });

    autoPollingService.start();
  });

  it('should start polling when start called', fakeAsync(() => {
    spyOn(searchService, 'search').and.callThrough();

    autoPollingService.start();

    tick(getIntervalInMS());

    expect(searchService.search).toHaveBeenCalledTimes(2);

    autoPollingService.stop();
  }));

  it('should broadcast polling response via data subject', fakeAsync(() => {
    const fakeSearchObservable = new Subject<SearchResponse>();
    const fakePollResponse = new SearchResponse();

    autoPollingService.start();
    // The reason am mocking the searchService.search here is to not interfere
    // with the initial request triggered right after the start
    searchService.search = () => fakeSearchObservable;

    autoPollingService.data.subscribe((result) => {
      expect(result).toBe(fakePollResponse);
      autoPollingService.stop();
    });

    tick(autoPollingService.getInterval() * 1000);

    fakeSearchObservable.next(fakePollResponse);
  }));

  it('should keep polling and broadcasting based on the interval', fakeAsync(() => {
    const fakeSearchObservable = new Subject<SearchResponse>();
    const broadcastObserverSpy = jasmine.createSpy('broadcastObserverSpy');
    const testInterval = 2;

    autoPollingService.setInterval(testInterval);
    autoPollingService.start();

    // The reason am mocking the searchService.search here is to not interfere
    // with the initial request triggered right after the start
    searchService.search = () => fakeSearchObservable;
    spyOn(searchService, 'search').and.callThrough();

    autoPollingService.data.subscribe(broadcastObserverSpy);

    tick(testInterval * 1000);
    expect(searchService.search).toHaveBeenCalledTimes(1);

    fakeSearchObservable.next({ total: 2 } as SearchResponse);
    expect(broadcastObserverSpy).toHaveBeenCalledTimes(1);
    expect(broadcastObserverSpy.calls.argsFor(0)[0]).toEqual({ total: 2 });

    tick(testInterval * 1000);
    expect(searchService.search).toHaveBeenCalledTimes(2);

    fakeSearchObservable.next({ total: 3 } as SearchResponse);
    expect(broadcastObserverSpy).toHaveBeenCalledTimes(2);
    expect(broadcastObserverSpy.calls.argsFor(1)[0]).toEqual({ total: 3 });

    autoPollingService.stop();
  }));

  it('should use latest query from query builder', () => {

  });

});
