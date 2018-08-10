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
  inject,
  fakeAsync,
  tick,
  discardPeriodicTasks
} from '@angular/core/testing';
import {
  BaseRequestOptions,
  HttpModule,
  Response,
  ResponseOptions,
  XHRBackend
} from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { Observable } from 'rxjs/Rx';

import { PcapService } from './pcap.service';
import { PcapStatusResponse } from '../model/pcap-status-response';
import { PcapRequest } from '../model/pcap.request';
import { fakePdml, fakePacket } from '../model/pdml.mock';
import { fakePcapStatusResponse, fakePcapRequest } from '../model/pcap.mock';

const jobId = 'job_1234567890123_4567';
let pdmlJsonMock = fakePdml;
pdmlJsonMock['pdml']['packet'].push(fakePacket);

describe('PcapService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpModule],
      providers: [
        PcapService,
        { provide: XHRBackend, useClass: MockBackend },
        MockBackend,
        BaseRequestOptions
      ]
    });
  });
  describe('getPackets()', () => {
    it('should return an Observable<Response>', inject(
      [PcapService, XHRBackend],
      (pcapService, mockBackend) => {
        let packets;

        mockBackend.connections.subscribe(connection => {
          expect(connection.request.url).toMatch(
            /\/api\/v1\/pcap\/job_1234567890123_4567\/pdml\?page=1/
          );
          connection.mockRespond(
            new Response(new ResponseOptions({ body: pdmlJsonMock }))
          );
        });
        pcapService.getPackets(jobId, 1).subscribe(r => (packets = r));
        expect(packets).toBeTruthy();
        expect(packets.pdml).toBeTruthy();
        expect(packets.pdml.packet.length).toBe(1);
        expect(packets.pdml.packet[0].protos.length).toBe(3);
      }
    ));
  });

  describe('pollStatus()', () => {
    it('should call getStatus() in intervals', fakeAsync(inject(
      [PcapService, XHRBackend], (pcapService, mockBackend) => {
          const responseMock: PcapStatusResponse = fakePcapStatusResponse;
          const spy = spyOn(pcapService, 'getStatus').and.returnValue(
            Observable.of(responseMock)
          );
          let response;

          pcapService.pollStatus(jobId).subscribe(r => (response = r));
          tick(4000);
          expect(spy.calls.count()).toBe(1);
          tick(4000);
          expect(spy.calls.count()).toBe(2);
          discardPeriodicTasks();
        })
      )
    );
  });

  describe('submitRequest()', () => {
    it('should return an Observable<PcapStatusResponse>', inject(
      [PcapService, XHRBackend],
      (pcapService, mockBackend) => {
        const request: PcapRequest = fakePcapRequest;
        const responseMock: PcapStatusResponse = fakePcapStatusResponse;
        let response;

        mockBackend.connections.subscribe(connection => {
          expect(connection.request.url).toMatch(/\/api\/v1\/pcap\/fixed/);
          connection.mockRespond(
            new Response(new ResponseOptions({ body: responseMock }))
          );
        });

        pcapService.submitRequest(request).subscribe(r => (response = r));
        expect(response).toBeTruthy();
      }
    ));
  });

  describe('getStatus()', () => {
    it('should return an Observable<PcapStatusResponse>', inject(
      [PcapService, XHRBackend],
      (pcapService, mockBackend) => {
        const responseMock: PcapStatusResponse = fakePcapStatusResponse;
        let response;

        mockBackend.connections.subscribe(connection => {
          expect(connection.request.url).toMatch(
            /\/api\/v1\/pcap\/job_1234567890123_4567/
          );
          connection.mockRespond(
            new Response(new ResponseOptions({ body: responseMock }))
          );
        });

        pcapService.getStatus(jobId).subscribe(r => (response = r));
        expect(response).toBeTruthy();
      }
    ));
  });

  describe('getRunningJob()', () => {
    it('should return an Observable<PcapStatusResponse>', inject(
            [PcapService, XHRBackend],
            (pcapService, mockBackend) => {
              const responseMock: PcapStatusResponse = fakePcapStatusResponse;
              let response;

              mockBackend.connections.subscribe(connection => {
                expect(connection.request.url).toMatch(
                        /\/api\/v1\/pcap\?state=RUNNING/
                );
                connection.mockRespond(
                        new Response(new ResponseOptions({ body: responseMock }))
                );
              });

              pcapService.getRunningJob().subscribe(r => (response = r));
              expect(response).toBeTruthy();
            }
    ));
  });

  describe('getPcapRequest()', () => {
    it('should return an Observable<PcapRequest>', inject(
            [PcapService, XHRBackend],
            (pcapService, mockBackend) => {
              const responseMock: PcapRequest = fakePcapRequest;
              let response;

              mockBackend.connections.subscribe(connection => {
                expect(connection.request.url).toMatch(
                        /\/api\/v1\/pcap\/job_1234567890123_4567\/config/
                );
                connection.mockRespond(
                        new Response(new ResponseOptions({ body: responseMock }))
                );
              });

              pcapService.getPcapRequest(jobId).subscribe(r => (response = r));
              expect(response).toBeTruthy();
            }
    ));
  });

  describe('getDownloadUrl()', () => {
    it('should return a url with the correct page to download the pdml', inject(
      [PcapService],
      pcapService => {
        expect(pcapService.getDownloadUrl(jobId, 2)).toBe(
          `/api/v1/pcap/job_1234567890123_4567/raw?page=2`
        );
      }
    ));
  });
});
