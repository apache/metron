import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { PcapPanelComponent } from './pcap-panel.component';
import { Component, Input } from '../../../../node_modules/@angular/core';
import { PdmlPacket } from '../model/pdml';
import { PcapService } from '../service/pcap.service';
import { PcapPagination } from '../model/pcap-pagination';

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
