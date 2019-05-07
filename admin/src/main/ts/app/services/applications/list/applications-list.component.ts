import { Component } from "@angular/core";

@Component({
    selector: 'apps-list',
    template: `
        <services-list serviceName="applications">
        </services-list>
    `
})
export class ApplicationsListComponent {

    constructor() {}

}