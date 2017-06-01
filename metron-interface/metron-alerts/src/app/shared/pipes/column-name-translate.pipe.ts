import { Pipe, PipeTransform } from '@angular/core';
import {ColumnNamesService} from '../../service/column-names.service';

@Pipe({
  name: 'columnNameTranslate',
  pure: false
})
export class ColumnNameTranslatePipe implements PipeTransform {

  transform(value: any, args?: any): any {
    if (!value) {
      return '';
    }

    let displayValue = ColumnNamesService.columnNameToDisplayValueMap[value];

    return displayValue ? displayValue : value;
  }
}
