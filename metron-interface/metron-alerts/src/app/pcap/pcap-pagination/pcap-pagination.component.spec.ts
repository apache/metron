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
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PcapPaginationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should increment up in value when the right arrow is clicked', () => {
    component.pagination = new PcapPagination();
    const incrementSpy = spyOn(component.pageChange, 'emit');
    const nextButton = fixture.debugElement.query(By.css('.fa-chevron-right'));
    nextButton.triggerEventHandler('click', null);
    expect(incrementSpy).toHaveBeenCalled();
    expect(component.onNext).toHaveBeenCalled();
  });
});
