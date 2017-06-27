import { Component, Input, Output, ChangeDetectionStrategy,
    ChangeDetectorRef, EventEmitter, AfterViewInit,
    TemplateRef, ContentChild } from '@angular/core'

@Component({
    selector: 'list-component',
    template: `
        <search-input [attr.placeholder]="searchPlaceholder | translate" (onChange)="inputChange.emit($event)"></search-input>
        <div class="toolbar">
            <ng-content select="[toolbar]"></ng-content>
        </div>
        <div class="list-wrapper" (scroll)="listScroll($event, model, cdRef)">
            <ul>
                <li *ngFor="let item of model | filter: filters | filter: inputFilter | orderBy: sort | slice: 0:limit"
                    (click)="onSelect.emit(item)"
                    [class.selected]="isSelected(item)"
                    [class.disabled]="isDisabled(item)"
                    [ngClass]="ngClass(item)">
                    <ng-template [ngTemplateOutlet]="templateRef" [ngOutletContext]="{$implicit: item}">
                    </ng-template>
                </li>
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

    @Output("inputChange") inputChange: EventEmitter<string> = new EventEmitter<string>()
    @Output("onSelect") onSelect: EventEmitter<{}> = new EventEmitter()

    @ContentChild(TemplateRef) templateRef:TemplateRef<any>;
}