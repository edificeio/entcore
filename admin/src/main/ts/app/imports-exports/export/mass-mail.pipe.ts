import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'filtersPipe'
})
export class FiltersPipe implements PipeTransform {
    transform(items: Array<any>, filters: Array<any>): Array<any> {
        if( !items || !filters){
            return items;
        }
        console.log("filtre")
        return items.filter(item =>
            filters['type'](item.profile)
        );
    }
}