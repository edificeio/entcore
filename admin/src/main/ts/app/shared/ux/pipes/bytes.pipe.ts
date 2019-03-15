import { Pipe, PipeTransform } from '@angular/core';
import { toDecimal } from '../../utils'

@Pipe({
    name: 'bytes'
})
export class BytesPipe implements PipeTransform {

    transform(value: number, unit: number, decimal?: number): number {
        if(decimal) {
            return toDecimal((value/unit), decimal);
        }
        return Math.round(value/unit);
    }

}
