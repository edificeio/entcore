import { Component, Injector, OnInit } from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { ServicesStore } from "src/app/services/services.store";

@Component({
    selector: 'ode-smart-widget',
    templateUrl: './smart-widget.component.html'
})
export class SmartWidgetComponent extends OdeComponent implements OnInit {
    constructor(injector: Injector, public servicesStore: ServicesStore) {
        super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.add(this.route.params.subscribe(params => {
            if (params.widgetId) {
                this.servicesStore.widget = this.servicesStore.structure.widgets.data.find(w => w.id === params.widgetId);
            }
        }));
    }
}