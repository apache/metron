import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { PcapListComponent } from './pcap-list.component';
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
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PcapListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
