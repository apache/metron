import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { PcapRequest } from '../model/pcap.request'

@Component({
  selector: 'app-pcap-filters',
  templateUrl: './pcap-filters.component.html',
  styleUrls: ['./pcap-filters.component.scss']
})
export class PcapFiltersComponent implements OnInit {

  @Output() search: EventEmitter<PcapRequest> = new EventEmitter<PcapRequest>()
  
  model = new PcapRequest()
  
  constructor() { }

  ngOnInit() {
  }
  
  onSubmit() {
    this.search.emit(this.model)
  }
}
