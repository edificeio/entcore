import {MatPaginatorIntl} from '@angular/material';
import { HttpClient } from '@angular/common/http';
import { BundlesService } from 'ngx-ode-sijil';

export class MatPaginatorIntlService extends MatPaginatorIntl {
    private of = 'of';

    constructor(private bundlesService: BundlesService) {
        super();

        this.itemsPerPageLabel = this.bundlesService.translate('mat.paginator.itemsPerPageLabel');
        this.nextPageLabel     = this.bundlesService.translate('mat.paginator.nextPageLabel');
        this.previousPageLabel = this.bundlesService.translate('mat.paginator.previousPageLabel');
        this.firstPageLabel = this.bundlesService.translate('mat.paginator.firstPageLabel');
        this.lastPageLabel = this.bundlesService.translate('mat.paginator.lastPageLabel');
        this.of = this.bundlesService.translate('mat.paginator.of');
    }

    getRangeLabel = (page: number, pageSize: number, length: number) => {
        if (length === 0 || pageSize === 0) {
            return `0 ${this.of} ${length}`;
        }
        length = Math.max(length, 0);
        const startIndex = page * pageSize;
        const endIndex = startIndex < length ? Math.min(startIndex + pageSize, length) : startIndex + pageSize;
        return `${startIndex + 1} – ${endIndex} ${this.of} ${length}`;
    }
}
