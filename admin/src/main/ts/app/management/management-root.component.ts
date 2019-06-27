import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core'
import { ActivatedRoute, Data, Router, NavigationEnd } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'
import { routing } from '../core/services/routing.service'

@Component({
    selector: 'management-root',
    template: `
        <div class="flex-header">
            <h1><i class="school"></i> {{ 'management.structure' | translate }}</h1>
        </div>

        <div class="tabs">
            <button class="tab" *ngFor="let tab of tabs"
                [disabled]="tab.disabled"
                [routerLink]="tab.view"
                [class.active]="isActive(tab)">
                {{ tab.label | translate }}
            </button>
        </div>

        <router-outlet></router-outlet>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ManagementRoot implements OnInit, OnDestroy {

     // Subscriberts
    private structureSubscriber: Subscription

    // Tabs
    tabs = [
        { label: "management.message.flash", view: "message-flash", subs: ["message-flash-edit", "message-flash-duplicate", "message-flash-create"]}
    ]

    private routerSubscriber : Subscription
    private error: Error

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef) { }

    ngOnInit(): void {
        // Watch selected structure
        this.structureSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
            if(data['structure']) {
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

    isActive(tab: {label: string, view: string, subs: string[]}): boolean {
        return this.router.isActive(this.router.createUrlTree([tab.view], {relativeTo: this.route}), false) ||
        (!!tab.subs && tab.subs.findIndex(sub => this.router.isActive(this.router.createUrlTree([sub], {relativeTo: this.route}), false)) != -1);
      }
}
