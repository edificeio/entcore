import { Component, Injector } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';

@Component({
    selector: 'ode-adml-home',
    template: `
        <div>
            <h1>
                <i class="fa fa-cog"></i><s5l>admin.title</s5l>
            </h1>
            <h3>
                <s5l>pick.a.structure</s5l>
            </h3>
        </div>
    `
})
export class AdmlHomeComponent extends OdeComponent {    

}
