import { StructureModel, UserModel } from '../../../../store'
import { routing } from '../../../../routing/routing.utils'
import { LoadingService, NotifyService } from '../../../../services'
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
    providers: [UsersStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersRoot implements OnInit, OnDestroy {

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef,
        public usersStore: UsersStore,
        private filtersService: UserlistFiltersService,
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

    private initFilters(structure) {
        this.filtersService.resetFilters()
        this.filtersService.setClasses(structure.classes)

        if (structure.users && structure.users.data) {
            let profiles = []
            let sources = []
            let functions = []
            let matieres = []
            let functionalGroups = []

            const usersLength = structure.users.data.length
            for (let i = 0; i < usersLength; i++) {
                let u: UserModel = structure.users.data[i]

                if (profiles.indexOf(u.type) < 0) {
                    profiles.push(u.type)
                }
                if (sources.indexOf(u.source) < 0) {
                    sources.push(u.source)
                }
                if(u.aafFunctions) {
                    if (u.type === 'Personnel' || u.type === 'Teacher') {
                        const aafLength = u.aafFunctions.length
                        for (let i = 0; i < aafLength; i++) {
                            let f = u.aafFunctions[i]
                            switch (u.type) {
                                case 'Personnel':
                                    if (functions.indexOf(f) < 0) {
                                        functions.push(f)
                                    }
                                    break;
                                case 'Teacher':
                                    if (matieres.indexOf(f) < 0) {
                                        matieres.push(f)
                                    }
                                default:
                                    break;
                            }
                        }
                    }
                }

                if (u.functionalGroups) {
                    const fgLength = u.functionalGroups.length
                    for (let i = 0; i < fgLength; i++) {
                        let fg = u.functionalGroups[i];
                        if (functionalGroups.indexOf(fg) < 0) {
                            functionalGroups.push(fg)
                        }
                    }
                }
            }

            this.filtersService.setProfiles(profiles)
            this.filtersService.setSources(sources)
            this.filtersService.setFunctions(functions)
            this.filtersService.setMatieres(matieres)
            this.filtersService.setFunctionalGroupsFilter(functionalGroups)
        }
    }
}