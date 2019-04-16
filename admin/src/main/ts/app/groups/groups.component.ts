import { ChangeDetectionStrategy, ChangeDetectorRef, Component, 
    OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Data, Router, NavigationEnd } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';

import { SpinnerService, routing } from '../core/services';
import { GroupsStore } from './groups.store';
import { CommunicationRulesService } from '../users/communication/communication-rules.service';

@Component({
    selector: 'groups-root',
    template: `
        <div class="flex-header">
            <h1><i class="fa fa-users"></i><s5l>groups</s5l></h1>
            
            <button [routerLink]="['manual', 'create']" 
                [class.hidden]="router.isActive('/admin/' + groupsStore.structure?.id + '/groups/manual/create', true)">
                <s5l>create.group</s5l>
                <i class="fonticon group_add is-size-3"></i>
            </button>
        </div>
        
        <div class="tabs">
            <button class="tab" *ngFor="let tab of tabs"
                [routerLink]="tab.view"
                routerLinkActive="active">
                {{ tab.label | translate }}
            </button>
        </div>

        <router-outlet></router-outlet>
    `,
    providers: [CommunicationRulesService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupsComponent implements OnInit, OnDestroy {

     // Subscriberts
    private structureSubscriber: Subscription

    // Tabs
    tabs = [
        { label: "ManualGroup", view: "manual" },
        { label: "ProfileGroup", view: "profile" },
        { label: "FunctionalGroup", view: "functional" }
    ]

    private routerSubscriber : Subscription
    private error: Error

    constructor(
        private route: ActivatedRoute,
        public router: Router,
        private cdRef: ChangeDetectorRef,
        public groupsStore: GroupsStore,
        private ls: SpinnerService) { }

    ngOnInit(): void {
        // Watch selected structure
        this.structureSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
            if(data['structure']) {
                this.groupsStore.structure = data['structure']
                this.cdRef.markForCheck()
            }
        })

        this.routerSubscriber = this.router.events.subscribe(e => {
            if(e instanceof NavigationEnd)
                this.cdRef.markForCheck()
        })
    }

    ngOnDestroy(): void {
        this.structureSubscriber.unsubscribe()
        this.routerSubscriber.unsubscribe()
    }

    onError(error: Error){
        console.error(error)
        this.error = error
    }
}
