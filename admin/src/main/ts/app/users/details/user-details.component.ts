import { Component, Input, Output, ViewChild, ChangeDetectorRef, 
    ChangeDetectionStrategy, OnDestroy, OnInit } from '@angular/core'
import { AbstractControl, NgForm } from '@angular/forms'
import { ActivatedRoute, Data, Router, NavigationEnd } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'

import { SpinnerService, NotifyService, UserListService } from '../../core/services'
import { globalStore } from '../../core/store'
import { UserModel, UserDetailsModel, StructureModel } from '../../core/store/models'
import { UsersStore } from '../users.store'

@Component({
    selector: 'user-detail',
    template: `
    <div class="panel-header">
        <div>
            <span class="user-displayname">
                {{ details.lastName | uppercase }} {{ details.firstName }}
            </span>
            <span *ngIf="user.type === 'Student' && user.classes[0]" class="user-class">
                - {{ user.classes[0].name }}
            </span>
        </div>

        <div class="panel-header-sub">
            <span *ngIf="isContextAdml()" class="user-admin">
                <s5l>ADMIN_LOCAL</s5l> <i class="fa fa-cog"></i>
            </span>
            <span class="user-inactive" 
                *ngIf="details?.activationCode && details?.activationCode?.length > 0">
                <s5l>user.inactive</s5l> <i class='fa fa-lock'></i>
            </span>
        </div>

        <div class="panel-message" *ngIf="details?.blocked">
            <i class="fa fa-ban"></i>
            <s5l>user.blocked</s5l>

            <button class="action" (click)="toggleUserBlock()" 
                [disabled]="spinner.isLoading('portal-content')">
                <s5l>unblock</s5l>
                <i class="fa fa-check"></i>
            </button>
        </div>

        <div class="panel-message" *ngIf="hasDuplicates()">
            <i class="fonticon duplicates"></i>
            <s5l>user.has.duplicates</s5l>

            <button class="action" anchor="user-duplicates-section" 
                (click)="openDuplicates()">
                <s5l>manage.duplicates</s5l>
                <i class="fa fa-compress"></i>
            </button>
        </div>

        <div class="panel-message" *ngIf="user?.deleteDate">
            <i class="fa fa-times-circle"></i>
            <s5l>user.predeleted</s5l>

            <button class="action" (click)="restoreUser()" 
                [disabled]="spinner.isLoading('portal-content')">
                <s5l>restore</s5l>
                <i class="fa fa-upload"></i>
            </button>
        </div>

        <div class="panel-message yellow" *ngIf="!user?.deleteDate && user?.disappearanceDate">
            <i class="fonticon waiting-predelete"></i>
            <s5l>user.predeleted.waiting</s5l>
        </div>

        <div class="panel-header-content">
            <div class="left">
                <img src="/userbook/avatar/{{user.id}}?thumbnail=100x100">
                <div>
                    <button (click)="toggleUserBlock()" 
                        [disabled]="spinner.isLoading('portal-content')" 
                        class="relative"
                        *ngIf="isUnblocked()">
                        <s5l [s5l-params]="[details.blocked]">
                            toggle.account
                        </s5l>
                        <i class="fa fa-ban"></i>
                    </button>
                </div>
                <div *ngIf="isRemovable()">
                    <button (click)="removeUser()" 
                        [disabled]="spinner.isLoading('portal-content')">
                        <s5l>predelete.account</s5l>
                        <i class="fa fa-times-circle"></i>
                    </button>
                </div>
            </div>

            <div class="right" *ngIf="!user.deleteDate">
                <button class="big" disabled title="En construction">
                    <s5l>users.details.button.comm.rules</s5l>
                    <i class="fa fa-podcast"></i>
                </button>

                <button class="big" disabled title="En construction">
                    <s5l>users.details.button.app.rights</s5l>
                    <i class="fa fa-th"></i>
                </button>
            </div>
        </div>
    </div>

    <div>
        <user-info-section [user]="user" [structure]="structure">
        </user-info-section>

        <user-administrative-section [user]="user" [structure]="structure">
        </user-administrative-section>
        
        <user-duplicates-section [user]="user" [structure]="structure" 
            [open]="forceDuplicates">
        </user-duplicates-section>
        
        <user-relatives-section [user]="user" [structure]="structure" 
            *ngIf="!user.deleteDate">
        </user-relatives-section>
        
        <user-children-section [user]="user" [structure]="structure" 
            *ngIf="!user.deleteDate">
        </user-children-section>
        
        <panel-section section-title="users.details.section.functions" 
            [folded]="true" *ngIf="!user.deleteDate">
            <ul><li *ngFor="let f of user?.aafFunctions">{{ f }}</li></ul>
        </panel-section>

        <user-structures-section [user]="user" [structure]="structure" 
            *ngIf="!user.deleteDate">
        </user-structures-section>
        
        <user-classes-section [user]="user" [structure]="structure" 
            *ngIf="!user.deleteDate">
        </user-classes-section>

        <user-functionalgroups-section [user]="user" [structure]="structure" 
            *ngIf="!user.deleteDate">
        </user-functionalgroups-section>

        <user-manualgroups-section [user]="user" [structure]="structure" 
            *ngIf="!user.deleteDate">
        </user-manualgroups-section>
    </div>`,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserDetails implements OnInit, OnDestroy{

    @ViewChild("codeInput")
    codeInput : AbstractControl
    
    @ViewChild("administrativeForm")
    administrativeForm : NgForm
    
    private userSubscriber: Subscription
    private dataSubscriber: Subscription

    forceDuplicates : boolean
    details : UserDetailsModel
    structure: StructureModel = this.usersStore.structure
    
    private _user : UserModel
    set user(user: UserModel) {
        this._user = user
        this.details = user.userDetails
        if(this.codeInput)
            this.codeInput.reset()
        if(this.administrativeForm)
            this.administrativeForm.reset()
    }
    get user(){ return this._user }

    constructor(
        public spinner: SpinnerService,
        private ns: NotifyService,
        private usersStore: UsersStore,
        private cdRef: ChangeDetectorRef,
        private route: ActivatedRoute,
        private router: Router,
        private userListService: UserListService
    ){}

    ngOnInit() {
        this.dataSubscriber = this.usersStore.onchange.subscribe(field => {
            if(field === 'user') {
                if(this.usersStore.user &&
                        !this.usersStore.user.structures.find(
                            s => this.usersStore.structure.id === s.id)) {
                    setTimeout(() => {
                        this.router.navigate(['..'], 
                        {relativeTo: this.route, replaceUrl: true})
                    }, 0)
                } else if(this.user !== this.usersStore.user || this.structure !== this.usersStore.structure) {
                    this.structure = this.usersStore.structure
                    this.user = this.usersStore.user
                }
                this.cdRef.markForCheck()
            }
        })
        this.userSubscriber = this.route.data.subscribe((data: Data) => {
            this.usersStore.user = data['user']
            this.cdRef.markForCheck()
        })
        //Scroll top in case of details switching, see comments on CAV2 #280
        this.router.events.subscribe((evt) => {
            if (!(evt instanceof NavigationEnd)) {
                return;
            }
            window.scrollTo(0, 0)
        });

        // Fix userlist inactive information after user creation
        if(!this.user.code && this.user.userDetails.activationCode) {
            this.user.code = this.user.userDetails.activationCode;
        }
    }

    ngOnDestroy() {
        this.userSubscriber.unsubscribe()
        this.dataSubscriber.unsubscribe()
    }

    isContextAdml() {
        return this.details && this.details.functions &&
            this.details.functions[0][0] &&
            this.details.functions[0][1].find(id => this.structure.id === id)
    }

    hasDuplicates() {
        return this.user.duplicates && this.user.duplicates.length > 0
    }

    openDuplicates() {
        this.forceDuplicates = null
        setTimeout(() => {
            this.forceDuplicates = true
            this.cdRef.markForCheck()
            this.cdRef.detectChanges()
        }, 0)
    }

    toggleUserBlock() {
        this.spinner.perform('portal-content', this.details.toggleBlock())
            .then(() => {
                this.user.blocked = !this.user.blocked;
                this.updateBlockedInStructures();
                this.userListService.updateSubject.next();

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
                    })
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
                    err)
            })
    }

    isUnblocked() {
        return !this.details.blocked
    }

    isRemovable() {
        return ((this.user.disappearanceDate 
            || (this.user.source !== 'AAF' && this.user.source !== "AAF1D"))
            && !this.user.deleteDate)
    }

    removeUser() {
        this.spinner.perform('portal-content', this.user.delete(null, {params: {'userId' : this.user.id}}))
            .then(() => {
                this.user.deleteDate = Date.now();
                this.user.duplicates = [];
                this.updateDeletedInStructures();
                this.userListService.updateSubject.next();
                this.cdRef.markForCheck();
                
                this.ns.success(
                    { 
                        key: 'notify.user.remove.content',
                        parameters: { 
                            user: this.details.firstName + ' ' + this.details.lastName
                        }
                    },
                    { 
                        key: 'notify.user.remove.title',
                        parameters: { 
                            user: this.details.firstName + ' ' + this.details.lastName
                        }
                    })
            })
            .catch(err => {
                this.ns.error(
                    { 
                        key: 'notify.user.remove.error.content',
                        parameters: { 
                            user: this.details.firstName + ' ' + this.details.lastName
                        }
                    },
                    { 
                        key: 'notify.user.remove.error.title',
                        parameters: { 
                            user: this.details.firstName + ' ' + this.details.lastName
                        }
                    },
                    err)
            })
    }

    restoreUser() {
        this.spinner.perform('portal-content', this.user.restore())
            .then(() => {
                this.user.deleteDate = null;
                this.updateDeletedInStructures();
                this.userListService.updateSubject.next();
                this.cdRef.markForCheck();

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
                    })
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
                    err)
            })
    }

    private updateDeletedInStructures() {
        this.user.structures.forEach(us => {
            if (us.id !== this.usersStore.structure.id) {
                let s = globalStore.structures.data.find(gs => gs.id === us.id)
                if (s.users && s.users.data && s.users.data.length > 0) {
                    let u = s.users.data.find(u => u.id === this.user.id)
                    if (u) {
                        u.deleteDate = this.user.deleteDate
                    }
                }
            }
        })
    }

    private updateBlockedInStructures() {
        this.user.structures.forEach(us => {
            if (us.id !== this.usersStore.structure.id) {
                let s = globalStore.structures.data.find(gs => gs.id === us.id)
                if (s.users && s.users.data && s.users.data.length > 0) {
                    let u = s.users.data.find(u => u.id === this.user.id)
                    if (u) {
                        u.blocked = this.user.blocked
                    }
                }
            }
        })
    }
}