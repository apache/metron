import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { PcapFiltersComponent } from './pcap-filters.component';
import { FormsModule } from '../../../../node_modules/@angular/forms';

describe('PcapFiltersComponent', () => {
  let component: PcapFiltersComponent;
  let fixture: ComponentFixture<PcapFiltersComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule
      ],
      declarations: [ PcapFiltersComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PcapFiltersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
