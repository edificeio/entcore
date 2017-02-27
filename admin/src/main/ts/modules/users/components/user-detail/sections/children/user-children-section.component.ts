import { Component, Input, ViewChild, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core'
import { AbstractControl } from '@angular/forms'

import { AbstractSection } from '../abstract.section'
import { UserListService, LoadingService } from '../../../../../../services'
import { User } from '../../../../../../store/mappings/user'

@Component({
    selector: 'user-children-section',
    template: `
        <panel-section section-title="users.details.section.children" [folded]="true" *ngIf="isRelative(user)">
            <button (click)="showChildrenLightbox = true">
                <s5l>add.child</s5l><i class="fa fa-plus-circle"></i>
            </button>
            <light-box class="inner-list"
                    [show]="showChildrenLightbox" (onClose)="showChildrenLightbox = false">
                <div class="padded">
                    <h3><s5l>add.child</s5l></h3>
                    <list-component class="inner-list"
                        [model]="structure?.users?.data"
                        [inputFilter]="userListService.filterByInput"
                        [filters]="filterChildren"
                        searchPlaceholder="search.user"
                        [sort]="userListService.sorts"
                        [display]="userListService.display"
                        (inputChange)="userListService.inputFilter = $event"
                        [isDisabled]="disableChild"
                        (onSelect)="wrap(details?.addChild, $event.id, 0, $event)">
                    </list-component>
                </div>
            </light-box>
            <ul class="actions-list">
                <li *ngFor="let child of details?.children">
                    <a class="action" [routerLink]="['..', child.id]">
                        {{ child.lastName | uppercase }} {{ child.firstName }}
                    </a>
                    <i  class="fa fa-times action" (click)="wrap(details?.removeChild, child.id, 0, child)"
                        [tooltip]="'delete.this.child' | translate"
                        [ngClass]="{ disabled: ls.isLoading(child.id)}"></i>
                </li>
            </ul>
        </panel-section>
    `,
    inputs: ['user', 'structure'],
    providers: [ UserListService ]
})
export class UserChildrenSection extends AbstractSection {

    constructor(
            private userListService: UserListService,
            protected ls: LoadingService,
            protected cdRef: ChangeDetectorRef) {
        super(ls, cdRef)
    }

    @ViewChild("codeInput") codeInput : AbstractControl

    protected onUserChange(){}

    private isRelative(u: User){
        return u.type === 'Relative'
    }

    private filterChildren = (u: User) => {
        return this.details && this.details.children &&
            u.type === 'Student' && !this.details.children.find(c => c.id === u.id)
    }

    private disableChild = (child) => {
        return this.ls.isLoading(child.id)
    }

}