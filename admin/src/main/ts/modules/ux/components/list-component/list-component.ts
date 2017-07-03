import { Component, Input, Output, ChangeDetectionStrategy,
    ChangeDetectorRef, EventEmitter, AfterViewInit,
    TemplateRef, ContentChild } from '@angular/core'
import { Subject } from 'rxjs/Subject'

@Component({
    selector: 'list-component',
    template: `
        <search-input [attr.placeholder]="searchPlaceholder | translate" (onChange)="inputChange.emit($event)"></search-input>
        <div class="toolbar">
            <ng-content select="[toolbar]"></ng-content>
        </div>
        <div class="list-wrapper" (scroll)="listScroll($event, model, cdRef)">
            <ul>
                <li *ngFor="let item of model | filter: filters | filter: inputFilter | orderBy: sort | slice: 0:limit | store:self:'storedElements'"
                    (click)="onSelect.emit(item)"
                    [class.selected]="isSelected(item)"
                    [class.disabled]="isDisabled(item)"
                    [ngClass]="ngClass(item)">
                    <ng-template [ngTemplateOutlet]="templateRef" [ngOutletContext]="{$implicit: item}">
                    </ng-template>
                </li>
            </ul>
            <ul *ngIf="storedElements && storedElements.length === 0">
                <li class="no-results">{{ noResultsLabel | translate }}</li>
            </ul>
        </div>
    `,
    styles: [`
        ul {
            margin: 0;
            padding: 0px;
            font-size: 0.9em;
        }

        ul li {
            cursor: pointer;
            border-top: 1px solid #ddd;
            padding: 10px 10px;
        }

        ul li.disabled {
            pointer-events: none;
        }
    `]
})
export class ListComponent implements AfterViewInit {

    /* Store pipe */
    self = this
    _storedElements = []

    constructor(private cdRef: ChangeDetectorRef){}
    ngAfterViewInit() {
        this.cdRef.markForCheck()
        this.cdRef.detectChanges()
    }

    @Input() model = []
    @Input() filters
    @Input() inputFilter
    @Input() sort
    @Input() limit: number

    @Input() searchPlaceholder = "search"
    @Input() isSelected = () => false
    @Input() isDisabled = () => false
    @Input() ngClass = () => ({})

    @Input() listScroll = (event, list, cdRef) => {}

    @Input() noResultsLabel = "list.results.no.items"

    @Output("inputChange") inputChange: EventEmitter<string> = new EventEmitter<string>()
    @Output("onSelect") onSelect: EventEmitter<{}> = new EventEmitter()
    @Output("listChange") listChange: EventEmitter<any> = new EventEmitter()

    @ContentChild(TemplateRef) templateRef:TemplateRef<any>;

    set storedElements(list) {
        this._storedElements = list
        this.listChange.emit(list)
    }

    get storedElements() {
        return this._storedElements
    }
}