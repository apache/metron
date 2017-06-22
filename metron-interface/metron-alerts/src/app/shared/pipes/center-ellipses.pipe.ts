import { Pipe, PipeTransform } from '@angular/core';

const limit = 72;

@Pipe({
  name: 'centerEllipses'
})
export class CenterEllipsesPipe implements PipeTransform {
  private trail = '...';


  transform(value: any, length?: number): any {
      let tLimit = length ? length : limit;

      if (!value) {
        return '';
      }

      if (!length) {
        return value;
      }

      return value.length > tLimit
        ? value.substring(0, tLimit / 2) + this.trail + value.substring(value.length - tLimit / 2, value.length)
        : value;
  }

}
