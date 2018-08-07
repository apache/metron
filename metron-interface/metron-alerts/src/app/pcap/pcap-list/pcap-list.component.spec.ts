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

import { PcapListComponent } from './pcap-list.component';
import { PcapPagination } from '../model/pcap-pagination';
import { PcapPaginationComponent } from '../pcap-pagination/pcap-pagination.component';
import { FormsModule } from '../../../../node_modules/@angular/forms';
import { PdmlPacket } from '../model/pdml';
import { Component, Input } from '@angular/core';
import { PcapPacketLineComponent } from '../pcap-packet-line/pcap-packet-line.component';
import { PcapPacketComponent } from '../pcap-packet/pcap-packet.component';

@Component({
  selector: '[app-pcap-packet-line]',
  template: ``,
})
class FakePcapPacketLineComponent {
  @Input() packet: PdmlPacket;
}

@Component({
  selector: 'app-pcap-packet',
  template: ``,
})
class FakePcapPacketComponent {
  @Input() packet: PdmlPacket;
}

describe('PcapListComponent', () => {
  let component: PcapListComponent;
  let fixture: ComponentFixture<PcapListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule
      ],
      declarations: [
        FakePcapPacketLineComponent,
        FakePcapPacketComponent,
        PcapListComponent,
        PcapPaginationComponent
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PcapListComponent);
    component = fixture.componentInstance;
    component.packets = [{name: 'test', protos: [], expanded: true}];
    component.pagination = new PcapPagination();
    component.pagination.total = 10;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit an event with onPageChange', () => {
    const incrementSpy = spyOn(component.pageUpdate, 'emit');
    component.onPageChange();
    expect(incrementSpy).toHaveBeenCalled();
  });

  it('should toggle packet details expansion with toggle()', () => {
    let packet = new PdmlPacket();
    component.toggle(packet);
    expect(packet.expanded).toBe(true);
    component.toggle(packet);
    expect(packet.expanded).toBe(false);
  });

  it('should execute toggle() when app-pcap-packet-line is clicked', () => {
    const packetLineDe  = fixture.debugElement.query(By.css('[data-qe-id="pcap-packet-line"]'));
    const incrementSpy = spyOn(component, 'toggle');
    packetLineDe.triggerEventHandler('click', null);
    expect(incrementSpy).toHaveBeenCalled();
  });
});
