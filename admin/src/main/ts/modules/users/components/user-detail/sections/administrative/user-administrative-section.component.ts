import { Component, Input, ViewChild, ChangeDetectorRef } from '@angular/core'
import { NgForm } from '@angular/forms'
import { AbstractSection } from '../abstract.section'
import { LoadingService, NotifyService } from '../../../../../../services'

@Component({
    selector: 'user-administrative-section',
    templateUrl: './user-administrative-section.component.html',
    inputs: ['user', 'structure']
})
export class UserAdministrativeSection extends AbstractSection {

    constructor(
        private ns: NotifyService,
        protected ls: LoadingService,
        protected cdRef: ChangeDetectorRef) {
        super()
    }

    @ViewChild("administrativeForm") administrativeForm : NgForm

    protected onUserChange() {
        if(this.administrativeForm){
            this.administrativeForm.reset(this.details && this.details.toJSON())
        }
    }

    public updateDetails() {
        this.ls.perform('user.update', this.details.update())
            .then(() => {
                this.ns.success(
                    { key: 'notify.user.update.content', parameters: {user: this.details.firstName + ' ' + this.user.lastName } },
                    'notify.user.update.title')
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

}