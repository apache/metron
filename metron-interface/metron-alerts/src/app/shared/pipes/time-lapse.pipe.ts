import { Pipe, PipeTransform } from '@angular/core';
import * as moment from 'moment/moment';

@Pipe({
  name: 'timeLapse'
})
export class TimeLapsePipe implements PipeTransform {

  transform(value: any): any {
    if (isNaN(value)) {
      return '';
    }

    return moment(new Date(value)).fromNow();
  }

}
