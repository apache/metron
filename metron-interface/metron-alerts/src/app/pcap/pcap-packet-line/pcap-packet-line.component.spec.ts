import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { PcapPacketLineComponent } from './pcap-packet-line.component';

describe('PcapPacketLineComponent', () => {
  let component: PcapPacketLineComponent;
  let fixture: ComponentFixture<PcapPacketLineComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PcapPacketLineComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PcapPacketLineComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
