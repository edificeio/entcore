import { LoadingService } from '../../../../services'
import { GroupsStore } from '../../store'
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core'
import { ActivatedRoute, Data, Router, NavigationEnd } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'
import { routing } from '../../../../routing/routing.utils'

@Component({
    selector: 'groups-root',
    template: `
        <div class="flex-header">
            <h1><i class="fa fa-users"></i><s5l>groups</s5l></h1>
            <button [routerLink]="['manual', 'create']" 
                [class.hidden]="router.isActive('/admin/' + groupsStore.structure?.id + '/groups/manual/create', true)">
                <s5l>create.group</s5l>
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
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupsRoot implements OnInit, OnDestroy {

     // Subscriberts
    private structureSubscriber: Subscription

    // Tabs
    private tabs = [
        { label: "manual.groups", view: "manual" },
        { label: "profile.groups", view: "profile" },
        { label: "functional.groups", view: "functional" }
    ]

    private routerSubscriber : Subscription
    private error: Error

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef,
        private groupsStore: GroupsStore,
        private ls: LoadingService) { }

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
