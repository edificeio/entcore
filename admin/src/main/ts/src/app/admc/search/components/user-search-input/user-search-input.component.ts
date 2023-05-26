import { OdeComponent } from 'ngx-ode-core';
import { Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, Renderer2, ViewChild, Injector } from '@angular/core';
import {Observable, Subject, Subscription} from 'rxjs';
import {debounceTime, distinctUntilChanged} from 'rxjs/operators';
import { SearchTypeEnum } from 'src/app/core/enum/SearchTypeEnum';

export type UserSearchTerms = Array<string>;

@Component({
    selector: 'admc-user-search-input',
    templateUrl: './user-search-input.component.html',
    styleUrls: ['./user-search-input.component.scss']
})
export class AdmcUserSearchInputComponent extends OdeComponent implements OnInit, OnDestroy {

    constructor(injector: Injector,
                private _elRef: ElementRef,
                private _renderer: Renderer2) {
        super(injector);
    }

    /* Inputs / Outputs / View */
    @Input() isSearchActive: boolean = false;
    @Input() isSearchButtonDisabled: boolean = false;
    @Input() searchInput: boolean = false;
    @Input() searchSubmit: () => void;
    @Input() set delay(d: number) {
        this._delay = d;
        this.observable = this.$searchTerms.pipe(debounceTime(this.delay), distinctUntilChanged());
        if (this.observer) {
            this.observer.unsubscribe();
        }
        this.observer = this.observable.subscribe(val => {
            this.onChange.emit(val);
        });
    }
    get delay() {
        return this._delay;
    }
    private _delay = 200;

    @Input() searchType:SearchTypeEnum = SearchTypeEnum.DISPLAY_NAME;
    @Output() onChange: EventEmitter<UserSearchTerms> = new EventEmitter<UserSearchTerms>();

    @ViewChild('searchBox1') searchBox1: ElementRef;
    @ViewChild('searchBox2') searchBox2: ElementRef;

    /* Internal logic */

    private $searchTerms = new Subject<UserSearchTerms>();
    private observable: Observable<UserSearchTerms>;
    private observer: Subscription;

    private evalAttributes() {
        const element = this._elRef.nativeElement;
        [this.searchBox1, this.searchBox2].forEach( box => {
            if (element && box) {
                for (let i = 0; i < element.attributes.length; i++) {
                    const attr = element.attributes[i];
                    this._renderer.setAttribute(box.nativeElement, attr.name, attr.value);
                }
            }
        });
    }

    ngOnInit(): void {
        super.ngOnInit();
        if (!this.observable) {
            this.observable = this.$searchTerms
                .pipe(
                    debounceTime(this.delay),
                    distinctUntilChanged()
                );
            this.observer = this.observable.subscribe(val => {
                this.onChange.emit(val);
            });
        }
        setTimeout(() => this.evalAttributes(), 20);
    }

    ngOnDestroy(): void {
        super.ngOnDestroy();
        this.observer.unsubscribe();
    }

    get showSearchByFullname(): boolean {
        return this.searchType === SearchTypeEnum.DISPLAY_NAME;
    }
    
    get searchPlaceholders(): Array<string> {
        return this.showSearchByFullname 
            ? ['search.user.firstname', 'search.user.lastname'] 
            : ['search.user'];
    }

    search(s:UserSearchTerms) {
        this.$searchTerms.next( s );
    }

    handleSubmit() {
        this.searchSubmit();
    }

}
