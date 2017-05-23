import {Injectable, Inject} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {Http} from '@angular/http';
import {Subject} from 'rxjs/Subject';

import {IAppConfig} from '../app.config.interface';
import {APP_CONFIG} from '../app.config';
import {ALERTS_TABLE_METADATA} from '../utils/constants';
import {ColumnMetadata} from '../model/column-metadata';
import {TableMetadata} from '../model/table-metadata';

@Injectable()
export class ConfigureTableService {

  private tableChangedSource = new Subject<string>();
  tableChanged$ = this.tableChangedSource.asObservable();
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};

  constructor(private http: Http, @Inject(APP_CONFIG) private config: IAppConfig) {
  }

  fireTableChanged() {
    this.tableChangedSource.next('table changed');
  }

  getTableMetadata(): Observable<TableMetadata> {
    return Observable.create(observer => {
      let tableMetadata: TableMetadata;
      try {
        tableMetadata = TableMetadata.fromJSON(JSON.parse(localStorage.getItem(ALERTS_TABLE_METADATA)));
      } catch (e) {}

      observer.next(tableMetadata);
      observer.complete();

    });
  }

  saveColumnMetaData(columns: ColumnMetadata[]): Observable<{}> {
    return Observable.create(observer => {
      try {
        let  tableMetadata = TableMetadata.fromJSON(JSON.parse(localStorage.getItem(ALERTS_TABLE_METADATA)));
        tableMetadata.tableColumns = columns;
        localStorage.setItem(ALERTS_TABLE_METADATA, JSON.stringify(tableMetadata));
      } catch (e) {}

      observer.next({});
      observer.complete();

    });
  }

  saveTableMetaData(tableMetadata: TableMetadata): Observable<TableMetadata> {
    return Observable.create(observer => {
      try {
        localStorage.setItem(ALERTS_TABLE_METADATA, JSON.stringify(tableMetadata));
      } catch (e) {}

      observer.next({});
      observer.complete();

    });
  }

}
