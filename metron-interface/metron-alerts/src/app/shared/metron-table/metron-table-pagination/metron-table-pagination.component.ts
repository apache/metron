
import { Component, Input, Output, EventEmitter } from '@angular/core';
import {Pagination} from '../../../model/pagination';

@Component({
  selector: 'metron-table-pagination',
  templateUrl: './metron-table-pagination.component.html',
  styleUrls: ['./metron-table-pagination.component.scss']
})
export class MetronTablePaginationComponent  {

  @Output() pageChange = new EventEmitter();
  pagination = new Pagination();

  @Input()
  get pagingData() {
    return this.pagination;
  }

  set pagingData(val) {
    this.pagination = val;
  }

  onPrevious() {
    this.pagination.from -= this.pagination.size;
    this.pageChange.emit();
  }

  onNext() {
    this.pagination.from  += this.pagination.size;
    this.pageChange.emit();
  }
}
