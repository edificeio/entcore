import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Data, NavigationEnd, Router} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {routing} from '../core/services';
import {CommunicationRulesService} from '../communication/communication-rules.service';
import {SubjectsStore} from "./subjects.store";

@Component({
    selector: 'subjects-root',
    templateUrl: './subjects.component.html',
    providers: [CommunicationRulesService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubjectsComponent implements OnInit, OnDestroy {

    // Tabs
    tabs = [
        {label: "CreateSubject", view: "create"},
        {label: "reconcile", view: "profile"},
    ];

    private error: Error;
    private structureSubscriber: Subscription;


    constructor(
        private route: ActivatedRoute,
        public router: Router,
        public subjectsStore: SubjectsStore) {
    }

    ngOnInit(): void {
        console.log("log ngOnInit subjects component");
        // Watch selected structure
        this.structureSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
            if (data['structure']) {
                this.subjectsStore.structure = data['structure'];
            }
        });

    }

    ngOnDestroy(): void {
        console.log("log ngOnDestroy");
    }

    onError(error: Error) {
        console.error(error);
        this.error = error;
    }
}