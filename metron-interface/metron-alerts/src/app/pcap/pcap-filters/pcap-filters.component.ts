import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import * as moment from 'moment/moment';
import { DEFAULT_TIMESTAMP_FORMAT } from '../../utils/constants';

import { PcapRequest } from '../model/pcap.request';

@Component({
  selector: 'app-pcap-filters',
  templateUrl: './pcap-filters.component.html',
  styleUrls: ['./pcap-filters.component.scss']
})
export class PcapFiltersComponent implements OnInit {

  @Input() queryRunning: boolean = true;
  @Output() search: EventEmitter<PcapRequest> = new EventEmitter<PcapRequest>();
  
  startTimeStr: string;
  endTimeStr: string;

  model = new PcapRequest();

  ngOnInit() {
    const endTime = new Date();
    const startTime = new Date().setDate(endTime.getDate() - 5);
    
    this.startTimeStr = moment(startTime).format(DEFAULT_TIMESTAMP_FORMAT);
    this.endTimeStr = moment(endTime).format(DEFAULT_TIMESTAMP_FORMAT);
  }

  onSubmit() {
    this.model.startTime = new Date(this.startTimeStr).getTime();
    this.model.endTime = new Date(this.endTimeStr).getTime();
    this.search.emit(this.model);
  }
}
