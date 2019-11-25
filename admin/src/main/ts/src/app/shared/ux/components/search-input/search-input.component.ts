import {Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, Renderer2, ViewChild} from '@angular/core';
import {Observable, Subject, Subscription} from 'rxjs';
import {debounceTime, distinctUntilChanged} from 'rxjs/operators';


@Component({
    selector: 'ode-search-input',
    templateUrl: './search-input.component.html'
})
export class SearchInputComponent implements OnInit, OnDestroy {

    constructor(private _elRef: ElementRef,
                private _renderer: Renderer2) {}

    /* Inputs / Outputs / View */

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

    @Output() onChange: EventEmitter<string> = new EventEmitter<string>();

    @ViewChild('searchBox', { static: false}) searchBox: ElementRef;

    /* Internal logic */

    private $searchTerms = new Subject<string>();
    private observable: Observable<string>;
    private observer: Subscription;

    private evalAttributes() {
        const element = this._elRef.nativeElement;
        if (element && this.searchBox) {
            for (let i = 0; i < element.attributes.length; i++) {
                const attr = element.attributes[i];
                this._renderer.setAttribute(this.searchBox.nativeElement, attr.name, attr.value);
            }
        }
    }

    ngOnInit(): void {
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
        this.observer.unsubscribe();
    }

    search(str: string) {
        this.$searchTerms.next(str);
    }

}
