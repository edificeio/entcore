import { Component, Input, ViewChild, ChangeDetectorRef, 
    ChangeDetectionStrategy } from '@angular/core'
import { AbstractControl } from '@angular/forms'

import { AbstractSection } from '../abstract.section'
import { UserListService, SpinnerService, NotifyService } from '../../../../core/services'
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
                        (onSelect)="addChild($event)">
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
                        <i  class="fa fa-times action" (click)="removeChild(child)"
                            [tooltip]="'delete.this.child' | translate"
                            [ngClass]="{ disabled: spinner.isLoading(child.id)}"></i>
                    </div>
                </li>
            </ul>
        </panel-section>
    `,
    inputs: ['user', 'structure'],
    providers: [ UserListService ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserChildrenSection extends AbstractSection {
    @ViewChild("codeInput") 
    codeInput : AbstractControl

    constructor(
            private userListService: UserListService,
            protected spinner: SpinnerService,
            private ns: NotifyService,
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

    addChild = (child) => {
        console.log(child);
        this.spinner.perform('portal-content', this.details.addChild(child))
            .then(() => {
                this.ns.success(
                    { 
                        key: 'notify.user.add.child.content', 
                        parameters: {
                            child:  child.displayName
                        } 
                    }, 'notify.user.add.child.title');
                this.cdRef.markForCheck();
            })
            .catch(err => {
                this.ns.error(
                    {
                        key: 'notify.user.add.child.error.content',
                        parameters: {
                            child:  child.displayName
                        }
                    }, 'notify.user.add.child.error.title', err);
            });
    }

    removeChild = (child) => {
        console.log(child);
        this.spinner.perform('portal-content', this.details.removeChild(child))
            .then(() => {
                this.ns.success(
                    { 
                        key: 'notify.user.remove.child.content', 
                        parameters: {
                            child:  child.displayName
                        } 
                    }, 'notify.user.remove.child.title');
                this.cdRef.markForCheck();
            })
            .catch(err => {
                this.ns.error(
                    {
                        key: 'notify.user.remove.child.error.content',
                        parameters: {
                            child:  child.displayName
                        }
                    }, 'notify.user.remove.child.error.title', err);
            });
    }
}
