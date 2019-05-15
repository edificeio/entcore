import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core'
import { ActivatedRoute, Data, Router } from '@angular/router'
import { Location } from '@angular/common'
import { Subscription } from 'rxjs/Subscription'

import { UsersStore } from '../users.store'
import { routing } from '../../core/services/routing.service'
import { StructureModel } from '../../core/store/models/structure.model'
import { UserModel } from '../../core/store/models/user.model'
import { SpinnerService, NotifyService, UserListService, UserChildrenListService } from '../../core/services'

@Component({
    selector: 'user-create',
    template: `
        <div class="panel-header">
            <span><s5l>new.user.creation</s5l></span>
        </div>

        <panel-section class="thin">
            <form #createForm="ngForm" (ngSubmit)="createNewUser()">
                <form-field label="firstName">
                    <input type="text" 
                        [(ngModel)]="newUser.firstName" 
                        name="firstName" 
                        required 
                        pattern=".*\\S+.*" 
                        #firstNameInput="ngModel"
                        (blur)="newUser.firstName = trim(newUser.firstName)">
                    <form-errors [control]="firstNameInput"></form-errors>
                </form-field>

                <form-field label="lastName">
                    <input type="text" 
                        [(ngModel)]="newUser.lastName" 
                        name="lastName" 
                        required 
                        pattern=".*\\S+.*" 
                        (blur)="newUser.lastName = trim(newUser.lastName)"
                        #lastNameInput="ngModel">
                    <form-errors [control]="lastNameInput"></form-errors>
                </form-field>

                <form-field label="birthDate">
                    <date-picker [(ngModel)]="newUser.userDetails.birthDate" 
                        name="birthDate" 
                        minDate="1900-01-01" 
                        maxDate="today" 
                        [required]="newUser.type == 'Student' ? true : false"
                        #birthDateInput="ngModel">
                    </date-picker>
                    <form-errors [control]="birthDateInput"></form-errors>
                </form-field>

                <form-field label="profile">
                    <select [(ngModel)]="newUser.type" name="type">
                        <option value="Teacher"><s5l>Teacher</s5l></option>
                        <option value="Personnel"><s5l>Personnel</s5l></option>
                        <option value="Relative"><s5l>Relative</s5l></option>
                        <option value="Student"><s5l>Student</s5l></option>
                        <option value="Guest"><s5l>Guest</s5l></option>
                    </select>
                </form-field>

                <div class="children-container" *ngIf="newUser.type === 'Relative'">
                    <div class="empty"></div>
                    <div class="search">
                        <search-input
                            [delay]="300"
                            [attr.placeholder]="'search' | translate"
                            (onChange)="userChildrenListService.inputFilter = $event">
                        </search-input>

                        <div class="list-wrapper" 
                            *ngIf="userChildrenListService.inputFilter?.length > 0">
                            <ul>
                                <li *ngFor="let child of usersStore.structure.users.data | filter: {type: 'Student'} | filter: userChildrenListService.filterByInput"
                                    (click)="addChild(child)">
                                    {{ child.lastName | uppercase }} {{ child.firstName }}
                                </li>
                            </ul>
                        </div>
                    </div>
                    <div class="children-list">
                        <h3><s5l>create.user.selectedchildren</s5l></h3>
                        <ul>
                            <li *ngFor="let child of newUser.userDetails.children">
                                <span>
                                    {{ child.lastName | uppercase }} {{ child.firstName }}
                                </span>
                                <i  class="fa fa-times action" 
                                    (click)="removeChild(child)"
                                    [title]="'create.user.deselect.child' | translate">
                                </i>
                            </li>
                        </ul>
                    </div>
                </div>

                <form-field label="create.user.classe">
                    <select [(ngModel)]="newUser.classes" name="classes">
                        <option [ngValue]="noClasses">
                            <s5l>create.user.sansclasse</s5l>
                        </option>
                        <option *ngFor="let classe of usersStore.structure.classes" 
                            [ngValue]="[classe]">{{classe.name}}</option>
                    </select>
                </form-field>

                <div class="action">
                    <button type="button" class="cancel" (click)="cancel()">
                        <s5l>create.user.cancel</s5l>
                    </button>
                    <button class="create confirm" 
                        [disabled]="createForm.pristine || createForm.invalid">
                        <s5l>create.user.submit</s5l>
                    </button>
                </div>
            </form>
        </panel-section>
    `,
    providers: [ UserChildrenListService ]
})
export class UserCreate implements OnInit, OnDestroy {

    newUser: UserModel = new UserModel()
    noClasses : Array<any> = []
    private structureSubscriber : Subscription
    private searchChildrenTerm: string
    
    constructor(
        public usersStore: UsersStore,
        private ns: NotifyService,
        private spinner: SpinnerService,
        private router: Router,
        private route: ActivatedRoute,
        private location: Location,
        private userListService: UserListService,
        private userChildrenListService: UserChildrenListService) {}

    ngOnInit(): void {
        this.usersStore.user = null
        this.newUser.classes = this.noClasses
        this.newUser.type = 'Personnel'
        let {id, name} = this.usersStore.structure
        this.newUser.structures = [{id: id, name:name}]

        this.structureSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
            if(data['structure']) {
                this.newUser.structures = [data['structure']]
            }
        })
        this.newUser.userDetails.children = []
    }

    ngOnDestroy(): void {
        this.structureSubscriber.unsubscribe()
    }

    createNewUser() {
        this.spinner.perform('portal-content', this.newUser.createNewUser(this.usersStore.structure.id)
            .then(res => {
                this.ns.success({
                    key: 'notify.user.create.content',
                    parameters: {
                        user: this.newUser.firstName + ' ' + this.newUser.lastName}
                    }
                    , 'notify.user.create.title')
                    
                this.newUser.id = res.data.id;
                this.newUser.source = 'MANUAL';
                this.usersStore.structure.users.data.push(this.newUser);
                this.usersStore.user = this.newUser

                this.router.navigate(['/admin', this.usersStore.structure.id, 'users', res.data.id, 'details'], {relativeTo: this.route, replaceUrl: false})
            }).catch(err => {
                this.ns.error({
                        key: 'notify.user.create.error.content',
                        parameters: {
                            user: this.newUser.firstName + ' ' + this.newUser.lastName}
                        }
                    , 'notify.user.create.error.title', err)
            })
        )
    }

    addChild(child) {
        if (this.newUser.userDetails.children.indexOf(child) < 0) { 
            this.newUser.userDetails.children.push(child)
        }
    }

    removeChild(child) {
        const index = this.newUser.userDetails.children.indexOf(child);
        this.newUser.userDetails.children.splice(index, 1)
    }

    cancel() {
        this.location.back();
    }

    trim(input:string) {
        if (input && input.length > 0) {
            return input.trim()
        }
        return input
    }
}
