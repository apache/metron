import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TimeRangeComponent } from './time-range.component';
import { DatePickerComponent } from '../date-picker/date-picker.component';
import { MapKeysPipe } from '../pipes/map-keys.pipe';
import { FormsModule } from '@angular/forms';

describe('TimeRangeComponent', () => {
  let component: TimeRangeComponent;
  let fixture: ComponentFixture<TimeRangeComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule
      ],
      declarations: [ 
        TimeRangeComponent,
        DatePickerComponent,
        MapKeysPipe
     ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TimeRangeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
