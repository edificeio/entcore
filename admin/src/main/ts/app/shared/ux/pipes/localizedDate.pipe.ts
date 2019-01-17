import { DatePipe } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';
import { BundlesService } from 'sijil';

@Pipe({
  name: 'localizedDate',
  pure: false
})
export class LocalizedDatePipe implements PipeTransform {

  constructor(private bundlesService: BundlesService) {
  }

  transform(value: any, pattern: string = 'mediumDate'): any {
    const datePipe: DatePipe = new DatePipe(this.bundlesService.currentLanguage);
    return datePipe.transform(value, pattern);
  }

}
