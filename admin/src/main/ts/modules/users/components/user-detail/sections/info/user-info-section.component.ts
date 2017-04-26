import { Component, Input, ViewChild, ChangeDetectorRef } from '@angular/core'
import { NgModel } from '@angular/forms'
import { AbstractSection } from '../abstract.section'
import { LoadingService, NotifyService } from '../../../../../../services'

@Component({
    selector: 'user-info-section',
    templateUrl: './user-info-section.component.html',
    inputs: ['user', 'structure']
})
export class UserInfoSection extends AbstractSection {

    constructor(
        private ns: NotifyService,
        protected ls: LoadingService,
        protected cdRef: ChangeDetectorRef) {
        super()
    }

    @ViewChild("passwordMailInput") passwordMailInput : NgModel

    protected onUserChange(){
        if(this.passwordMailInput)
            this.passwordMailInput.reset()
    }

    private addAdml() {
        this.ls.perform('portal-content', this.details.addAdml(this.structure.id))
            .then(res => {
                this.ns.success({
                        key: 'notify.user.add.adml.content',
                        parameters: {
                            user: this.user.firstName + ' ' + this.user.lastName}
                        }
                    , 'notify.user.add.adml.title')
            }).catch(err => {
                this.ns.error({
                        key: 'notify.user.add.adml.error.content',
                        parameters: {
                            user: this.user.firstName + ' ' + this.user.lastName}
                        }
                    , 'notify.user.add.adml.error.title', err)
            })
    }

    private removeAdml() {
        this.ls.perform('portal-content', this.details.removeAdml())
            .then(res => {
                this.ns.success({
                        key: 'notify.user.remove.adml.content',
                        parameters: {
                            user: this.user.firstName + ' ' + this.user.lastName}
                        }
                    , 'notify.user.remove.adml.title')
            }).catch(err => {
                this.ns.error({
                        key: 'notify.user.remove.adml.error.content',
                        parameters: {
                            user: this.user.firstName + ' ' + this.user.lastName}
                        }
                    , 'notify.user.remove.adml.error.title', err)
            })
    }
}