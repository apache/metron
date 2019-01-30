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
import {
  TestBed,
  fakeAsync,
  tick,
  discardPeriodicTasks,
  getTestBed
} from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';
import { of } from 'rxjs';

import { PcapService } from './pcap.service';
import { PcapStatusResponse } from '../model/pcap-status-response';
import { PcapRequest } from '../model/pcap.request';
import { fakePdml, fakePacket } from '../model/pdml.mock';
import { fakePcapStatusResponse, fakePcapRequest } from '../model/pcap.mock';
import { AppConfigService } from '../../service/app-config.service';

const jobId = 'job_1234567890123_4567';
let pdmlJsonMock = fakePdml;
let injector: TestBed;
let pcapService: PcapService;
let mockBackend: HttpTestingController;

pdmlJsonMock['pdml']['packet'].push(fakePacket);

class FakeAppConfigService {

  getApiRoot() {
    return '/api/v1'
  }

  getLoginPath() {
    return '/login'
  }
}

describe('PcapService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        PcapService,
        { provide: AppConfigService, useClass: FakeAppConfigService }
      ]
    });
    injector = getTestBed();
    pcapService = injector.get(PcapService);
    mockBackend = injector.get(HttpTestingController);
  });

  afterEach(() => {
    mockBackend.verify();
  });

  describe('getPackets()', () => {
    it('should return an Observable<Pdml>', () => {
      let packets;

      pcapService.getPackets(jobId, 1).subscribe(r => {
        packets = r;
        expect(packets).toBeTruthy();
        expect(packets.pdml).toBeTruthy();
        expect(packets.pdml.packet.length).toBe(1);
        expect(packets.pdml.packet[0].protos.length).toBe(3);
      });

      const req = mockBackend.expectOne(
        `/api/v1/pcap/job_1234567890123_4567/pdml?page=1`
      );
      expect(req.request.method).toEqual('GET');
      req.flush(pdmlJsonMock);
    });
  });

  describe('pollStatus()', () => {
    it('should call getStatus() in intervals', fakeAsync(() => {
      const responseMock: PcapStatusResponse = fakePcapStatusResponse;
      const spy = spyOn(pcapService, 'getStatus').and.returnValue(
        of(responseMock)
      );
      let response;

      pcapService.pollStatus(jobId).subscribe(r => (response = r));
      tick(4000);
      expect(spy.calls.count()).toBe(1);
      tick(4000);
      expect(spy.calls.count()).toBe(2);
      discardPeriodicTasks();
    }));
  });

  describe('submitRequest()', () => {
    it('should return an Observable<PcapStatusResponse>', () => {
      const request: PcapRequest = fakePcapRequest;
      const responseMock: PcapStatusResponse = fakePcapStatusResponse;
      let response;

      pcapService.submitRequest(request).subscribe(r => {
        response = r;
        expect(response).toBeTruthy();
      });

      const req = mockBackend.expectOne(`/api/v1/pcap/fixed`);
      expect(req.request.method).toEqual('POST');
      req.flush(responseMock);
    });
  });

  describe('getStatus()', () => {
    it('should return an Observable<PcapStatusResponse>', () => {
      const responseMock: PcapStatusResponse = fakePcapStatusResponse;
      let response;

      pcapService.getStatus(jobId).subscribe(r => {
        response = r;
        expect(response).toBeTruthy();
      });

      const req = mockBackend.expectOne(`/api/v1/pcap/job_1234567890123_4567`);
      expect(req.request.method).toEqual('GET');
      req.flush(responseMock);
    });
  });

  describe('getRunningJob()', () => {
    it('should return an Observable<PcapStatusResponse>', () => {
      const responseMock: PcapStatusResponse = fakePcapStatusResponse;
      let response;

      pcapService.getRunningJob().subscribe(r => {
        response = r;
        expect(response).toBeTruthy();
      });

      const req = mockBackend.expectOne(`/api/v1/pcap?state=RUNNING`);
      expect(req.request.method).toEqual('GET');
      req.flush(responseMock);
    });
  });

  describe('getPcapRequest()', () => {
    it('should return an Observable<PcapRequest>', () => {
      const responseMock: PcapRequest = fakePcapRequest;
      let response;

      pcapService.getPcapRequest(jobId).subscribe(r => {
        response = r;
        expect(response).toBeTruthy();
      });

      const req = mockBackend.expectOne(
        `/api/v1/pcap/job_1234567890123_4567/config`
      );
      expect(req.request.method).toEqual('GET');
      req.flush(responseMock);
    });
  });

  describe('getDownloadUrl()', () => {
    it('should return a url with the correct page to download the pdml', () => {
      expect(pcapService.getDownloadUrl(jobId, 2)).toBe(
        `/api/v1/pcap/job_1234567890123_4567/raw?page=2`
      );
    });
  });
});
