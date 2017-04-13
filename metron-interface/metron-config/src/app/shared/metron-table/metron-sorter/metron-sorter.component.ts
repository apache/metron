import { Component, Input } from '@angular/core';
import {Sort} from '../../../util/enums';
import {MetronTableDirective, SortEvent} from '../metron-table.directive';

@Component({
  selector: 'metron-config-sorter',
  templateUrl: './metron-sorter.component.html',
  styleUrls: ['./metron-sorter.component.scss']
})
export class MetronSorterComponent {

  @Input() sortBy: string;

  sortAsc: boolean = false;
  sortDesc: boolean = false;

  constructor(private metronTable: MetronTableDirective ) {
    this.metronTable.onSortColumnChange.subscribe((event: SortEvent) => {
      this.sortAsc = (event.sortBy === this.sortBy && event.sortOrder === Sort.ASC);
      this.sortDesc = (event.sortBy === this.sortBy && event.sortOrder === Sort.DSC);
    });
  }

  sort() {
    let order = this.sortAsc ? Sort.DSC : Sort.ASC;
    this.metronTable.setSort({sortBy: this.sortBy, sortOrder: order});
  }
}
