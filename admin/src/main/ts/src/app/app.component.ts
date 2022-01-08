import { Component } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';


@Component({
    selector: 'ode-admin-app',
    template: '<router-outlet></router-outlet>'
})
export class AppComponent extends OdeComponent {
    
}
