import { Pipe, PipeTransform } from '@angular/core'

@Pipe({
    name:"store"
})
export class StorePipe implements PipeTransform {

    transform(value, context, prop = "_storedRef") {
        context[prop] = value
        return value
    }

}
