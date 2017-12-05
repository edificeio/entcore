import { Pipe, PipeTransform } from '@angular/core'

@Pipe({
    name:"limit",
    pure: false
})
export class LimitPipe implements PipeTransform {

    transform(array: Array<any>, limit: number, offset: number = 0) {
        return array.slice(offset, limit)
    }

}
