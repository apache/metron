import { Component, OnInit, ViewChild, ElementRef, OnChanges, SimpleChanges, Input, Output, EventEmitter } from '@angular/core';
import * as moment from 'moment/moment';
import * as Pikaday from "pikaday-time";

@Component({
  selector: 'app-date-picker',
  templateUrl: './date-picker.component.html',
  styleUrls: ['./date-picker.component.scss']
})
export class DatePickerComponent implements OnInit, OnChanges {
  defaultDateStr = 'now/d';
  picker: Pikaday;
  dateStr = this.defaultDateStr;

  @Input() date = '';
  @Input() minDate = '';
  @Output() dateChange = new EventEmitter<string>();
  @ViewChild('inputText') inputText: ElementRef;

  constructor(private elementRef: ElementRef) {}

  ngOnInit() {
    let _datePickerComponent = this;
    let pikadayConfig = {
      field: this.elementRef.nativeElement,
      showSeconds: true,
      use24hour: true,
      onSelect: function() {
        _datePickerComponent.dateStr = this.getMoment().format('YYYY-MM-DD HH:mm:ss');
        setTimeout(() => _datePickerComponent.dateChange.emit(_datePickerComponent.dateStr), 0);
      }
    };
    this.picker = new Pikaday(pikadayConfig);
    this.setDate();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes && changes['minDate'] && this.picker) {
      this.setMinDate();
    }

    if (changes && changes['date'] && this.picker) {
      this.setDate();
    }
  }

  setDate() {
    if (this.date === '') {
      this.dateStr = this.defaultDateStr;
    } else {
      this.dateStr = this.date;
      this.picker.setDate(this.dateStr);
    }
  }

  setMinDate() {
    let currentDate = new Date(this.dateStr).getTime();
    let currentMinDate = new Date(this.minDate).getTime();
    if (currentMinDate > currentDate) {
      this.dateStr = this.defaultDateStr;
    }
    this.picker.setMinDate(new Date(this.minDate));
    this.picker.setDate(moment(this.minDate).endOf('day').format('YYYY-MM-DD HH:mm:ss'));
  }

  toggleDatePicker($event) {
    if (this.picker) {
      if (this.picker.isVisible()) {
        this.picker.hide();
      } else {
        this.picker.show();
      }

      $event.stopPropagation();
    }
  }
}
