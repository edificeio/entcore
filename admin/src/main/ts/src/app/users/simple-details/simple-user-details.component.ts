import { ChangeDetectionStrategy, Component, Injector, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { AbstractControl, NgForm } from '@angular/forms';
import { Data, NavigationEnd } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { SpinnerService } from 'ngx-ode-ui';
import { NotifyService } from '../../core/services/notify.service';
import { UserListService } from '../../core/services/userlist.service';
import { StructureModel } from '../../core/store/models/structure.model';
import { UserModel } from '../../core/store/models/user.model';
import { UserDetailsModel } from '../../core/store/models/userdetails.model';
import { Config } from '../../core/resolvers/Config';
import { globalStore } from '../../core/store/global.store';
import { UsersStore } from '../users.store';
import { SessionModel } from 'src/app/core/store/models/session.model';
import { Session } from 'src/app/core/store/mappings/session';


@Component({
    selector: 'ode-simple-user-detail',
    templateUrl: './simple-user-details.component.html',
    styleUrls: ['./simple-user-details.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SimpleUserDetailsComponent extends OdeComponent implements OnInit, OnDestroy {

    @ViewChild('codeInput', { static: false })
    codeInput: AbstractControl;

    @ViewChild('administrativeForm', { static: false })
    administrativeForm: NgForm;

    public config: Config;

    public showPersEducNatBlockingConfirmation = false;
    public showMultipleStructureLightbox = false;
    forceDuplicates: boolean;
    details: UserDetailsModel;
    structure: StructureModel = this.usersStore.structure;
    imgSrc: string;

    private session: Session;

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

    async ngOnInit() {
        super.ngOnInit();
        this.subscriptions.add(this.usersStore.$onchange.subscribe(field => {
            if (field === 'user') {
                if (!this.user || !this.user.userDetails || this.user.id !== this.usersStore.user.id ||
                    !this.structure || this.structure !== this.usersStore.structure) {
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

        this.session = await SessionModel.getSession();
    }
   
    isContextAdml() {
        if (this.structure && 
            this.details && 
            this.details.functions && 
            this.details.functions.length > 0) {
            const admlIndex = this.details.functions.findIndex((f) => f[0] === 'ADMIN_LOCAL');
            if (admlIndex >= 0) {
                return this.details.functions[admlIndex][1].includes(this.structure.id);
            }
        }
    }

    displayFullUserDetails() {
        if (this.user.structures.length > 1) {
            this.showMultipleStructureLightbox = true;
        } else {
            this.spinner.perform('portal-content', this.router.navigate(['admin', this.user.structures[0].id, 'users', 'list', this.user.id, 'details']));
        }
    }

    goToUserDetails(structure: StructureModel) {
        this.spinner.perform('portal-content', this.router.navigate(['admin', structure.id, 'users', 'list', this.user.id, 'details']));
    }

    toggleUserBlock(withLightbox: boolean) {
        if(withLightbox == true)
        {
            // Only display a lightbox for teachers & personnel
            if((this.details.type.indexOf("Teacher") > -1 || this.details.type.indexOf("Personnel") > -1) && this.details.structureNodes.length > 0)
            {
                this.showPersEducNatBlockingConfirmation = true;
                return;
            }
        }
        this.spinner.perform('portal-content', this.details.toggleBlock())
            .then(() => {
                this.user.blocked = !this.user.blocked;
                this.updateBlockedInStructures();
                this.userListService.$updateSubject.next();
                this.changeDetector.markForCheck();

                this.showPersEducNatBlockingConfirmation = false;
                this.ns.success(
                    {
                        key: 'notify.user.toggleblock.content',
                        parameters: {
                            user: this.details.firstName + ' ' + this.details.lastName,
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
                this.showPersEducNatBlockingConfirmation = false;
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
        return this.details != null && !this.details.blocked;
    }

    isAdmlOf(structure: StructureModel): boolean {
        if (this.session && this.session.isADMC()) {
            return true;
        }

        if (this.session && this.session.functions && this.session.functions["ADMIN_LOCAL"]) {            
            const { scope } = this.session.functions["ADMIN_LOCAL"];
            return scope.includes(structure.id);
        }
        
        return false;
    }

    private updateBlockedInStructures() {
        if (!this.usersStore.structure) {
            return;
        }

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
