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
import { async, ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';

import { PcapPanelComponent } from './pcap-panel.component';
import { Component, Input } from '../../../../node_modules/@angular/core';
import { PdmlPacket, Pdml } from '../model/pdml';
import { PcapService, PcapStatusResponse } from '../service/pcap.service';
import { PcapPagination } from '../model/pcap-pagination';
import { By } from '../../../../node_modules/@angular/platform-browser';
import { PcapRequest } from '../model/pcap.request';
import { defer } from 'rxjs/observable/defer';

@Component({
  selector: 'app-pcap-filters',
  template: '',
})
class FakeFilterComponent {
  @Input() queryRunning: boolean;
}

@Component({
  selector: 'app-pcap-list',
  template: '',
})
class FakePcapListComponent {
  @Input() packets: PdmlPacket[];
  @Input() pagination: PcapPagination;
}

class FakePcapService {
  getDownloadUrl() {
    return '';
  }
  submitRequest() {}
}

describe('PcapPanelComponent', () => {
  let component: PcapPanelComponent;
  let fixture: ComponentFixture<PcapPanelComponent>;
  let pcapService: PcapService;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        FakeFilterComponent,
        FakePcapListComponent,
        PcapPanelComponent,
      ],
      providers: [
        { provide: PcapService, useClass: FakePcapService },
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    pcapService = TestBed.get(PcapService);
    fixture = TestBed.createComponent(PcapPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should hold filter bar', () => {
    expect(fixture.debugElement.query(By.css('app-pcap-filters'))).toBeDefined();
  });

  it('should pass queryRunning to filter bar', () => {
    const myBoolean = new Boolean(true);
    component.queryRunning = myBoolean as boolean;
    fixture.detectChanges();
    const filterBar = fixture.debugElement.query(By.css('app-pcap-filters'));
    expect(filterBar.componentInstance.queryRunning).toBe(myBoolean);
  });

  it('should show download link if page/pdml availabe', () => {
    component.pdml = new Pdml();
    fixture.detectChanges();
    const submitButton = fixture.debugElement.query(By.css('[data-qe-id="download-link"]'));
    expect(submitButton).toBeTruthy();
  });

  it('should hide download link if page/pdml not availabe', () => {
    component.pdml = null;
    fixture.detectChanges();
    const submitButton = fixture.debugElement.query(By.css('[data-qe-id="download-link"]'));
    expect(submitButton).toBeFalsy();
  });

  it('should show the progress bar if the query is running', () => {
    expect(fixture.debugElement.query(By.css('.pcap-progress'))).toBeFalsy();
    component.progressWidth = 42;
    component.queryRunning = true;
    fixture.detectChanges();
    const progress = fixture.debugElement.query(By.css('.pcap-progress'));
    expect(progress).toBeTruthy();
    expect(progress.nativeElement.textContent).toBe(component.progressWidth + '%');
    expect(progress.attributes['aria-valuenow']).toBe(String(component.progressWidth));
    expect(progress.styles.width).toBe(component.progressWidth + '%');
  });

  it('should render the given error message', () => {
    expect(fixture.debugElement.query(By.css('[data-qe-id="error"]'))).toBeFalsy();
    component.errorMsg = 'something went wrong!';
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('[data-qe-id="error"]')).nativeElement.textContent.trim()).toBe(component.errorMsg);
  });

  it('should hide the progress bar if the query is not running', () => {
    expect(fixture.debugElement.query(By.css('.pcap-progress'))).toBeFalsy();
    component.queryRunning = false;
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('.pcap-progress'))).toBeFalsy();
  });

  it('should render the pcap list and the download link if a valid pdml is provided', fakeAsync(() => {

    const page = 42;
    const myPdml = new Pdml();
    myPdml.packets = [];

    pcapService.getPackets = jasmine.createSpy('getPackets').and.returnValue(
      defer(() => Promise.resolve(myPdml))
    );

    component.pdml = null;
    fixture.detectChanges();

    component.changePage(page);

    expect(fixture.debugElement.query(By.css('app-pcap-list'))).toBeFalsy();
    expect(fixture.debugElement.query(By.css('[data-qe-id="download-link"]'))).toBeFalsy();

    tick();
    fixture.detectChanges();

    const pcapList = fixture.debugElement.query(By.css('app-pcap-list'));

    expect(pcapList).toBeTruthy();
    expect((pcapList.componentInstance.pagination as PcapPagination).selectedPage).toBe(page, 'it should pass the selected page number');
    expect(pcapList.componentInstance.packets).toBe(myPdml.packets, 'it should pass the packets from the given pdml');
    expect(fixture.debugElement.query(By.css('[data-qe-id="download-link"]'))).toBeTruthy();
  }));

  it('should not render the pcap list and the download link if there is no pdml', fakeAsync(() => {

    pcapService.getPackets = jasmine.createSpy('getPackets').and.returnValue(
      defer(() => Promise.resolve(null))
    );

    component.pdml = null;
    fixture.detectChanges();

    component.changePage(42);

    expect(fixture.debugElement.query(By.css('app-pcap-list'))).toBeFalsy();
    expect(fixture.debugElement.query(By.css('[data-qe-id="download-link"]'))).toBeFalsy();

    tick();
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('app-pcap-list'))).toBeFalsy();
    expect(fixture.debugElement.query(By.css('[data-qe-id="download-link"]'))).toBeFalsy();
  }));

  it('should render the error message if the search response has no valid job id', fakeAsync(() => {
    const response = new PcapStatusResponse();
    response.jobId = '';
    response.description = 'error message';
    pcapService.submitRequest = jasmine.createSpy('submitRequest').and.returnValue(
      defer(() => Promise.resolve(response))
    );

    component.onSearch(new PcapRequest());

    tick();
    fixture.detectChanges();

    expect(
      fixture.debugElement.query(By.css('[data-qe-id="error"]'))
      .nativeElement
      .textContent.trim()
    ).toBe(response.description);
  }));

  it('should render the error message if the search request fails', fakeAsync(() => {

    pcapService.submitRequest = jasmine.createSpy('submitRequest').and.returnValue(
      defer(() => Promise.reject(new Error('search error')))
    );

    component.onSearch(new PcapRequest());

    tick();
    fixture.detectChanges();

    expect(
      fixture.debugElement.query(By.css('[data-qe-id="error"]'))
      .nativeElement
      .textContent.trim()
    ).toBe('Response message: search error. Something went wrong with your query submission!');
  }));

  it('should render the error message if the poll status request fails', fakeAsync(() => {

    const response = new PcapStatusResponse();
    response.jobId = '42';
    pcapService.submitRequest = jasmine.createSpy('submitRequest').and.returnValue(
      defer(() => Promise.resolve(response))
    );

    pcapService.pollStatus = jasmine.createSpy('pollStatus').and.returnValue(
      defer(() => Promise.reject(new Error('poll error')))
    );

    component.onSearch(new PcapRequest());

    tick();
    fixture.detectChanges();

    expect(
      fixture.debugElement.query(By.css('[data-qe-id="error"]'))
      .nativeElement
      .textContent.trim()
    ).toBe('Response message: poll error. Something went wrong with your status request!');
  }));

  it('should render the error message if the poll response`s job status is "failed"', fakeAsync(() => {
    const searchResponse = new PcapStatusResponse();
    searchResponse.jobId = '42';

    pcapService.submitRequest = jasmine.createSpy('submitRequest').and.returnValue(
      defer(() => Promise.resolve(searchResponse))
    );

    const pollResponse = new PcapStatusResponse();
    pollResponse.jobStatus = 'FAILED';
    pcapService.pollStatus = jasmine.createSpy('pollStatus').and.returnValue(
      defer(() => Promise.resolve(pollResponse))
    );

    component.onSearch(new PcapRequest());

    tick();
    fixture.detectChanges();

    expect(
      fixture.debugElement.query(By.css('[data-qe-id="error"]'))
      .nativeElement
      .textContent.trim()
    ).toBe(`Query status: ${pollResponse.jobStatus}. Check your filter criteria and try again!`);
  }));

  it('should render the progress if the poll status is neither "succeded" nor "failed"', fakeAsync(() => {
    const searchResponse = new PcapStatusResponse();
    searchResponse.jobId = '42';

    pcapService.submitRequest = jasmine.createSpy('submitRequest').and.returnValue(
      defer(() => Promise.resolve(searchResponse))
    );

    const pollResponse = new PcapStatusResponse();
    pollResponse.percentComplete = 86;
    pcapService.pollStatus = jasmine.createSpy('pollStatus').and.returnValue(
      defer(() => Promise.resolve(pollResponse))
    );

    component.progressWidth = 98;

    component.onSearch(new PcapRequest());

    tick();
    fixture.detectChanges();

    const progress = fixture.debugElement.query(By.css('.pcap-progress'));
    expect(progress.nativeElement.textContent).toBe(pollResponse.percentComplete + '%');
  }));

  it('should render the pcap list if the poll status is "succeeded"', fakeAsync(() => {
    const searchResponse = new PcapStatusResponse();
    searchResponse.jobId = '42';

    pcapService.submitRequest = jasmine.createSpy('submitRequest').and.returnValue(
      defer(() => Promise.resolve(searchResponse))
    );

    const pollResponse = new PcapStatusResponse();
    pcapService.pollStatus = jasmine.createSpy('pollStatus').and.returnValue(
      defer(() => Promise.resolve(pollResponse))
    );

    const myPdml = new Pdml();
    pcapService.getPackets = jasmine.createSpy('getPackets').and.returnValue(
      defer(() => Promise.resolve(myPdml))
    );

    component.onSearch(new PcapRequest());

    expect(fixture.debugElement.query(By.css('app-pcap-list'))).toBeFalsy();

    tick();
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('app-pcap-list'))).toBeDefined();
  }));
});
