import { StructureModel, UserModel } from '../../../../store'
import { routing } from '../../../../routing/routing.utils'
import { UserlistFiltersService, LoadingService, NotifyService, ProfilesService } from '../../../../services'
import { UsersStore } from '../../store'
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core'
import { ActivatedRoute, Data, NavigationEnd, Router } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'

@Component({
    selector: 'users-root',
    template: `
        <div class="flex-header">
            <h1><i class="fa fa-user"></i><s5l>users.title</s5l></h1>
            <button (click)="openCreationView()"
                [class.hidden]="router.isActive('/admin/' + usersStore.structure?.id + '/users/create', true)">
                <s5l>create.user</s5l>
            </button>
        </div>
        <side-layout (closeCompanion)="closeCompanion()"
                [showCompanion]="!router.isActive('/admin/' + usersStore.structure?.id + '/users', true)">
            <div side-card>
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
    providers: [UsersStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersRoot implements OnInit, OnDestroy {

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef,
        public usersStore: UsersStore,
        private listFilters: UserlistFiltersService,
        private ls: LoadingService,
        private ns: NotifyService){}

    // Subscriptions
    private structureSubscriber : Subscription
    private routerSubscriber : Subscription

    ngOnInit(): void {
        this.structureSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
            if(data['structure'] && data['userlist']) {
                let structure: StructureModel = data['structure']
                this.usersStore.structure = structure
                this.initFilters(structure)
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

    private initFilters(structure: StructureModel) {
        this.listFilters.resetFilters()
        
        this.listFilters.setClasses(structure.classes)
        this.listFilters.setSources(structure.sources)
        this.listFilters.setFunctions(structure.aafFunctions)
        
        ProfilesService.getProfiles().then(p => this.listFilters.setProfiles(p))
        
        this.listFilters.setFunctionalGroups(
            structure.groups.data.filter(g => g.type === 'FunctionalGroup').map(g => g.name))
        this.listFilters.setManualGroups(
            structure.groups.data.filter(g => g.type === 'ManualGroup').map(g => g.name))
    }
    
}