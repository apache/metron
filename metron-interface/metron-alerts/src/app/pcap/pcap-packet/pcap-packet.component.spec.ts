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
import { fakePacket } from '../model/pdml.mock';

import { PcapPacketComponent } from './pcap-packet.component';
import { By } from '@angular/platform-browser';

describe('PcapPacketComponent', () => {
  let component: PcapPacketComponent;
  let fixture: ComponentFixture<PcapPacketComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PcapPacketComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PcapPacketComponent);
    component = fixture.componentInstance;
    component.packet = fakePacket;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should expand the packet`s proto fieldset', () => {

    const protos = fixture.debugElement.queryAll(By.css('[data-qe-id="proto"]'));

    protos.forEach((proto, i) => {

      expect(proto.query(By.css('[data-qe-id="proto-fields"]'))).toBeFalsy();
      proto.nativeElement.click();
      fixture.detectChanges();
      const fieldsContainer = proto.query(By.css('[data-qe-id="proto-fields"]'));
      expect(fieldsContainer).toBeDefined();

      const fields = fieldsContainer.queryAll(By.css('[data-qe-id="proto-field"]'));

      fields.forEach((field, j) => {
        const name = field.query(By.css('[data-qe-id="proto-field-name"]'));
        expect(name.nativeElement.textContent.trim()).toBe(fakePacket.protos[i].fields[j].name);
        const showname = field.query(By.css('[data-qe-id="proto-field-showname"]'));
        expect(showname.nativeElement.textContent.trim()).toBe(fakePacket.protos[i].fields[j].showname);
      });
    });
  });

  it('should render proto`s showname property', () => {
    const protos = fixture.debugElement.queryAll(By.css('[data-qe-id="proto"]'));
    protos.forEach((proto, i) => {
      expect(
        proto.query(By.css('[data-qe-id="proto-showname"]'))
          .nativeElement
          .textContent.trim()
      ).toBe(fakePacket.protos[i].showname);
    });
  });
});
