import { Component, Input, ViewChild } from '@angular/core'
import { NgForm, AbstractControl } from '@angular/forms'

import { AbstractSection } from '../abstract.section'

import { SpinnerService, NotifyService, UserListService } from '../../../../core/services'
import { UserInfoService } from '../info/user-info.service';

import { globalStore } from '../../../../core/store'
import { UsersStore } from '../../../users.store'

@Component({
    selector: 'user-administrative-section',
    template: `
        <panel-section section-title="users.details.section.administrative">

            <form #administrativeForm="ngForm">
                <fieldset disabled="{{ user.deleteDate != null ? 'disabled' : '' }}">
                    <form-field label="firstName">
                        <input type="text" [(ngModel)]="details.firstName" 
                            name="firstName" required #firstNameInput="ngModel">
                        <form-errors [control]="firstNameInput"></form-errors>
                    </form-field>
                    <form-field label="lastName">
                        <input type="text" [(ngModel)]="details.lastName" 
                            name="lastName" required #lastNameInput="ngModel" >
                        <form-errors [control]="lastNameInput"></form-errors>
                    </form-field>
                    <form-field label="displayName">
                        <input type="text" [(ngModel)]="details.displayName" 
                            name="displayName" required #displayNameInput="ngModel" >
                        <form-errors [control]="displayNameInput"></form-errors>
                    </form-field>
                    <form-field label="birthDate">
                        <date-picker [(ngModel)]="details.birthDate" name="birthDate" 
                            #birthDateInput="ngModel" minDate="1900-01-01" maxDate="today" 
                            [disabled]="user.deleteDate != null">
                        </date-picker>
                        <form-errors [control]="birthDateInput"></form-errors>
                    </form-field>
                    <form-field label="address">
                        <input type="text" [(ngModel)]="details.address" name="address" >
                    </form-field>
                    <form-field label="zipCode">
                        <input type="text" [(ngModel)]="details.zipCode" name="zipCode" >
                    </form-field>
                    <form-field label="city">
                        <input type="text" [(ngModel)]="details.city" name="city">
                    </form-field>
                    <form-field label="email">
                        <input type="email" [(ngModel)]="details.email" name="email" 
                            #emailInput="ngModel" [pattern]="emailPattern" >
                        <form-errors [control]="emailInput"></form-errors>
                    </form-field>
                    <form-field label="homePhone">
                        <input type="tel" [(ngModel)]="details.homePhone" name="homePhone" >
                    </form-field>
                    <form-field label="mobilePhone">
                        <input type="tel" [(ngModel)]="details.mobile" name="mobile" >
                    </form-field>
                </fieldset>
            </form>
            
            <button (click)="updateDetails()" 
                class="is-pulled-right"
                [disabled]="administrativeForm.pristine || administrativeForm.invalid || spinner.isLoading('portal-content')"
                *ngIf="user.deleteDate == null">
                    <s5l>save.modifications</s5l>
                    <i class="fa fa-floppy-o"></i>
            </button>
        </panel-section>
    `,
    inputs: ['user', 'structure']
})
export class UserAdministrativeSection extends AbstractSection {

    @ViewChild('administrativeForm') 
    administrativeForm : NgForm
    
    @ViewChild('firstNameInput') 
    firstNameInput: AbstractControl
    
    @ViewChild('lastNameInput') 
    lastNameInput: AbstractControl

    constructor(
        private usersStore: UsersStore,
        private ns: NotifyService,
        public spinner: SpinnerService,
        private userListService: UserListService,
        private userInfoService: UserInfoService) {
        super()
    }

    protected onUserChange() {
        if(this.administrativeForm){
            this.administrativeForm.reset(this.details && this.details.toJSON())
        }
    }

    updateDetails() {
        this.spinner.perform('portal-content', this.details.update())
            .then(() => {
                if (this.firstNameInput && this.firstNameInput.dirty) {
                    this.user.firstName = this.details.firstName
                }
                if (this.lastNameInput && this.lastNameInput.dirty) {
                    this.user.lastName = this.details.lastName
                }
                this.updateInStructures()
                this.userListService.updateSubject.next();

                this.administrativeForm.reset(this.details && this.details.toJSON())

                this.ns.success(
                    { 
                        key: 'notify.user.update.content', 
                        parameters: {
                            user: this.details.firstName + ' ' + this.user.lastName 
                        } 
                    }, 'notify.user.update.title')

                this.userInfoService.setState(this.details);
            })
            .catch(err => {
                this.ns.error(
                    {
                        key: 'notify.user.update.error.content',
                        parameters: {
                            user: this.user.firstName + ' ' + this.user.lastName
                        }
                    }, 'notify.user.update.error.title', err)
            })
    }

    private updateInStructures() {
        this.user.structures.forEach(us => {
            if (us.id !== this.usersStore.structure.id) {
                let s = globalStore.structures.data.find(gs => gs.id === us.id)
                if (s.users && s.users.data && s.users.data.length > 0) {
                    let u = s.users.data.find(u => u.id === this.user.id)
                    if (u) {
                        u.firstName = this.user.firstName
                        u.lastName = this.user.lastName
                    }
                }
            }
        })
    }
}