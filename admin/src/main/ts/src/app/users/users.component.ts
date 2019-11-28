import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Data, NavigationEnd, Router} from '@angular/router';
import {Subscription} from 'rxjs';

import {StructureModel} from '../core/store/models/structure.model';
import {routing} from '../core/services/routing.service';
import {UsersStore} from './users.store';
import { UserListService } from '../core/services/userlist.service';
import { UserlistFiltersService } from '../core/services/userlist.filters.service';
import { SpinnerService } from '../core/services/spinner.service';

@Component({
    selector: 'ode-users-root',
    templateUrl: './users.component.html',
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
        this.dataSubscriber = routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                const structure: StructureModel = data.structure;
                this.usersStore.structure = structure;
                this.initFilters(structure);
                this.cdRef.detectChanges();
            }
        });

        this.routerSubscriber = this.router.events.subscribe(e => {
            if (e instanceof NavigationEnd) {
                this.cdRef.markForCheck();
            }
        });
    }

    ngOnDestroy(): void {
        this.dataSubscriber.unsubscribe();
        this.routerSubscriber.unsubscribe();
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

        const filterAafFunctions: Array<Array<string>> = [];
        structure.aafFunctions.forEach(structureAafFunctions => {
            structureAafFunctions.forEach(structureAafFunction => {
                if (!includes(filterAafFunctions, [structureAafFunction[2], structureAafFunction[4]])) {
                    filterAafFunctions.push([structureAafFunction[2], structureAafFunction[4]]);
                }
            });
        });
        this.listFilters.setFunctionsComboModel(filterAafFunctions);

        this.listFilters.setProfilesComboModel(structure.profiles.map(p => p.name));
        this.listFilters.setFunctionalGroupsComboModel(
            structure.groups.data.filter(g => g.type === 'FunctionalGroup').map(g => g.name));
        this.listFilters.setManualGroupsComboModel(
            structure.groups.data.filter(g => g.type === 'ManualGroup').map(g => g.name));
        this.listFilters.setMailsComboModel([]);
    }
}

export function isEqual(arr1: Array<any>, arr2: Array<any>): boolean {
    if (arr1.length !== arr2.length) {
        return false;
    } else {
        for (let i = 0; i < arr1.length; i++) {
            if (arr1[i] !== arr2[i]) {
                return false;
            }
        }
        return true;
    }
}

export function includes(arr1: Array<any>, arr2: Array<any>) {
    let res = false;
    arr1.forEach(a => {
        if (isEqual(a, arr2)) {
            res = true;
        }
    });
    return res;
}
