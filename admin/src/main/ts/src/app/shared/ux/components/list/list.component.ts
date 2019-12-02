import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { Component, ContentChild, EventEmitter, Input, Output, TemplateRef, Injector } from '@angular/core';

@Component({
    selector: 'ode-list',
    templateUrl: './list.component.html',
    styleUrls: ['./list.component.scss']
})
export class ListComponent extends OdeComponent {

    set storedElements(list) {
        this._storedElements = list;
        this.listChange.emit(list);
    }

    get storedElements() {
        return this._storedElements;
    }

    /* Store pipe */
    self = this;
    _storedElements = [];

    @Input() model = [];
    @Input() filters;
    @Input() inputFilter;
    @Input() sort;
    @Input() limit: number;
    @Input() searchPlaceholder = 'search';
    @Input() noResultsLabel = 'list.results.no.items';

    @Output() inputChange: EventEmitter<string> = new EventEmitter<string>();
    @Output() onSelect: EventEmitter<any> = new EventEmitter();
    @Output() listChange: EventEmitter<any> = new EventEmitter();
    @Output() scrolledDown: EventEmitter<any> = new EventEmitter();

    @ContentChild(TemplateRef, {static: false}) templateRef: TemplateRef<any>;
    @Input() isSelected = (arg?: any) => false;
    @Input() isDisabled = (arg?: any) => false;
    @Input() ngClass = (arg?: any) => ({});

    constructor(injector: Injector) {
        super(injector);
    }
}
