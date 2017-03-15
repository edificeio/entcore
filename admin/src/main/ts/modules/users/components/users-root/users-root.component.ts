import { StructureModel } from '../../../../store'
import { routing } from '../../../../routing/routing.utils'
import { LoadingService } from '../../../../services'
import { UserlistFiltersService } from '../../../../services/userlist.filters.service'
import { UsersStore } from '../../store'
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core'
import { ActivatedRoute, Data, NavigationEnd, Router } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'

@Component({
    selector: 'users-root',
    template: `
        <h1><i class="fa fa-user"></i><s5l>users.title</s5l></h1>
        <side-layout (closeCompanion)="closeCompanion()"
                [showCompanion]="!router.isActive('/admin/' + usersStore.structure?.id + '/users', true)">
            <div side-card>
                <div class="round-button top-right-button"
                    (click)="openCreationView()"
                    [class.selected]="router.isActive('/admin/' + usersStore.structure?.id + '/users/create', true)"
                    [tooltip]="'create.user' | translate" position="top">+</div>
                <user-list [userlist]="usersStore.structure.users.data"
                    (listCompanionChange)="openCompanionView($event)"
                    [selectedUser]="usersStore.user"
                    (selectedUserChange)="openUserDetail($event)"></user-list>
            </div>
            <div side-companion>
                <router-outlet></router-outlet>
            </div>
        </side-layout>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersRoot implements OnInit, OnDestroy {

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef,
        private usersStore: UsersStore,
        private filtersService: UserlistFiltersService,
        private ls: LoadingService){}

    // Subscriptions
    private structureSubscriber : Subscription
    private routerSubscriber : Subscription

    ngOnInit(): void {
        this.structureSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
            if(data['structure']) {
                let structure: StructureModel = data['structure']
                this.usersStore.structure = structure
                this.filtersService.resetFilters()
                this.filtersService.setClasses(structure.classes)
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

    closeCompanion() {
        this.router.navigate(['../users'], {relativeTo: this.route }).then(() => {
            this.usersStore.user = null
        })
    }

    openUserDetail(user) {
        this.usersStore.user = user
        this.router.navigate([user.id], {relativeTo: this.route })
    }

    openCreationView() {
        this.router.navigate(['create'], { relativeTo: this.route })
    }

    openCompanionView(view) {
        this.router.navigate([view], { relativeTo: this.route })
    }

}