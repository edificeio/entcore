import { Pipe, PipeTransform } from '@angular/core'

@Pipe({
    name:"length",
    pure: false
})
export class LengthPipe implements PipeTransform {

    transform(source: Array<any> | string ) {
        return source.length;
    }

}
