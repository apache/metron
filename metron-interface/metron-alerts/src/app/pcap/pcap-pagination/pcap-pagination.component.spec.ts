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
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { PcapPagination } from '../model/pcap-pagination';
import { PcapPaginationComponent } from './pcap-pagination.component';

describe('PcapPaginationComponent', () => {
  let component: PcapPaginationComponent;
  let fixture: ComponentFixture<PcapPaginationComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PcapPaginationComponent ]
    })
    .compileComponents();
    fixture = TestBed.createComponent(PcapPaginationComponent);
    component = fixture.componentInstance;
    component.pagination = new PcapPagination();
    component.pagination.total = 10;
    fixture.detectChanges();
  }));

  beforeEach(() => {
  });

  it('should disable the back button if on the first page result', () => {
    const nextButton = fixture.debugElement.query(By.css('[data-qe-id="pcap-pagination-back"]')).nativeElement;
    expect(nextButton.disabled).toBe(true);
  });

  it('should disable the next button if on the last page result', () => {
    component.pagination.selectedPage = 10;
    fixture.detectChanges();
    const nextButton = fixture.debugElement.query(By.css('[data-qe-id="pcap-pagination-next"]')).nativeElement;
    expect(nextButton.disabled).toBe(true);
  });

  it('should increment the current page by 1 with onNext()', () => {
    component.onNext();
    expect(component.pagination.selectedPage).toBe(2);
  });

  it('should emit an event with onNext()', () => {
    const incrementSpy = spyOn(component.pageChange, 'emit');
    component.onNext();
    expect(incrementSpy).toHaveBeenCalled();
  });

  it('should decrement the current page by 1 with OnPrevious()', () => {
    component.pagination.selectedPage += 1;
    component.onPrevious();
    expect(component.pagination.selectedPage).toBe(1);
  });

  it('should emit an event with OnPrevious()', () => {
    const incrementSpy = spyOn(component.pageChange, 'emit');
    component.onPrevious();
    expect(incrementSpy).toHaveBeenCalled();
  });

});
