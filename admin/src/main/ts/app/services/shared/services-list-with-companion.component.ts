import { Component, ChangeDetectorRef, Input, Output,
    ContentChild, TemplateRef, EventEmitter, AfterViewInit } from "@angular/core";

import { ActivatedRoute, Router } from '@angular/router';

@Component({
    selector: 'services-list-with-companion',
    template: `
        <side-layout (closeCompanion)="closePanel()" [showCompanion]="showCompanion">
            <div side-card>
                <list-component
                [model]="model"
                sort="{{ sort }}"
                [inputFilter]="inputFilter"
                searchPlaceholder="{{ searchPlaceholder }}"
                noResultsLabel="{{ noResultsLabel }}"
                [isSelected]="isSelected"
                (inputChange)="inputChange.emit($event)"
                (onSelect)="onSelect.emit($event)">
                    <ng-template let-item>
                        <div>
                            {{ item.name }}
                            {{ item.icon }}
                        </div>                        
                    </ng-template>
                </list-component>
            </div>
            <div side-companion>
                <router-outlet></router-outlet>
            </div>
        </side-layout>
    `,
    styles: [`

    `]
})
export class ServicesListWithCompanionComponent implements AfterViewInit {
    

    /* Store pipe */
    self = this;
    _storedElements = [];

    constructor(
        public cdRef: ChangeDetectorRef,
        private router: Router,
        private route: ActivatedRoute){}

    ngAfterViewInit() {
        this.cdRef.markForCheck();
        this.cdRef.detectChanges();
    }

    @Input() showCompanion;

    @Input() model = [];
    @Input() filters;
    @Input() inputFilter;
    @Input() sort;
    @Input() limit: number;

    @Input() searchPlaceholder = "search";
    @Input() isSelected = () => false;
    @Input() isDisabled = () => false;
    @Input() ngClass = () => ({});

    @Input() listScroll = (event, list, cdRef) => {}

    @Input() noResultsLabel = "list.results.no.items";

    @Output("inputChange") inputChange: EventEmitter<string> = new EventEmitter<string>();
    @Output("onSelect") onSelect: EventEmitter<{}> = new EventEmitter();
    @Output("listChange") listChange: EventEmitter<any> = new EventEmitter();

    @ContentChild(TemplateRef) templateRef:TemplateRef<any>;

    closePanel() {
        this.router.navigate(['..'], { relativeTo: this.route });
    }
}