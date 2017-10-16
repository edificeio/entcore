import { Component, OnInit, OnDestroy, ChangeDetectionStrategy,} from "@angular/core";

@Component({
    selector: 'apps-list',
    template: `
        <services-list-with-companion serviceName="applications">
        </services-list-with-companion>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ApplicationsListComponent {

    constructor() {}

}