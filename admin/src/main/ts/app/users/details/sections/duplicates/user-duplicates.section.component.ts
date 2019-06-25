import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { AbstractSection } from '../abstract.section';
import { NotifyService, SpinnerService, UserListService } from '../../../../core/services';
import { globalStore, Session, SessionModel } from '../../../../core/store';
import { UsersStore } from '../../../users.store';

@Component({
    selector: 'user-duplicates-section',
    template: `
        <panel-section
                section-title="users.details.section.duplicates"
                id="user-duplicates-section"
                [folded]="!open" *ngIf="user?.duplicates?.length > 0">
            <ul class="actions-list">
                <li *ngFor="let duplicate of user?.duplicates">
                    <span *ngIf="findVisibleStruct(duplicate.structures)">
                        <a class="action" target="_blank"
                           [routerLink]="['/admin', findVisibleStruct(duplicate.structures).id, 'users', duplicate.id, 'details']">
                            {{ duplicate.lastName | uppercase }} {{ duplicate.firstName }} {{ formatStructures(duplicate.structures) }}
                        </a>
                    </span>
                    <span *ngIf="!findVisibleStruct(duplicate.structures)">
                        {{ duplicate.lastName | uppercase }} {{ duplicate.firstName }} {{ (formatStructures(duplicate.structures)) }}
                    </span>
                    <span class="badge alert" *ngIf="duplicate.score > 3"
                          [title]="'blocking.duplicate.tooltip' | translate">
                        <s5l>blocking.duplicate</s5l>
                    </span>
                    <span class="badge info" *ngIf="duplicate.score < 4"
                          [title]="'minor.duplicate.tooltip' | translate">
                        <s5l>minor.duplicate</s5l>
                    </span>
                    <button class="actions-list__button" (click)="separate(duplicate.id)"
                            [disabled]="spinner.isLoading(duplicate.id)">
                        <spinner-cube class="button-spinner" waitingFor="duplicate.id">
                        </spinner-cube>
                        <s5l>separate</s5l>
                        <i class="fa fa-arrow-left"></i>
                        <i class="fa fa-arrow-right"></i>
                    </button>
                    <button class="actions-list__button" (click)="merge(duplicate.id)"
                            *ngIf="canMerge(duplicate)" [disabled]="spinner.isLoading(duplicate.id)">
                        <spinner-cube class="button-spinner"
                                      waitingFor="duplicate.id"></spinner-cube>
                        <s5l>merge</s5l>
                        <i class="fa fa-arrow-right"></i>
                        <i class="fa fa-arrow-left"></i>
                    </button>
                </li>
            </ul>
        </panel-section>
    `,
    inputs: ['user', 'structure', 'open'],
    providers: [UserListService],
    styles: [`
        ul.actions-list {
            margin: 0;
        }

        .actions-list .badge.alert {
            margin-right: 10px;
        }

        .actions-list__button {
            margin: 0 5px;
        }

        .actions-list__button i {
            font-size: 11px;
            float: none;
            padding-left: 5px;
        }
    `]
})
export class UserDuplicatesSection extends AbstractSection implements OnInit {

    constructor(protected spinner: SpinnerService,
                protected cdRef: ChangeDetectorRef,
                private router: Router,
                private usersStore: UsersStore,
                private userListService: UserListService,
                private ns: NotifyService) {
        super();
    }

    private open = true;
    private session: Session;

    protected onUserChange() {
    }

    ngOnInit() {
        SessionModel.getSession().then(session => this.session = session);
    }

    formatStructures(structures): string {
        return '(' + structures.map(structure => structure.name).join(', ') + ')';
    }

    canMerge(duplicate: { code: string, structures: [{ id: string, name: string }] }): boolean {
        if (!this.session) {
            return false;
        }
        const localScope = this.session.functions['ADMIN_LOCAL'] && this.session.functions['ADMIN_LOCAL'].scope;
        const superAdmin = this.session.functions['SUPER_ADMIN'];
        const bothActivated = !this.user.code && !duplicate.code;
        return !(bothActivated) && (!!superAdmin || localScope && duplicate.structures
                .some(structure => localScope.some(f => f == structure.id))
        );
    }

    findVisibleStruct(structures: [{ id: string, name: string }]): { id: string, name: string } {
        return structures.find(structure => globalStore.structures.data.some(struct => struct.id == structure.id));
    }

    private merge = (dupId) => {
        return this.spinner.perform(dupId, this.user.mergeDuplicate(dupId)).then(res => {
            if (res.id !== this.user.id && res.structure) {
                this.usersStore.structure.users.data.splice(
                    this.usersStore.structure.users.data.findIndex(u => u.id === this.user.id), 1
                );
                const resUser = this.usersStore.structure.users.data.find(u => u.id === res.id);
                resUser.duplicates = resUser.duplicates.filter(d => d.id !== this.user.id);
                this.router.navigate(['/admin', res.structure.id, 'users', res.id, 'details']);
                this.userListService.updateSubject.next();
                this.ns.success({
                    key: 'notify.user.merge.success.content',
                    parameters: {}
                }, 'notify.user.merge.success.title');
            } else {
                this.usersStore.structure.users.data.splice(
                    this.usersStore.structure.users.data.findIndex(u => u.id === res.id), 1
                );
            }
            this.userListService.updateSubject.next();
        }).catch((err) => {
            this.ns.error({
                key: 'notify.user.merge.error.content',
                parameters: {}
            }, 'notify.user.merge.error.title', err);
        })
    };

    private separate = (dupId) => {
        return this.spinner.perform(dupId, this.user.separateDuplicate(dupId)).then(res => {
            this.userListService.updateSubject.next();
            this.ns.success({
                key: 'notify.user.dissociate.success.content',
                parameters: {}
            }, 'notify.user.dissociate.success.title');
        }).catch((err) => {
            this.ns.error({
                key: 'notify.user.dissociate.error.content',
                parameters: {}
            }, 'notify.user.dissociate.error.title', err);
        })
    }
}