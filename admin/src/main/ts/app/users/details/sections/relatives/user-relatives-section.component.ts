import { Component, Input, ViewChild, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core'

import { AbstractSection } from '../abstract.section'
import { UserListService, SpinnerService, NotifyService } from '../../../../core/services'
import { UserModel } from '../../../../core/store/models'
import { OnChanges, OnInit } from '@angular/core/src/metadata/lifecycle_hooks';

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
                        [model]="lightboxRelatives"
                        [inputFilter]="userListService.filterByInput"
                        searchPlaceholder="search.user"
                        [sort]="userListService.sorts"
                        (inputChange)="userListService.inputFilter = $event"
                        [isDisabled]="disableRelative"
                        (onSelect)="addRelative($event)">
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
                        <a class="action" [routerLink]="['/admin', structure.id ,'users', parent.id, 'details']">{{ parent.displayName?.split(' ')[1] | uppercase }} {{ parent.displayName?.split(' ')[0] }}</a>
                        <i class="fa fa-times action" (click)="removeRelative(parent)"
                            [title]="'delete.this.relative' | translate"
                            [ngClass]="{ disabled: spinner.isLoading(parent.id) }"></i>
                    </div>
                </li>
            </ul>
        </panel-section>
    `,
    inputs: ['user', 'structure'],
    providers: [ UserListService ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserRelativesSection extends AbstractSection implements OnInit, OnChanges {
    public showRelativesLightbox = false;

    lightboxRelatives: UserModel[] = [];

    constructor(
        public userListService: UserListService,
        public spinner: SpinnerService,
        private ns: NotifyService,
        protected cdRef: ChangeDetectorRef) {
        super()
    }

    ngOnInit() {
        this.updateLightboxRelatives();
    }

    ngOnChanges() {
        this.updateLightboxRelatives();
    }

    private updateLightboxRelatives() {
        this.lightboxRelatives = this.structure.users.data.filter(
            u => u.type == 'Relative' 
                && !u.deleteDate
                && this.details.parents 
                && !this.details.parents.find(p => p.id == u.id)
        );
    }

    protected onUserChange(){}

    isStudent(u: UserModel){
        return u.type === 'Student'
    }

    disableRelative = (relative) => {
        return this.spinner.isLoading(relative.id)
    }

    addRelative = (relative) => {
        this.spinner.perform('portal-content', this.details.addRelative(relative)
            .then(() => {
                this.ns.success(
                    { 
                        key: 'notify.user.add.relative.content', 
                        parameters: {
                            relative:  relative.displayName
                        } 
                    }, 'notify.user.add.relative.title');

                this.updateLightboxRelatives();
                this.cdRef.markForCheck();
            })
            .catch(err => {
                this.ns.error(
                    {
                        key: 'notify.user.add.relative.error.content',
                        parameters: {
                            relative:  relative.displayName
                        }
                    }, 'notify.user.add.relative.error.title', err);
            })
        );
    }

    removeRelative = (relative) => {
        this.spinner.perform('portal-content', this.details.removeRelative(relative)
            .then(() => {
                this.ns.success(
                    { 
                        key: 'notify.user.remove.relative.content', 
                        parameters: {
                            relative:  relative.displayName
                        } 
                    }, 'notify.user.remove.relative.title');

                this.updateLightboxRelatives();
                this.cdRef.markForCheck();
            })
            .catch(err => {
                this.ns.error(
                    {
                        key: 'notify.user.remove.relative.error.content',
                        parameters: {
                            relative:  relative.displayName
                        }
                    }, 'notify.user.remove.relative.error.title', err);
            })
        );
    }
}