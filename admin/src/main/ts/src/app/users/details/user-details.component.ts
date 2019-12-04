import { OdeComponent } from './../../core/ode/OdeComponent';
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, ViewChild, Injector } from '@angular/core';
import {AbstractControl, NgForm} from '@angular/forms';
import {Data, NavigationEnd} from '@angular/router';

import {globalStore} from '../../core/store/global.store';
import {UsersStore} from '../users.store';
import {Config} from '../../core/resolvers/Config';
import { UserDetailsModel } from 'src/app/core/store/models/userdetails.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { UserModel } from 'src/app/core/store/models/user.model';
import { SpinnerService } from 'ngx-ode-ui';
import { NotifyService } from 'src/app/core/services/notify.service';
import { UserListService } from 'src/app/core/services/userlist.service';

@Component({
    selector: 'ode-user-detail',
    templateUrl: './user-details.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserDetailsComponent extends OdeComponent implements OnInit, OnDestroy {

    @ViewChild('codeInput', { static: false })
    codeInput: AbstractControl;
    @ViewChild('administrativeForm', { static: false })
    administrativeForm: NgForm;

    public config: Config;

    private SECONDS_IN_DAYS = 24 * 3600;
    private MILLISECONDS_IN_DAYS = this.SECONDS_IN_DAYS * 1000;

    public showRemoveUserConfirmation = false;
    forceDuplicates: boolean;
    details: UserDetailsModel;
    structure: StructureModel = this.usersStore.structure;
    imgSrc: string;
    imgLoaded = false;

    private _user: UserModel;
    set user(user: UserModel) {
        this._user = user;
        this.details = user.userDetails;
        if (this.codeInput) {
            this.codeInput.reset();
        }
        if (this.administrativeForm) {
            this.administrativeForm.reset();
        }
        this.imgSrc = '/userbook/avatar/' + user.id + '?thumbnail=100x100';
    }

    get user() {
        return this._user;
    }

    constructor(
        injector: Injector,
        public spinner: SpinnerService,
        private ns: NotifyService,
        private usersStore: UsersStore,
        private userListService: UserListService
    ) {
        super(injector);
    }

    ngOnInit() {
        super.ngOnInit();
        this.subscriptions.add(this.usersStore.$onchange.subscribe(field => {
            if (field === 'user') {
                if (this.usersStore.user &&
                    !this.usersStore.user.structures.find(
                        s => this.usersStore.structure.id === s.id)) {
                    setTimeout(() => {
                        this.router.navigate(['..'],
                            {relativeTo: this.route, replaceUrl: true});
                    }, 0);
                } else if (this.user !== this.usersStore.user || this.structure !== this.usersStore.structure) {
                    this.structure = this.usersStore.structure;
                    this.user = this.usersStore.user;
                }
            } else if (field === 'structure') {
                this.structure = this.usersStore.structure;
                this.changeDetector.markForCheck();
            }
        }));
        this.subscriptions.add(this.route.data.subscribe((data: Data) => {
            this.usersStore.user = data.user;
            this.config = data.config;
            this.changeDetector.markForCheck();
        }));
        // Scroll top in case of details switching, see comments on CAV2 #280
        this.router.events.subscribe(evt => {
            if (!(evt instanceof NavigationEnd)) {
                return;
            }
            window.scrollTo(0, 0);
        });

        // Fix userlist inactive information after user creation
        if (!this.user.code && this.user.userDetails.activationCode) {
            this.user.code = this.user.userDetails.activationCode;
        }

    }

    millisecondToDays(millisecondTimestamp: number): number {
        return Math.ceil(millisecondTimestamp / this.MILLISECONDS_IN_DAYS);
    }

    secondsToDays(timestamp: number): number {
        return Math.ceil(timestamp / this.SECONDS_IN_DAYS);
    }

    millisecondsUntilEffectiveDeletion(timestamp: number): number {
        return (timestamp + this.config['delete-user-delay']) - (new Date()).getTime();
    }

    millisecondsUntilPreDeletion(timestamp: number, profile: string): number {
        return (timestamp + this.config[profile.toLowerCase() + '-pre-delete-delay']) - (new Date()).getTime();
    }

   
    isContextAdml() {
        if (this.details && this.details.functions && this.details.functions.length > 0) {
            const admlIndex = this.details.functions.findIndex((f) => f[0] === 'ADMIN_LOCAL');
            if (admlIndex >= 0) {
                return this.details.functions[admlIndex][1].includes(this.structure.id);
            }
        }
    }

    hasDuplicates() {
        return this.user.duplicates && this.user.duplicates.length > 0;
    }

    openDuplicates() {
        this.forceDuplicates = null;
        setTimeout(() => {
            this.forceDuplicates = true;
            this.changeDetector.markForCheck();
            this.changeDetector.detectChanges();
        }, 0);
    }

    toggleUserBlock() {
        this.spinner.perform('portal-content', this.details.toggleBlock())
            .then(() => {
                this.user.blocked = !this.user.blocked;
                this.updateBlockedInStructures();
                this.userListService.$updateSubject.next();
                this.changeDetector.markForCheck();

                this.ns.success(
                    {
                        key: 'notify.user.toggleblock.content',
                        parameters: {
                            user: this.user.firstName + ' ' + this.user.lastName,
                            blocked: this.user.blocked
                        }
                    },
                    {
                        key: 'notify.user.toggleblock.title',
                        parameters: {
                            blocked: this.user.blocked
                        }
                    });
            }).catch(err => {
            this.ns.error(
                {
                    key: 'notify.user.toggleblock.error.content',
                    parameters: {
                        user: this.details.firstName + ' ' + this.user.lastName,
                        blocked: !this.user.blocked
                    }
                },
                {
                    key: 'notify.user.toggleblock.error.title',
                    parameters: {
                        blocked: !this.user.blocked
                    }
                },
                err);
        });
    }

    isUnblocked() {
        return !this.details.blocked;
    }

    isRemovable() {
        return ((this.user.disappearanceDate
            || (this.user.source !== 'AAF' && this.user.source !== "AAF1D" && this.user.source !== "EDT" && this.user.source !== "UDT"))
            && !this.user.deleteDate);
    }

    isActive() {
        return !(this.details.activationCode && this.details.activationCode.length > 0);
    }

    removeUser() {
        const parameters = {
            user: `${this.details.firstName} ${this.details.lastName}`,
            numberOfDays: this.millisecondToDays(this.config['delete-user-delay'])
        };

        this.spinner.perform('portal-content', this.user.delete(null, {params: {userId: this.user.id}}))
            .then(() => {
                this.user.deleteDate = Date.now();
                this.user.duplicates = [];
                this.updateDeletedInStructures();
                this.userListService.$updateSubject.next();
                this.changeDetector.markForCheck();

                if (this.isActive()) {
                    this.ns.success(
                        {key: 'notify.user.predelete.content', parameters},
                        {key: 'notify.user.predelete.title', parameters}
                    );
                } else {
                    this.usersStore.structure.users.data.splice(
                        this.usersStore.structure.users.data.findIndex(u => u.id === this.user.id), 1
                    );
                    this.router.navigate(['/admin', this.structure.id, 'users', 'filter']);
                    this.ns.success(
                        {key: 'notify.user.delete.content', parameters},
                        {key: 'notify.user.delete.title', parameters}
                    );
                }
            })
            .catch(err => {
                if (this.isActive()) {
                    this.ns.error(
                        {key: 'notify.user.predelete.error.content', parameters},
                        {key: 'notify.user.predelete.error.title', parameters},
                        err);
                } else {
                    this.ns.error(
                        {key: 'notify.user.delete.error.content', parameters},
                        {key: 'notify.user.delete.error.title', parameters},
                        err);
                }
            });
    }

    restoreUser() {
        this.spinner.perform('portal-content', this.user.restore())
            .then(() => {
                this.user.deleteDate = null;
                this.updateDeletedInStructures();
                this.userListService.$updateSubject.next();
                this.changeDetector.markForCheck();

                this.ns.success(
                    {
                        key: 'notify.user.restore.content',
                        parameters: {
                            user: this.details.firstName + ' ' + this.details.lastName
                        }
                    },
                    {
                        key: 'notify.user.restore.title',
                        parameters: {
                            user: this.details.firstName + ' ' + this.details.lastName
                        }
                    });
            })
            .catch(err => {
                this.ns.error(
                    {
                        key: 'notify.user.restore.error.content',
                        parameters: {
                            user: this.details.firstName + ' ' + this.details.lastName
                        }
                    },
                    {
                        key: 'notify.user.restore.error.title',
                        parameters: {
                            user: this.details.firstName + ' ' + this.details.lastName
                        }
                    },
                    err);
            });
    }

    deleteImg() {
        this.details.deletePhoto().then(() => {
            this.ns.success(
                {
                    key: 'notify.user.remove.photo.content',
                    parameters: {
                        user: this.details.firstName + ' ' + this.details.lastName
                    }
                },
                {
                    key: 'notify.user.remove.photo.title',
                    parameters: {
                        user: this.details.firstName + ' ' + this.details.lastName
                    }
                });
        }).catch(() => {
            this.ns.error(
                {
                    key: 'notify.user.remove.photo.error.content',
                    parameters: {
                        user: this.details.firstName + ' ' + this.details.lastName
                    }
                },
                {
                    key: 'notify.user.remove.photo.error.title',
                    parameters: {
                        user: this.details.firstName + ' ' + this.details.lastName
                    }
                });
        });
    }

    imgLoad() {
        this.imgLoaded = true;
    }

    openUserCommunication() {
        this.spinner.perform('portal-content', this.router.navigate([this.user.id, 'communication'], {relativeTo: this.route.parent}));
    }

    private updateDeletedInStructures() {
        this.user.structures.forEach(userStructure => {
            if (userStructure.id !== this.usersStore.structure.id) {
                const structure = globalStore.structures.data.find(gs => gs.id === userStructure.id);
                if (structure.users && structure.users.data && structure.users.data.length > 0) {
                    const user = structure.users.data.find(u => u.id === this.user.id);
                    if (user) {
                        user.deleteDate = this.user.deleteDate;
                    }
                }
            }
        });
    }

    private updateBlockedInStructures() {
        this.user.structures.forEach(userStructure => {
            if (userStructure.id !== this.usersStore.structure.id) {
                const structure = globalStore.structures.data.find(gs => gs.id === userStructure.id);
                if (structure.users && structure.users.data && structure.users.data.length > 0) {
                    const user = structure.users.data.find(u => u.id === this.user.id);
                    if (user) {
                        user.blocked = this.user.blocked;
                    }
                }
            }
        });
    }
}
