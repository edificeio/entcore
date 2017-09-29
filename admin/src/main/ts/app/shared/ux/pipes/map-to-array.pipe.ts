import {Pipe, PipeTransform} from '@angular/core';

@Pipe({name: 'mapToArray'})
export class MapToArrayPipe implements PipeTransform {
    transform(map: Map<any, any>, keyLabel?: string, valueLabel?: string): Object[] {
        let returnArray = [];

        keyLabel = keyLabel || 'key';
        valueLabel = valueLabel || 'value';

        map.forEach((value, key) => {
            let obj = {};
            obj[keyLabel] = key;
            obj[valueLabel] = value;
            returnArray.push(obj);
        });

        return returnArray;
    }
}