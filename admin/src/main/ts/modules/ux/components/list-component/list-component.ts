import { Component, Input, Output, ChangeDetectionStrategy,
    ChangeDetectorRef, EventEmitter, AfterViewInit } from '@angular/core'

@Component({
    selector: 'list-component',
    templateUrl: './list-component.html',
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

    @Input() display = (item) => { return item }

    @Output("inputChange") inputChange: EventEmitter<string> = new EventEmitter<string>()
    @Output("onSelect") onSelect: EventEmitter<{}> = new EventEmitter()

}