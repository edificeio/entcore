import { Component, Input, ViewChild, ChangeDetectorRef, ChangeDetectionStrategy, OnDestroy, OnInit } from '@angular/core'
import { AbstractControl, NgForm } from '@angular/forms'
import { LoadingService, NotifyService } from '../../../../services'
import { UserModel, StructureModel, UserDetailsModel } from '../../../../store'
import { Subscription } from 'rxjs/Subscription'
import { ActivatedRoute, Data, Router } from '@angular/router'
import { UsersStore } from '../../store'

@Component({
    selector: 'user-detail',
    templateUrl: './user-detail.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserDetail implements OnInit, OnDestroy{

    constructor(private ls: LoadingService,
        private ns: NotifyService,
        private usersStore: UsersStore,
        private cdRef: ChangeDetectorRef,
        private route: ActivatedRoute,
        private router: Router){}

    @ViewChild("codeInput") codeInput : AbstractControl
    @ViewChild("administrativeForm") administrativeForm : NgForm

    ngOnInit() {
        this.dataSubscriber = this.usersStore.onchange.subscribe(field => {
            if(field === 'user') {
                if(this.usersStore.user &&
                        !this.usersStore.user.structures.find(s => this.usersStore.structure.id === s.id)) {
                    setTimeout(() => {
                        this.router.navigate(['..'], {relativeTo: this.route, replaceUrl: true})
                    }, 0)
                } else if(this.user !== this.usersStore.user) {
                    this.user = this.usersStore.user
                }
            }
        })
        this.userSubscriber = this.route.data.subscribe((data: Data) => {
            this.usersStore.user = data['user']
        })
    }

    ngOnDestroy() {
        this.userSubscriber.unsubscribe()
        this.dataSubscriber.unsubscribe()
    }

    // Subscription
    private userSubscriber: Subscription
    private dataSubscriber: Subscription

    set user(user: UserModel) {
        this._user = user
        this.details = user.userDetails
        if(this.codeInput)
            this.codeInput.reset()
        if(this.administrativeForm)
            this.administrativeForm.reset()
    }
    get user(){ return this._user }
    private _user : UserModel
    private details : UserDetailsModel
    private structure: StructureModel = this.usersStore.structure

    private isContextAdml() {
        return this.details && this.details.functions &&
            this.details.functions[0][0] &&
            this.details.functions[0][1].find(id => this.structure.id === id)
    }

    private hasDuplicates() {
        return this.user.duplicates && this.user.duplicates.length > 0
    }

    private forceDuplicates : boolean
    private openDuplicates() {
        this.forceDuplicates = null
        setTimeout(() => {
            this.forceDuplicates = true
            this.cdRef.markForCheck()
            this.cdRef.detectChanges()
        }, 0)
    }

    private toggleUserBlock() {
        this.ls.perform('user.block', this.details.toggleBlock())
            .then(() => {
                this.user.blocked = !this.user.blocked

                this.ns.success(
                    { key: 'notify.user.toggleblock.content', 
                    parameters: { user: this.user.firstName + ' ' + this.user.lastName, blocked: this.user.blocked }},
                    { key: 'notify.user.toggleblock.title', 
                    parameters: { blocked: this.user.blocked }})
            }).catch(err => {
                 this.ns.error(
                    { key: 'notify.user.toggleblock.error.content',
                    parameters: { user: this.details.firstName + ' ' + this.user.lastName, blocked: !this.user.blocked }},
                    { key: 'notify.user.toggleblock.error.title',
                    parameters: { blocked: !this.user.blocked }}, 
                    err)
            })
    }
}