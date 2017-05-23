import { Component, OnInit } from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Rx';

import {ConfigureTableService} from '../../service/configure-table.service';
import {ClusterMetaDataService} from '../../service/cluster-metadata.service';
import {ColumnMetadata} from '../../model/column-metadata';

export enum AlertState {
  NEW, OPEN, ESCALATE, DISMISS, RESOLVE
}

export class ColumnMetadataWrapper {
  columnMetadata: ColumnMetadata;
  selected: boolean;

  constructor(columnMetadata: ColumnMetadata, selected: boolean) {
    this.columnMetadata = columnMetadata;
    this.selected = selected;
  }
}

@Component({
  selector: 'app-configure-table',
  templateUrl: './configure-table.component.html',
  styleUrls: ['./configure-table.component.scss']
})

export class ConfigureTableComponent implements OnInit {

  allColumns: ColumnMetadataWrapper[] = [];

  constructor(private router: Router, private activatedRoute: ActivatedRoute, private configureTableService: ConfigureTableService,
              private clusterMetaDataService: ClusterMetaDataService) { }

  goBack() {
    this.router.navigateByUrl('/alerts-list');
    return false;
  }

  indexOf(columnMetadata: ColumnMetadata, configuredColumns: ColumnMetadata[]): number {
    for (let i = 0; i < configuredColumns.length; i++) {
      if (configuredColumns[i].name === columnMetadata.name) {
        return i;
      }
    }
  }

  indexToInsert(columnMetadata: ColumnMetadata, allColumns: ColumnMetadata[], configuredColumnNames: string[]): number {
    let i = 0;
    for ( ; i < allColumns.length; i++) {
      if (configuredColumnNames.indexOf(allColumns[i].name) === -1 && columnMetadata.name.localeCompare(allColumns[i].name) === -1 ) {
        break;
      }
    }
    return i;
  }

  ngOnInit() {
    Observable.forkJoin(
      this.clusterMetaDataService.getDefaultColumns(),
      this.clusterMetaDataService.getColumnMetaData(),
        this.configureTableService.getTableMetadata()
    ).subscribe((response: any) => {
      this.prepareData(response[0], response[1], response[2].tableColumns);
    });
  }

  onSelectDeselectAll($event) {
    let checked = $event.target.checked;
    this.allColumns.forEach(colMetaData => colMetaData.selected = checked);
  }

  /* Slight variation of insertion sort with bucketing the items in the display order*/
  prepareData(defaultColumns: ColumnMetadata[], allColumns: ColumnMetadata[], savedColumns: ColumnMetadata[]) {
    let configuredColumns: ColumnMetadata[] = (savedColumns && savedColumns.length > 0) ?  savedColumns : defaultColumns;
    let configuredColumnNames: string[] = configuredColumns.map((mData: ColumnMetadata) => mData.name);

    allColumns = allColumns.filter((mData: ColumnMetadata) => configuredColumnNames.indexOf(mData.name) === -1);
    allColumns = allColumns.sort((mData1: ColumnMetadata, mData2: ColumnMetadata) => { return mData1.name.localeCompare(mData2.name); });

    let sortedConfiguredColumns = JSON.parse(JSON.stringify(configuredColumns));
    sortedConfiguredColumns = sortedConfiguredColumns.sort((mData1: ColumnMetadata, mData2: ColumnMetadata) => {
                                                                return mData1.name.localeCompare(mData2.name);
                                                          });

    while (configuredColumns.length > 0 ) {
      let columnMetadata = sortedConfiguredColumns.shift();

      let index = this.indexOf(columnMetadata, configuredColumns);
      let itemsToInsert: any[] = configuredColumns.splice(0, index + 1);


      let indexInAll = this.indexToInsert(columnMetadata, allColumns, configuredColumnNames);
      allColumns.splice.apply(allColumns, [indexInAll, 0].concat(itemsToInsert));
    }

    this.allColumns = allColumns.map(mData => { return new ColumnMetadataWrapper(mData, configuredColumnNames.indexOf(mData.name) > -1); });
  }

  postSave() {
    this.configureTableService.fireTableChanged();
    this.goBack();
  }

  save() {
    let selectedColumns = this.allColumns.filter((mDataWrapper: ColumnMetadataWrapper) => mDataWrapper.selected)
                          .map((mDataWrapper: ColumnMetadataWrapper) => mDataWrapper.columnMetadata);
    this.configureTableService.saveColumnMetaData(selectedColumns).subscribe(() => {
      this.postSave();
    }, error => {
      console.log('Unable to save column preferences ...');
      this.postSave();
    });

  }

  selectColumn(columns: ColumnMetadataWrapper) {
    columns.selected = !columns.selected;
  }

  swapUp(index: number) {
    if (index > 0) {
      [this.allColumns[index], this.allColumns[index - 1]] = [this.allColumns[index - 1], this.allColumns[index]];
    }
  }

  swapDown(index: number) {
    if (index + 1 < this.allColumns.length) {
      [this.allColumns[index], this.allColumns[index + 1]] = [this.allColumns[index + 1], this.allColumns[index]];
    }
  }
}


