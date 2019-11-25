import {Pipe, PipeTransform} from '@angular/core';

@Pipe({name: 'keys'})
export class KeysPipe implements PipeTransform {
  transform(obj, args?: string[]): any {
    const keys = [];
    for (const key in obj) {
      if (key) {
        keys.push(key);
      }
    }
    return keys;
  }
}
