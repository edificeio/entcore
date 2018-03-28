import { Component, Input, ViewChild, ChangeDetectorRef, 
    ChangeDetectionStrategy } from '@angular/core'
import { AbstractControl } from '@angular/forms'

import { AbstractSection } from '../abstract.section'
import { UserListService, SpinnerService } from '../../../../core/services'
import { UserModel } from '../../../../core/store/models/user.model'

@Component({
    selector: 'user-children-section',
    template: `
        <panel-section section-title="users.details.section.children" [folded]="true" *ngIf="isRelative(user)">
            <button (click)="showChildrenLightbox = true">
                <s5l>add.child</s5l><i class="fa fa-plus-circle"></i>
            </button>
            <lightbox class="inner-list"
                    [show]="showChildrenLightbox" (onClose)="showChildrenLightbox = false">
                <div class="padded">
                    <h3><s5l>add.child</s5l></h3>
                    <list class="inner-list"
                        [model]="structure?.users?.data"
                        [inputFilter]="userListService.filterByInput"
                        [filters]="filterChildren"
                        searchPlaceholder="search.user"
                        [sort]="userListService.sorts"
                        (inputChange)="userListService.inputFilter = $event"
                        [isDisabled]="disableChild"
                        (onSelect)="spinner.perform($event.id, details?.addChild($event), 0)">
                        <ng-template let-item>
                            <span class="display-name">
                                {{ item.displayName?.split(' ')[1] | uppercase }} {{ item.displayName?.split(' ')[0] }}
                            </span>
                        </ng-template>
                    </list>
                </div>
            </lightbox>
            <ul class="actions-list">
                <li *ngFor="let child of details?.children">
                    <div *ngIf="child.id">
                        <a class="action" [routerLink]="['..', child.id]">
                        {{ child.displayName?.split(' ')[1] | uppercase }} {{ child.displayName?.split(' ')[0] }}
                        </a>
                        <i  class="fa fa-times action" (click)="spinner.perform(child.id, details?.removeChild(child), 0)"
                            [tooltip]="'delete.this.child' | translate"
                            [ngClass]="{ disabled: spinner.isLoading(child.id)}"></i>
                    </div>
                </li>
            </ul>
        </panel-section>
    `,
    inputs: ['user', 'structure'],
    providers: [ UserListService ]
})
export class UserChildrenSection extends AbstractSection {
    @ViewChild("codeInput") 
    codeInput : AbstractControl

    constructor(
            private userListService: UserListService,
            protected spinner: SpinnerService,
            protected cdRef: ChangeDetectorRef) {
        super()
    }

    protected onUserChange(){}

    isRelative(u: UserModel){
        return u.type === 'Relative'
    }

    filterChildren = (u: UserModel) => {
        return this.details 
            && this.details.children 
            && !this.details.children.find(c => c.id === u.id)
            && u.type === 'Student' 
            && !u.deleteDate
    }

    disableChild = (child) => {
        return this.spinner.isLoading(child.id)
    }
}
