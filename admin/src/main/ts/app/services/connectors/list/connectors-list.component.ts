import { Component } from "@angular/core";

@Component({
    selector: 'connectors-list',
    template: `
        <services-list-with-companion serviceName="connectors">
        </services-list-with-companion>
    `
})
export class ConnectorsListComponent  {
    constructor() {}
}