import { Component, Input, ViewChild, ChangeDetectorRef, OnInit } from '@angular/core'
import { NgModel } from '@angular/forms'
import { AbstractSection } from '../abstract.section'
import { LoadingService, NotifyService } from '../../../../../../services'

@Component({
    selector: 'user-info-section',
    templateUrl: './user-info-section.component.html',
    inputs: ['user', 'structure']
})
export class UserInfoSection extends AbstractSection implements OnInit {

    private passwordResetMail
    private passwordResetMobile

    constructor(
        private ns: NotifyService,
        protected ls: LoadingService,
        protected cdRef: ChangeDetectorRef) {
        super()
    }

    ngOnInit() {
        this.passwordResetMail = this.details.email
        this.passwordResetMobile = this.details.mobile
    }

    protected onUserChange(){
        if(!this.details.activationCode) {
            this.passwordResetMail = this.details.email
            this.passwordResetMobile = this.details.mobile
        }
    }

    private addAdml() {
        this.ls.perform('portal-content', this.details.addAdml(this.structure.id))
            .then(res => {
                this.ns.success({
                        key: 'notify.user.add.adml.content',
                        parameters: {user: this.user.firstName + ' ' + this.user.lastName}
                    }, 'notify.user.add.adml.title')
            }).catch(err => {
                this.ns.error({
                        key: 'notify.user.add.adml.error.content',
                        parameters: {user: this.user.firstName + ' ' + this.user.lastName}
                    }, 'notify.user.add.adml.error.title', err)
            })
    }

    private removeAdml() {
        this.ls.perform('portal-content', this.details.removeAdml())
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

    private sendResetPasswordMail(email: string) {
        this.ls.perform('portal-content', this.details.sendResetPassword({type: 'email', value: email}))
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

    private sendResetPasswordMobile(mobile: string) {
        this.ls.perform('portal-content', this.details.sendResetPassword({type: 'mobile', value: mobile}))
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
}