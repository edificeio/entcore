import { Location } from '@angular/common';
import { Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { SelectOption, SpinnerService } from 'ngx-ode-ui';
import { NotifyService } from 'src/app/core/services/notify.service';
import { UserChildrenListService } from 'src/app/core/services/userlist.service';
import { routing } from '../../core/services/routing.service';
import { UserModel } from '../../core/store/models/user.model';
import { UsersStore } from '../users.store';
import { UserPosition } from 'src/app/core/store/models/userPosition.model';
import { UserPositionServices } from 'src/app/core/services/user-position.service';


@Component({
    selector: 'ode-user-create',
    templateUrl: './user-create.component.html',
    providers: [UserChildrenListService]
})
export class UserCreateComponent extends OdeComponent implements OnInit, OnDestroy {

    newUser: UserModel = new UserModel();
    noClasses: Array<any> = [];

    public typeOptions: SelectOption<string>[] = ['Teacher', 'Personnel', 'Relative', 'Student', 'Guest'].map(t => ({
        value: t,
        label: t
    }));
    public classeOptions: SelectOption<{ id: string, name: string }[]>[] = [];

    /** Check whether or not the new user may have positions in the structure. */
    get canHavePositions() {
        return this.newUser.type === 'Personnel';
    }
    /** List of all positions existing in structures the user is ADMx of. */
    positionList: UserPosition[];
    /** List of selectable positions = all positions except duplicates and those already assigned. */
    get filteredPositionList() {
        // Extract and trim names
        const filteredList = this.positionList?.map(position => position.name)
            // Remove empty and already selected names
            .filter(name => !this.newUser.userDetails.userPositions.some(value=> name.length===0 || value.name.trim()===name)) ?? [];
        // Remove remaining duplicates
        return filteredList.filter((name, index) => (index+1>=filteredList.length || filteredList.indexOf(name, index+1)<0))
            // return result as an array of UserPosition
            .map(name => ({name}));
    }

    newPosition: UserPosition = {name: "", source: "MANUAL"};
    set newPositionName(name) {
        name = name ? name.trim() : "";
        // Check if the name of this new position does not already exist in the list
        if(this.positionList && !this.positionList.some(position => position.name===name) ) {
            this.newPosition = {name, source: "MANUAL"};
            this.showNewPositionProposal = name && name.length;
        }
    }
    showNewPositionProposal = false;
    showUserPositionCreationLightbox = false;

    constructor(
        injector: Injector,
        public usersStore: UsersStore,
        private ns: NotifyService,
        private spinner: SpinnerService,
        private location: Location,
        private userPositionServices: UserPositionServices,
        public userChildrenListService: UserChildrenListService) {
            super(injector);
    }

    async ngOnInit() {
        super.ngOnInit();
        this.usersStore.user = null;
        this.newUser.classes = null;
        this.newUser.type = 'Personnel';
        const {id, name, externalId} = this.usersStore.structure;
        this.newUser.structures = [{id, name, externalId}];
        this.classeOptions = [{value: null, label: 'create.user.sansclasse'}];

        this.newPositionName = undefined;
        this.positionList = await this.spinner
          .perform('portal-content', this.userPositionServices.searchUserPositions())
          .catch(err => []);
        this.newUser.userDetails.userPositions = [];

        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {

                this.newUser.structures = [data.structure];
                this.classeOptions = [{value: null, label: 'create.user.sansclasse'}];
                this.classeOptions.push(...this.usersStore.structure.classes.map(c => ({value: [c], label: c.name})));
            }
        }));
        this.newUser.userDetails.children = [];
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

                this.router.navigate(['/admin', this.usersStore.structure.id, 'users', 'list', res.data.id, 'details'], {
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

    addPosition(position: UserPosition) {
        const name = position.name.trim();
        const structureId = this.usersStore.structure.id;
        // Search the structure for a UserPosition with this name.
        Promise.resolve(this.positionList.find(pos => pos.name==name && pos.structureId==structureId))
        .then( async (positionToAdd) => positionToAdd 
            ? positionToAdd
            // If none is found then create one before selecting it.
            : await this.spinner.perform<UserPosition|undefined>('portal-content', 
                this.userPositionServices.createUserPosition({name, structureId})
                .then(created => {
                    this.positionList.push(created);
                    this.ns.success(
                        "notify.user-position.create.success.content",
                        "notify.user-position.success.title"
                    );
                    return created;
                })
                .catch(err => {
                    this.ns.error({
                            key: 'notify.user-position.create.error.content',
                            parameters: {position: name}},
                        'notify.user-position.create.error.title',
                        err
                    );
                    return undefined;
                })
            )
        )
        .then(addedPosition => {
            if(addedPosition) {
                this.newUser.userDetails.userPositions.push(addedPosition);
                this.showNewPositionProposal = false;
            }
        });
    }

    removePosition(position: UserPosition) {
        this.newUser.userDetails.userPositions = this.newUser.userDetails.userPositions.filter((p) => p.id !== position.id);
    }

    addUserPositionToList(position: UserPosition | undefined) {
        if( position ) {
            this.positionList.push(position);
        }
        this.showUserPositionCreationLightbox = false;
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
