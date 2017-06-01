import {PageSize, RefreshInterval} from '../alerts/configure-rows/configure-rows-enums';
import {ColumnMetadata} from './column-metadata';

export class TableMetadata {
  size = PageSize.TWENTY_FIVE;
  refreshInterval = RefreshInterval.ONE_MIN;
  hideResolvedAlerts = true;
  hideDismissedAlerts = true;
  tableColumns: ColumnMetadata[];

  static fromJSON(obj: any): TableMetadata {
    let tableMetadata = new TableMetadata();
    if (obj) {
      tableMetadata.size = obj.size;
      tableMetadata.refreshInterval = obj.refreshInterval;
      tableMetadata.hideResolvedAlerts = obj.hideResolvedAlerts;
      tableMetadata.hideDismissedAlerts = obj.hideDismissedAlerts;
      tableMetadata.tableColumns = (typeof (obj.tableColumns) === 'string') ? JSON.parse(obj.tableColumns) : obj.tableColumns;
    }

    return tableMetadata;
  }
}
