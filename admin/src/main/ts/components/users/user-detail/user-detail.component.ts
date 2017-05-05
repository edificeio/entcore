import { Component, Input, ViewChild, ChangeDetectorRef, ChangeDetectionStrategy, OnDestroy, OnInit } from '@angular/core'
import { AbstractControl, NgForm } from '@angular/forms'
import { User } from '../../../models/mappings'
import { UserDetailsModel } from '../../../models'
import { LoadingService } from '../../../services'
import { structureCollection, StructureModel } from '../../../models'
import { Subscription } from 'rxjs'
import { ActivatedRoute, Data, Router } from '@angular/router'
import { UsersDataService } from '../../../services/users/users.data.service'

@Component({
    selector: 'user-detail',
    templateUrl: require('./user-detail.component.html'),
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserDetail implements OnInit, OnDestroy{

    constructor(private loadingService: LoadingService,
        private dataService: UsersDataService,
        private cdRef: ChangeDetectorRef,
        private route: ActivatedRoute,
        private router: Router){}

    @ViewChild("codeInput") codeInput : AbstractControl
    @ViewChild("administrativeForm") administrativeForm : NgForm

    ngOnInit() {
        this.dataSubscriber = this.dataService.onchange.subscribe(field => {
            if(field === 'user') {
                if(this.dataService.user &&
                        !this.dataService.user.structures.find(s => this.dataService.structure.id === s.id)) {
                    setTimeout(() => {
                        this.router.navigate(['..'], {relativeTo: this.route, replaceUrl: true})
                    }, 0)
                } else if(this.user !== this.dataService.user) {
                    this.user = this.dataService.user
                }
            }
        })
        this.userSubscriber = this.route.data.subscribe((data: Data) => {
            this.dataService.user = data['user']
        })
    }

    ngOnDestroy() {
        this.userSubscriber.unsubscribe()
        this.dataSubscriber.unsubscribe()
    }

    // Subscription
    private userSubscriber: Subscription
    private dataSubscriber: Subscription

    set user(user: User) {
        this._user = user
        this.details = user.userDetails
        if(this.codeInput)
            this.codeInput.reset()
        if(this.administrativeForm)
            this.administrativeForm.reset()
        this.cdRef.markForCheck()
    }
    get user(){ return this._user }
    private _user : User
    private details : UserDetailsModel
    private structure: StructureModel = this.dataService.structure

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
}