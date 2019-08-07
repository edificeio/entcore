import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, Output, EventEmitter } from '@angular/core'
import { BundlesService } from 'sijil'


@Component({
    selector: 'pager',
    template: `
        <strong><s5l>pager.page</s5l></strong>
        <a class="button" [ngClass]="{'is-hidden': offset - limit < 0}" (click)="previousPage()">
            <i class="fa fa-chevron-left"></i> <s5l>pager.page.previous</s5l>
        </a>
        <span>{{"pager.position" | translate : {offset : offset+1,  offsetLimit:offsetLimit(), total:total} }}</span>
        <a class="button" [ngClass]="{'is-hidden': offset + limit  > total}" (click)="nextPage()">
            <s5l>pager.page.next</s5l> <i class="fa fa-chevron-right"></i>
        </a>
    `,
    styles: [`
        i {cursor: pointer; padding: 0 2px;}
    `],
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
