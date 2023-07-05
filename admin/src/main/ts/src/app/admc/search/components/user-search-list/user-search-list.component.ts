import { OdeComponent } from 'ngx-ode-core';
import { Component, ContentChild, EventEmitter, Input, Output, TemplateRef, Injector } from '@angular/core';
import { SearchTypeEnum } from 'src/app/core/enum/SearchTypeEnum';

@Component({
    selector: 'admc-user-search-list',
    templateUrl: './user-search-list.component.html',
    styleUrls: ['./user-search-list.component.scss']
})
export class AdmcUserSearchListComponent extends OdeComponent {

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

    @Input() model;
    @Input() filters;
    @Input() inputFilter;
    @Input() sort;
    @Input() limit: number;
    @Input() noResultsLabel = 'list.results.no.items';
    @Input() placeholder = 'list.placeholder';
    @Input() searchType:SearchTypeEnum = SearchTypeEnum.DISPLAY_NAME;
    @Input() isSearchActive = true;
    @Input() isSearchButtonDisabled: boolean = false;
    @Input() searchInput: boolean = false;
    @Input() searchDelay: number;
    @Input() searchSubmit: () => void;

    @Output() inputChange: EventEmitter<string> = new EventEmitter<string>();
    @Output() onSelect: EventEmitter<any> = new EventEmitter();
    @Output() listChange: EventEmitter<any> = new EventEmitter();
    @Output() scrolledDown: EventEmitter<any> = new EventEmitter();

    @ContentChild(TemplateRef) templateRef: TemplateRef<any>;
    @Input() isSelected = (arg?: any) => false;
    @Input() isDisabled = (arg?: any) => false;
    @Input() ngClass = (arg?: any) => ({});

    constructor(injector: Injector) {
        super(injector);
    }
}
