import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Data, NavigationEnd, Router } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';

import { StructureModel } from '../core/store';
import { routing } from '../core/services/routing.service';
import { SpinnerService, UserlistFiltersService, UserListService } from '../core/services';
import { UsersStore } from './users.store';

@Component({
    selector: 'users-root',
    template: `
        <div class="flex-header">
            <h1><i class="fa fa-user"></i>
                <s5l>users.title</s5l>
            </h1>
            <button [routerLink]="['create']"
                    [class.hidden]="router.isActive('/admin/' + usersStore.structure?.id + '/users/create', true)">
                <s5l>create.user</s5l>
                <i class="fa fa-user-plus is-size-5"></i>
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
    providers: [UsersStore, UserListService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersComponent implements OnInit, OnDestroy {

    constructor(
        private route: ActivatedRoute,
        public router: Router,
        private cdRef: ChangeDetectorRef,
        public usersStore: UsersStore,
        private listFilters: UserlistFiltersService,
        private spinner: SpinnerService) {
    }

    private dataSubscriber: Subscription;
    private routerSubscriber: Subscription;

    ngOnInit(): void {
        this.dataSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
            if (data['structure']) {
                let structure: StructureModel = data['structure'];
                this.usersStore.structure = structure;
                this.initFilters(structure);
                this.cdRef.detectChanges();
            }
        });

        this.routerSubscriber = this.router.events.subscribe(e => {
            if (e instanceof NavigationEnd) {
                this.cdRef.markForCheck();
            }
        })
    }

    ngOnDestroy(): void {
        this.dataSubscriber.unsubscribe()
        this.routerSubscriber.unsubscribe()
    }

    closeCompanion() {
        this.router.navigate(['../users'], {relativeTo: this.route}).then(() => {
            this.usersStore.user = null;
        });
    }

    openUserDetail(user) {
        this.usersStore.user = user;
        this.spinner.perform('portal-content', this.router.navigate([user.id, 'details'], {relativeTo: this.route}));
    }

    openCompanionView(view) {
        this.router.navigate([view], {relativeTo: this.route});
    }

    private initFilters(structure: StructureModel) {
        this.listFilters.resetFilters();

        this.listFilters.setClassesComboModel(structure.classes);
        this.listFilters.setSourcesComboModel(structure.sources);
        this.listFilters.setFunctionsComboModel(structure.aafFunctions);
        this.listFilters.setProfilesComboModel(structure.profiles.map(p => p.name));
        this.listFilters.setFunctionalGroupsComboModel(
            structure.groups.data.filter(g => g.type === 'FunctionalGroup').map(g => g.name));
        this.listFilters.setManualGroupsComboModel(
            structure.groups.data.filter(g => g.type === 'ManualGroup').map(g => g.name));
        this.listFilters.setMailsComboModel([]);
    }
}