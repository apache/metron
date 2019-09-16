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
import { TestBed, fakeAsync } from '@angular/core/testing';
import { SearchService } from './search.service';
import { HttpTestingController, HttpClientTestingModule, TestRequest } from '@angular/common/http/testing';
import { AppConfigService } from './app-config.service';
import { SearchRequest } from 'app/model/search-request';
import { noop } from 'rxjs';
import { HttpUtil } from 'app/utils/httpUtil';

fdescribe('SearchService', () => {

  let searchService: SearchService;
  let mockBackend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ HttpClientTestingModule ],
      providers: [
        SearchService,
        { provide: AppConfigService, useClass: () => {
          return {
            getApiRoot: () => '/api/v1',
          }
        } },
      ]
    });

    searchService = TestBed.get(SearchService);
    mockBackend = TestBed.get(HttpTestingController);
  });

  it('should not swallow errors', fakeAsync(() => {
    searchService.search(new SearchRequest())
      .subscribe(
        noop,
        (error) => {
          expect(error.status).toBe(500);
        },
      );

    const expectedReq: TestRequest = mockBackend.expectOne('/api/v1/search/search');
    expect(expectedReq.request.method).toEqual('POST');

    expectedReq.error(new ErrorEvent('internal server error'), { status: 500 });
  }));

  it('should redirect to login on session expiration or unauthorized access', () => {
    spyOn(HttpUtil, 'navigateToLogin');

    searchService.search(new SearchRequest()).subscribe(
      noop,
      (error) => {
        expect(HttpUtil.navigateToLogin).toHaveBeenCalled();
      },
    );

    const expectedReq: TestRequest = mockBackend.expectOne('/api/v1/search/search');
    expect(expectedReq.request.method).toEqual('POST');

    expectedReq.error(new ErrorEvent('internal server error'), { status: 401 });
  });
});
