import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { PcapListComponent } from './pcap-list.component';

describe('PcapListComponent', () => {
  let component: PcapListComponent;
  let fixture: ComponentFixture<PcapListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PcapListComponent ]
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
