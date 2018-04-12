import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { PcapPanelComponent } from './pcap-panel.component';

describe('PcapPanelComponent', () => {
  let component: PcapPanelComponent;
  let fixture: ComponentFixture<PcapPanelComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PcapPanelComponent ]
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
