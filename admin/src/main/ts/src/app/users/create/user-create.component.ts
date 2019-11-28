import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Data, Router} from '@angular/router';
import {Location} from '@angular/common';
import {Subscription} from 'rxjs';

import {UsersStore} from '../users.store';
import {routing} from '../../core/services/routing.service';
import {UserModel} from '../../core/store/models/user.model';
import {SelectOption} from '../../shared/ux/components/multi-select/multi-select.component';
import { UserChildrenListService, UserListService } from 'src/app/core/services/userlist.service';
import { NotifyService } from 'src/app/core/services/notify.service';
import { SpinnerService } from 'src/app/core/services/spinner.service';

@Component({
    selector: 'ode-user-create',
    templateUrl: './user-create.component.html'
    ,
    providers: [UserChildrenListService]
})
export class UserCreateComponent implements OnInit, OnDestroy {

    newUser: UserModel = new UserModel();
    noClasses: Array<any> = [];
    private structureSubscriber: Subscription;

    public typeOptions: SelectOption<string>[] = ['Teacher', 'Personnel', 'Relative', 'Student', 'Guest'].map(t => ({
        value: t,
        label: t
    }));
    public classeOptions: SelectOption<{ id: string, name: string }[]>[] = [];

    constructor(
        public usersStore: UsersStore,
        private ns: NotifyService,
        private spinner: SpinnerService,
        private router: Router,
        private route: ActivatedRoute,
        private location: Location,
        private userListService: UserListService,
        public userChildrenListService: UserChildrenListService) {
    }

    ngOnInit(): void {
        this.usersStore.user = null;
        this.newUser.classes = null;
        this.newUser.type = 'Personnel';
        const {id, name} = this.usersStore.structure;
        this.newUser.structures = [{id, name}];
        this.classeOptions = [{value: null, label: 'create.user.sansclasse'}];

        this.structureSubscriber = routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {

                this.newUser.structures = [data.structure];
                this.classeOptions = [{value: null, label: 'create.user.sansclasse'}];
                this.classeOptions.push(...this.usersStore.structure.classes.map(c => ({value: [c], label: c.name})));
            }
        });
        this.newUser.userDetails.children = [];
    }

    ngOnDestroy(): void {
        this.structureSubscriber.unsubscribe();
    }

    createNewUser() {
        this.spinner.perform('portal-content', this.newUser.createNewUser(this.usersStore.structure.id)
            .then(res => {
                this.ns.success({
                        key: 'notify.user.create.content',
                        parameters: {
                            user: this.newUser.firstName + ' ' + this.newUser.lastName
                        }
                    }
                    , 'notify.user.create.title');

                this.newUser.id = res.data.id;
                this.newUser.source = 'MANUAL';
                this.newUser.displayName = `${this.newUser.lastName} ${this.newUser.firstName}`;
                if (this.newUser.classes == null) {
                    this.newUser.classes = [];
                }
                this.usersStore.structure.users.data.push(this.newUser);

                this.router.navigate(['/admin', this.usersStore.structure.id, 'users', res.data.id, 'details'], {
                    relativeTo: this.route,
                    replaceUrl: false
                });
            }).catch(err => {
                this.ns.error({
                        key: 'notify.user.create.error.content',
                        parameters: {
                            user: this.newUser.firstName + ' ' + this.newUser.lastName
                        }
                    }
                    , 'notify.user.create.error.title', err);
            })
        );
    }

    addChild(child) {
        if (this.newUser.userDetails.children.indexOf(child) < 0) {
            this.newUser.userDetails.children.push(child);
        }
    }

    removeChild(child) {
        const index = this.newUser.userDetails.children.indexOf(child);
        this.newUser.userDetails.children.splice(index, 1);
    }

    cancel() {
        this.location.back();
    }

    trim(input: string) {
        if (input && input.length > 0) {
            return input.trim();
        }
        return input;
    }
}
