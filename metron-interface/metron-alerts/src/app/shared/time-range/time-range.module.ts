import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {TimeRangeComponent} from './time-range.component';
import {DatePickerModule} from '../date-picker/date-picker.module';
import {SharedModule} from '../shared.module';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    DatePickerModule
  ],
  declarations: [TimeRangeComponent],
  exports: [TimeRangeComponent]
})
export class TimeRangeModule { }
