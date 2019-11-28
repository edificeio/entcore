import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnChanges, OnInit} from '@angular/core';

import {AbstractSection} from '../abstract.section';
import {UserModel} from '../../../../core/store/models/user.model';
import { UserListService } from 'src/app/core/services/userlist.service';
import { SpinnerService } from 'src/app/core/services/spinner.service';
import { NotifyService } from 'src/app/core/services/notify.service';

@Component({
    selector: 'ode-user-children-section',
    templateUrl: './user-children-section.component.html',
    inputs: ['user', 'structure'],
    providers: [ UserListService ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserChildrenSectionComponent extends AbstractSection implements OnInit, OnChanges {
    lightboxChildren: UserModel[] = [];

    public showChildrenLightbox = false;

    constructor(
            public userListService: UserListService,
            public spinner: SpinnerService,
            private ns: NotifyService,
            protected cdRef: ChangeDetectorRef) {
        super();
    }

    ngOnInit() {
        this.updateLightboxChildren();
    }

    ngOnChanges() {
        this.updateLightboxChildren();
    }

    private updateLightboxChildren() {
        this.lightboxChildren = this.structure.users.data.filter(
            u => u.type == 'Student'
                && !u.deleteDate
                && this.details.children
                && !this.details.children.find(c => c.id == u.id)
        );
    }

    protected onUserChange() {}

    isRelative(u: UserModel) {
        return u.type === 'Relative';
    }

    disableChild = (child) => {
        return this.spinner.isLoading(child.id);
    }

    addChild = (child) => {
        this.spinner.perform('portal-content', this.details.addChild(child)
            .then(() => {
                this.ns.success(
                    {
                        key: 'notify.user.add.child.content',
                        parameters: {
                            child:  child.displayName
                        }
                    }, 'notify.user.add.child.title');

                this.updateLightboxChildren();
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
            })
        );
    }

    removeChild = (child) => {
        this.spinner.perform('portal-content', this.details.removeChild(child)
            .then(() => {
                this.ns.success(
                    {
                        key: 'notify.user.remove.child.content',
                        parameters: {
                            child:  child.displayName
                        }
                    }, 'notify.user.remove.child.title');

                this.updateLightboxChildren();
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
            })
        );
    }
}
