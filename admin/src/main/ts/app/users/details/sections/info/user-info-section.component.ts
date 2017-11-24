import { Component, Input, ViewChild, ChangeDetectorRef, OnInit } from '@angular/core'
import { NgModel } from '@angular/forms'

import { AbstractSection } from '../abstract.section'
import { SpinnerService, NotifyService, PlateformeInfoService } from '../../../../core/services'

@Component({
    selector: 'user-info-section',
    template: `
    <panel-section section-title="users.details.section.infos">
        <form-field label="profile">
            <span>{{ user.type | translate }}</span>
        </form-field>
        <form-field label="login">
            <span>{{ details.login }}</span>
        </form-field>
        <form-field label="activation.code" *ngIf="details.activationCode">
            <span>{{ details.activationCode }}</span>
        </form-field>
        <form-field label="id">
            <span>{{ user.id }}</span>
        </form-field>
        <form-field label="externalId">
            <span>{{ details.externalId }}</span>
        </form-field>
        <form-field label="source">
            <span>{{ details.source | translate }}</span>
        </form-field>
        <form-field label="mergeKey">
            <span *ngIf="details.mergeKey">{{ details.mergeKey }}</span>
            <button class= "noflex"
                *ngIf="!details.mergeKey"
                (click)="generateMergeKey()">
                <s5l>generate</s5l>
                <i class="fa fa-cog"></i>
            </button>
        </form-field>
        <form-field label="administration" *ngIf="!user.deleteDate">
            <button class= "noflex"
                *ngIf="!details.isAdml(this.structure.id)" 
                (click)="addAdml()">
                <s5l>adml.add</s5l>
                <i class="fa fa-cog"></i>
            </button>
            <div *ngFor="let function of details.functions">
                <div class="adml-listing">
                    {{ function[0] | translate }}
                    <span *ngIf="function[1] && function[1].length > 0 && getStructure(function[1][0])">
                        ({{ getStructures(function[1]) }})
                    </span>
                    <span *ngIf="function[1] && function[1].length > 0 && !getStructure(function[1][0])">
                        ({{ 'member.of.n.structures' | translate:{ count: function[1].length } }})
                    </span>
                </div>
                <button *ngIf="details.isAdml(this.structure.id)" 
                    (click)="showConfirmation = true">
                    <s5l>adml.remove</s5l>
                    <i class="fa fa-cog"></i>
                </button>
                <confirm-light-box
                    [show]="showConfirmation"
                    [title]="'warning'"
                    (onConfirm)="removeAdml()"
                    (onCancel)="showConfirmation = false">
                    <p>{{ 'user.remove.adml.disclaimer.info' | translate:{ username: user.displayName } }}</p>
                    <p>{{ 'user.remove.adml.disclaimer.confirm' | translate }}</p>
                </confirm-light-box>
            </div>
        </form-field>
        <form-field label="send.reset.password" *ngIf="!details.activationCode">
            <div>
                <div class="sendPassword">
                    <input type="email" [(ngModel)]="passwordResetMail" name="passwordResetMail" 
                        [attr.placeholder]="'send.reset.password.email.placeholder' | translate"
                        #passwordMailInput="ngModel" [pattern]="emailPattern">
                    <button (click)="sendResetPasswordMail(passwordResetMail)" 
                        [disabled]="!passwordResetMail || passwordMailInput.errors">
                        <span><s5l>send.reset.password.button</s5l></span>
                        <i class="fa fa-envelope"></i>
                    </button>
                </div>

                <div class="sendPassword" *ngIf="smsModule">
                    <input type="tel" [(ngModel)]="passwordResetMobile" name="passwordResetMobile" 
                        [attr.placeholder]="'send.reset.password.mobile.placeholder' | translate"
                        #passwordMobileInput="ngModel">
                    <button class="mobile"
                        (click)="sendResetPasswordMobile(passwordResetMobile)" 
                        [disabled]="!passwordResetMobile || passwordMobileInput.errors ">
                        <span><s5l>send.reset.password.button</s5l></span>
                        <i class="fa fa-mobile"></i>
                    </button>
                </div>
            </div>
        </form-field>
    </panel-section>
    `,
    inputs: ['user', 'structure']
})
export class UserInfoSection extends AbstractSection implements OnInit {
    passwordResetMail: string
    passwordResetMobile: string
    smsModule: boolean
    showConfirmation: boolean = false;

    constructor(
        private ns: NotifyService,
        private spinner: SpinnerService,
        private cdRef: ChangeDetectorRef) {
        super()
    }

    ngOnInit() {
        this.passwordResetMail = this.details.email
        this.passwordResetMobile = this.details.mobile
        PlateformeInfoService.isSmsModule().then(res => {
            this.smsModule = res
            this.cdRef.markForCheck()
        })
    }

    protected onUserChange(){
        if(!this.details.activationCode) {
            this.passwordResetMail = this.details.email
            this.passwordResetMobile = this.details.mobile
        }
    }

    addAdml() {
        this.spinner.perform('portal-content', this.details.addAdml(this.structure.id))
            .then(res => {
                this.ns.success({
                        key: 'notify.user.add.adml.content',
                        parameters: {user: this.user.firstName + ' ' + this.user.lastName}
                    }, 'notify.user.add.adml.title');
                this.cdRef.markForCheck();
            }).catch(err => {
                this.ns.error({
                        key: 'notify.user.add.adml.error.content',
                        parameters: {user: this.user.firstName + ' ' + this.user.lastName}
                    }, 'notify.user.add.adml.error.title', err)
            })
    }

    removeAdml() {
        this.showConfirmation = false;
        this.spinner.perform('portal-content', this.details.removeAdml())
            .then(res => {
                this.ns.success({
                        key: 'notify.user.remove.adml.content',
                        parameters: {user: this.user.firstName + ' ' + this.user.lastName}
                    }, 'notify.user.remove.adml.title')
            }).catch(err => {
                this.ns.error({
                        key: 'notify.user.remove.adml.error.content',
                        parameters: {user: this.user.firstName + ' ' + this.user.lastName}
                    }, 'notify.user.remove.adml.error.title', err)
            })
    }

    sendResetPasswordMail(email: string) {
        this.spinner.perform('portal-content', this.details.sendResetPassword({type: 'email', value: email}))
            .then(res => {
                this.ns.success({
                        key: 'notify.user.sendResetPassword.email.content',
                        parameters: {
                            user: this.user.firstName + ' ' + this.user.lastName,
                            mail: email
                        }
                    }, 'notify.user.sendResetPassword.email.title')
            })
            .catch(err => {
                this.ns.error({
                        key: 'notify.user.sendResetPassword.email.error.content',
                        parameters: {
                            user: this.user.firstName + ' ' + this.user.lastName,
                            mail: email   
                        }
                    }, 'notify.user.sendResetPassword.email.error.title', err)
            })
    }

    sendResetPasswordMobile(mobile: string) {
        this.spinner.perform('portal-content', this.details.sendResetPassword({type: 'mobile', value: mobile}))
            .then(res => {
                this.ns.success({
                        key: 'notify.user.sendResetPassword.mobile.content',
                        parameters: {
                            user: this.user.firstName + ' ' + this.user.lastName,
                            mobile: mobile
                        }
                    }, 'notify.user.sendResetPassword.mobile.title')
            })
            .catch(err => {
                this.ns.error({
                        key: 'notify.user.sendResetPassword.mobile.error.content',
                        parameters: {
                            user: this.user.firstName + ' ' + this.user.lastName,
                            mobile: mobile   
                        }
                    }, 'notify.user.sendResetPassword.mobile.error.title', err)
            })
    }

    generateMergeKey() {
        this.spinner.perform('portal-content', this.details.generateMergeKey())
    }

    getStructures(fn) {
        return fn.map((id: string) => this.getStructure(id).name).join(', ');
    }
}
