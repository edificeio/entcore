import { PipeTransform, Pipe } from '@angular/core';

@Pipe({name: 'keys'})
export class KeysPipe implements PipeTransform {
  transform(obj, args:string[]) : any {
    let keys = [];
    for (let key in obj) {
      keys.push(key);
    }
    return keys;
  }
}
