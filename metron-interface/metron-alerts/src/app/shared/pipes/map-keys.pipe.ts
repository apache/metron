import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'mapKeys'
})
export class MapKeysPipe implements PipeTransform {

  transform(value: any, args?: any): any {
    return value ? Object.keys(value) : [];
  }

}
