import {Injectable, Inject} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {Http} from '@angular/http';
import {Subject} from 'rxjs/Subject';

import {IAppConfig} from '../app.config.interface';
import {APP_CONFIG} from '../app.config';
import {ALERTS_COLUMN_NAMES} from '../utils/constants';
import {ColumnNames} from '../model/column-names';

@Injectable()
export class ColumnNamesService {

  static columnNameToDisplayValueMap = {};
  static columnDisplayValueToNameMap = {};

  constructor(private http: Http, @Inject(APP_CONFIG) private config: IAppConfig) {
  }

  list(): Promise<ColumnNames> {
    return Observable.create(observer => {
      let columnNames: ColumnNames[];
      try {
        columnNames = JSON.parse(localStorage.getItem(ALERTS_COLUMN_NAMES));
        ColumnNamesService.toMap(columnNames);
      } catch (e) {}

      columnNames = columnNames || [];

      observer.next(columnNames);
      observer.complete();

    }).toPromise();
  }

  save(columns: ColumnNames[]): Observable<{}> {
    return Observable.create(observer => {
      try {
        localStorage.setItem(ALERTS_COLUMN_NAMES, JSON.stringify(columns));
      } catch (e) {}
      ColumnNamesService.toMap(columns);
      observer.next({});
      observer.complete();

    });
  }

  public static getColumnDisplayValue(key: string) {
    if (!key) {
      return '';
    }

    let displayValue = ColumnNamesService.columnNameToDisplayValueMap[key];

    return displayValue ? displayValue : key;
  }

  public static getColumnDisplayKey(key: string) {
    if (!key) {
      return '';
    }

    let name = ColumnNamesService.columnDisplayValueToNameMap[key];

    return name ? name : '';
  }

  private static toMap(columnNames:ColumnNames[]) {
    ColumnNamesService.columnNameToDisplayValueMap = {};
    ColumnNamesService.columnDisplayValueToNameMap = {};

    columnNames.forEach(columnName => {
      ColumnNamesService.columnNameToDisplayValueMap[columnName.key] = columnName.displayValue;
      ColumnNamesService.columnDisplayValueToNameMap[columnName.displayValue] = columnName.key;
    });
  }
}
