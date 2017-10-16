import { Component, OnInit, OnDestroy, ChangeDetectionStrategy } from "@angular/core";

@Component({
    selector: 'connectors-list',
    template: `
        <services-list-with-companion serviceName="connectors">
        </services-list-with-companion>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConnectorsListComponent  {
    constructor() {}
}