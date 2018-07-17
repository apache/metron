import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { PcapRequest } from '../model/pcap.request'
import { FilterValidator } from './pcap-filter.validator';

@Component({
  selector: 'app-pcap-filters',
  templateUrl: './pcap-filters.component.html',
  styleUrls: ['./pcap-filters.component.scss']
})
export class PcapFiltersComponent {

  @Input() queryRunning: boolean = true;
  @Output() search: EventEmitter<PcapRequest> = new EventEmitter<PcapRequest>();
  
  model = new PcapRequest();
  validator = new FilterValidator();

  onSubmit() {
    if (this.validator.validate(this.model)) {
      this.search.emit(this.model);
    }
  }
}
