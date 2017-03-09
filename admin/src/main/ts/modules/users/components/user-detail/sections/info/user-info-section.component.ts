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
        super(ls, cdRef)
    }

    @ViewChild("passwordMailInput") passwordMailInput : NgModel

    protected onUserChange(){
        if(this.passwordMailInput)
            this.passwordMailInput.reset()
    }

    toggleUserBlock() {
        this.ls.perform('user.block', this.details.toggleBlock())
            .then(() => {
                this.user.blocked = !this.user.blocked
                this.ns.success(
                    {
                        key: 'notify.user.toggleblock.content',
                        parameters: {
                            user:       this.user.firstName + ' ' + this.user.lastName,
                            blocked:    this.user.blocked
                        }
                    },
                    {
                        key: 'notify.user.toggleblock.title',
                        parameters: {
                            blocked:    this.user.blocked
                        }
                    })
            }).catch(err => {
                 this.ns.error(
                    {
                        key: 'notify.user.toggleblock.error.content',
                        parameters: {
                            user:       this.details.firstName + ' ' + this.user.lastName,
                            blocked:    !this.user.blocked
                        }
                    },
                    {
                        key: 'notify.user.toggleblock.error.title',
                        parameters: {
                            blocked:    !this.user.blocked
                        }
                    }, err)
            })
    }

}