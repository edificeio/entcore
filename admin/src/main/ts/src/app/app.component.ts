import { AfterViewInit, Component, ElementRef, Injector, Type } from '@angular/core';
import http from 'axios';
import { OdeComponent } from 'ngx-ode-core';


@Component({
    selector: 'ode-admin-app',
    template: '<router-outlet></router-outlet>'
})
export class AppComponent extends OdeComponent implements AfterViewInit {
    private elementRef: ElementRef;

    constructor(injector: Injector) {
        super(injector);
        this.elementRef = injector.get<ElementRef>(ElementRef as Type<ElementRef>);
    }

    ngAfterViewInit() {
        super.ngAfterViewInit();
    }
}
