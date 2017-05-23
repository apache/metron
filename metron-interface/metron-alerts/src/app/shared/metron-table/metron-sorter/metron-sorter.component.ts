import { Component, Input } from '@angular/core';
import {MetronTableDirective, SortEvent} from '../metron-table.directive';
import {Sort} from '../../../utils/enums';

@Component({
  selector: 'metron-config-sorter',
  templateUrl: './metron-sorter.component.html',
  styleUrls: ['./metron-sorter.component.scss']
})
export class MetronSorterComponent {

  @Input() sortBy: string;
  @Input() type = 'string';

  sortAsc = false;
  sortDesc = false;

  constructor(private metronTable: MetronTableDirective ) {
    this.metronTable.onSortColumnChange.subscribe((event: SortEvent) => {
      this.sortAsc = (event.sortBy === this.sortBy && event.sortOrder === Sort.ASC);
      this.sortDesc = (event.sortBy === this.sortBy && event.sortOrder === Sort.DSC);
    });
  }

  sort() {
    let order = this.sortAsc ? Sort.DSC : Sort.ASC;
    this.metronTable.setSort({sortBy: this.sortBy, sortOrder: order, type: this.type});
  }
}
