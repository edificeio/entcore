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
}