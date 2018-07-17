import { Component, Input, Output, EventEmitter } from '@angular/core';
import { PdmlPacket } from '../model/pdml';
import { PcapPagination } from '../model/pcap-pagination';

@Component({
  selector: 'app-pcap-list',
  templateUrl: './pcap-list.component.html',
  styleUrls: ['./pcap-list.component.scss']
})
export class PcapListComponent  {

  @Input() pagination: PcapPagination = new PcapPagination();
  @Input() packets: PdmlPacket[];
  @Output() pageUpdate: EventEmitter<number> = new EventEmitter();

  constructor() { }

  toggle(packet) {
    packet.expanded = !packet.expanded;
  }

  onPageChange() {
    this.pageUpdate.emit(this.pagination.from);
  }

}
