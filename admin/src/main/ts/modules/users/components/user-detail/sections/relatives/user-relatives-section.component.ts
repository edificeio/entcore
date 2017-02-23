import { Component, Input, ViewChild, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core'
import { AbstractControl } from '@angular/forms'

import { AbstractSection } from '../abstract.section'
import { UserListService, LoadingService } from '../../../../../../services'
import { User } from '../../../../../../models/mappings/user'

@Component({
    selector: 'user-relatives-section',
    template: `
        <panel-section section-title="users.details.section.relatives" [folded]="true" *ngIf="isStudent(user)">
            <button (click)="showRelativesLightbox = true">
                <s5l>add.relative</s5l><i class="fa fa-plus-circle"></i>
            </button>
            <light-box class="inner-list"
                    [show]="showRelativesLightbox" (onClose)="showRelativesLightbox = false">
                <div class="padded">
                    <h3><s5l>add.relative</s5l></h3>
                    <list-component class="inner-list"
                        [model]="structure?.users?.data"
                        [inputFilter]="userListService.filterByInput"
                        [filters]="filterRelatives"
                        searchPlaceholder="search.user"
                        [sort]="userListService.sorts"
                        [display]="userListService.display"
                        (inputChange)="userListService.inputFilter = $event"
                        [isDisabled]="disableRelative"
                        (onSelect)="wrap(details?.addRelative, $event.id, 0, $event)">
                    </list-component>
                </div>
            </light-box>
            <ul class="actions-list">
                <li *ngFor="let parent of details?.parents">
                    <a class="action" [routerLink]="['..', parent.id]">
                        {{ parent.lastName | uppercase }} {{ parent.firstName }}
                    </a>
                    <i  class="fa fa-times action" (click)="wrap(details?.removeRelative, parent.id, 0, parent)"
                        [tooltip]="'delete.this.relative' | translate"
                        [ngClass]="{ disabled: ls.isLoading(parent.id) }"></i>
                </li>
            </ul>
        </panel-section>
    `,
    inputs: ['user', 'structure'],
    providers: [ UserListService ]
})
export class UserRelativesSection extends AbstractSection {

    constructor(
            private userListService: UserListService,
            protected ls: LoadingService,
            protected cdRef: ChangeDetectorRef) {
        super(ls, cdRef)
    }

    @ViewChild("codeInput") codeInput : AbstractControl

    protected onUserChange(){}

    private isStudent(u:User){
        return u.type === 'Student'
    }

    private filterRelatives = (u: User) => {
        return this.details && this.details.parents &&
            u.type === 'Relative' && !this.details.parents.find(p => p.id === u.id)
    }

    private disableRelative = (relative) => {
        return this.ls.isLoading(relative.id)
    }

}