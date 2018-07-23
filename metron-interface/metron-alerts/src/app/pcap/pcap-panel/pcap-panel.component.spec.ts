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

import { PcapPanelComponent } from './pcap-panel.component';
import { Component, Input } from '../../../../node_modules/@angular/core';
import { PdmlPacket } from '../model/pdml';
import { PcapService } from '../service/pcap.service';

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
}

describe('PcapPanelComponent', () => {
  let component: PcapPanelComponent;
  let fixture: ComponentFixture<PcapPanelComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        FakeFilterComponent,
        FakePcapListComponent,
        PcapPanelComponent,
      ],
      providers: [
        { provide: PcapService, useValue: {} },
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PcapPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
