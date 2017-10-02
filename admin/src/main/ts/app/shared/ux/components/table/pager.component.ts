import { 
    ChangeDetectionStrategy, ChangeDetectorRef, AfterViewInit,
    Component, Input, Output, EventEmitter } from '@angular/core'
import { BundlesService } from 'sijil'


@Component({
    selector: 'pager',
    template: `
    <button (click)="previousPage()" class="button" [disabled]="offset - limit < 0">
        {{'preious' | translate}}
    </button>
    <span>{{"pager.position" | translate : {offset : offset,  offsetLimit:offsetLimit(), total:total} }}</span>
    <button (click)="nextPage()" class="button" [disabled]="offset + limit  > total">
        {{'next' | translate}}
    </button>
    `,
    styles: [``],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class PagerComponent { 
    constructor (
        private bundles: BundlesService,
        private cdRef : ChangeDetectorRef)  {}
    
    translate = (...args) => { return (<any> this.bundles.translate)(...args) }

    @Input() limit: number;

    @Input() offset: number;
    @Output() offsetChange = new EventEmitter<number>();
    
    @Input() total: number;  
 
    ngOnInit() {}

    previousPage():void {
        if (this.offset - this.limit >= 0) {
            this.offset = this.offset - this.limit;
        } else {
            this.offset = 0;
        }
        this.offsetChange.emit(this.offset);
    }

    nextPage():void {
        if (this.offset + this.limit <= this.total) {
            this.offset = this.offset + this.limit;
        }
        this.offsetChange.emit(this.offset);
    }

    offsetLimit():number {
        return this.offset + this.limit < this.total ? 
            this.offset + this.limit : this.total;
    }
}
