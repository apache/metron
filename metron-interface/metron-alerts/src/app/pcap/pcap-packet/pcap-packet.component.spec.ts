import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { PcapPacketComponent } from './pcap-packet.component';

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
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
