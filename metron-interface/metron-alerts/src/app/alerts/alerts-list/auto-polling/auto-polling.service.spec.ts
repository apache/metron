import { AutoPollingService } from './auto-polling.service';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { SearchService } from 'app/service/search.service';
import { Subject } from 'rxjs';
import { QueryBuilder } from '../query-builder';
import { SearchResponse } from 'app/model/search-response';

fdescribe('AutoPollingService', () => {

  let autoPollingService: AutoPollingService;
  let searchService: SearchService;
  const fakeSearchObservable = new Subject<SearchResponse>();

  function emitInitialResponse() {
    fakeSearchObservable.next(new SearchResponse());
  }

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
          search: () => fakeSearchObservable,
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

    emitInitialResponse();
  });

  it('should start polling when start called', fakeAsync(() => {
    spyOn(searchService, 'search').and.callThrough();

    autoPollingService.start();
    emitInitialResponse();

    tick(getIntervalInMS());

    expect(searchService.search).toHaveBeenCalledTimes(2);

    autoPollingService.stop();
  }));

  it('should broadcast polling response via data subject', fakeAsync(() => {
    const fakePollResponse = new SearchResponse();

    autoPollingService.start();
    emitInitialResponse();

    autoPollingService.data.subscribe((result) => {
      expect(result).toBe(fakePollResponse);
      autoPollingService.stop();
    });

    tick(autoPollingService.getInterval() * 1000);

    fakeSearchObservable.next(fakePollResponse);
  }));

});
