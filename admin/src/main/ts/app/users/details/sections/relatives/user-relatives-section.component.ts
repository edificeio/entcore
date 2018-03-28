import { Component, Input, ViewChild, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core'
import { AbstractControl } from '@angular/forms'

import { AbstractSection } from '../abstract.section'
import { UserListService, SpinnerService } from '../../../../core/services'
import { UserModel } from '../../../../core/store/models'

@Component({
    selector: 'user-relatives-section',
    template: `
        <panel-section section-title="users.details.section.relatives" [folded]="true" *ngIf="isStudent(user)">
            <button (click)="showRelativesLightbox = true">
                <s5l>add.relative</s5l><i class="fa fa-plus-circle"></i>
            </button>
            <lightbox class="inner-list"
                    [show]="showRelativesLightbox" (onClose)="showRelativesLightbox = false">
                <div class="padded">
                    <h3><s5l>add.relative</s5l></h3>
                    <list class="inner-list"
                        [model]="structure?.users?.data"
                        [inputFilter]="userListService.filterByInput"
                        [filters]="filterRelatives"
                        searchPlaceholder="search.user"
                        [sort]="userListService.sorts"
                        (inputChange)="userListService.inputFilter = $event"
                        [isDisabled]="disableRelative"
                        (onSelect)="spinner.perform($event.id, details?.addRelative($event), 0)">
                        <ng-template let-item>
                            <span class="display-name">
                                {{ item.displayName?.split(' ')[1] | uppercase }} {{ item.displayName?.split(' ')[0] }}
                            </span>
                        </ng-template>
                    </list>
                </div>
            </lightbox>
            <ul class="actions-list">
                <li *ngFor="let parent of details.parents">
                    <div *ngIf="parent.id">
                        <a class="action" [routerLink]="['..', parent.id]">
                            {{ parent.displayName?.split(' ')[1] | uppercase }} {{ parent.displayName?.split(' ')[0] }}
                        </a>
                        <i  class="fa fa-times action" (click)="spinner.perform(parent.id, details?.removeRelative(parent), 0)"
                            [tooltip]="'delete.this.relative' | translate"
                            [ngClass]="{ disabled: spinner.isLoading(parent.id) }"></i>
                    </div>
                </li>
            </ul>
        </panel-section>
    `,
    inputs: ['user', 'structure'],
    providers: [ UserListService ]
})
export class UserRelativesSection extends AbstractSection {
    @ViewChild("codeInput") 
    codeInput : AbstractControl

    constructor(
        public userListService: UserListService,
        public spinner: SpinnerService) {
        super()
    }

    protected onUserChange(){}

    isStudent(u: UserModel){
        return u.type === 'Student'
    }

    filterRelatives = (u: UserModel) => {
        return this.details 
            && this.details.parents 
            && !this.details.parents.find(p => p.id === u.id)
            && u.type === 'Relative' 
            && !u.deleteDate
    }

    disableRelative = (relative) => {
        return this.spinner.isLoading(relative.id)
    }

}