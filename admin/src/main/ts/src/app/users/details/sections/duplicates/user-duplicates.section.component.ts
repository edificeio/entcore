import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';

import {AbstractSection} from '../abstract.section';
import {NotifyService, SpinnerService, UserListService} from '../../../../core/services';
import {globalStore, Session, SessionModel} from '../../../../core/store';
import {UsersStore} from '../../../users.store';

@Component({
    selector: 'ode-user-duplicates-section',
    templateUrl: './user-duplicates.section.component.html',
    inputs: ['user', 'structure', 'open'],
    providers: [UserListService],
    styleUrls: ['./user-duplicates.section.component.scss']
})
export class UserDuplicatesSectionComponent extends AbstractSection implements OnInit {

    constructor(public spinner: SpinnerService,
                protected cdRef: ChangeDetectorRef,
                private router: Router,
                private usersStore: UsersStore,
                private userListService: UserListService,
                private ns: NotifyService) {
        super();
    }

    public comparedUserId: string = null;

    open = true;
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
        const localScope = this.session.functions.ADMIN_LOCAL && this.session.functions.ADMIN_LOCAL.scope;
        const superAdmin = this.session.functions.SUPER_ADMIN;
        const bothActivated = !this.user.code && !duplicate.code;
        return !(bothActivated) && (!!superAdmin || localScope && duplicate.structures
                .some(structure => localScope.some(f => f == structure.id))
        );
    }

    findVisibleStruct(structures: [{ id: string, name: string }]): { id: string, name: string } {
        return structures.find(structure => globalStore.structures.data.some(struct => struct.id == structure.id));
    }

    public compare(comparedId: string) {
        this.comparedUserId = comparedId;
    }

    merge = (dupId) => {
        return this.spinner.perform(dupId, this.user.mergeDuplicate(dupId)).then(res => {
            if (res.id !== this.user.id && res.structure) {
                this.usersStore.structure.users.data.splice(
                    this.usersStore.structure.users.data.findIndex(u => u.id === this.user.id), 1
                );
                const resUser = this.usersStore.structure.users.data.find(u => u.id === res.id);
                resUser.duplicates = resUser.duplicates.filter(d => d.id !== this.user.id);
                this.router.navigate(['/admin', res.structure.id, 'users', res.id, 'details']);
                this.userListService.$updateSubject.next();
                this.ns.success({
                    key: 'notify.user.merge.success.content',
                    parameters: {}
                }, 'notify.user.merge.success.title');
            } else {
                this.usersStore.structure.users.data.splice(
                    this.usersStore.structure.users.data.findIndex(u => u.id === res.id), 1
                );
            }
            this.userListService.$updateSubject.next();
        }).catch((err) => {
            this.ns.error({
                key: 'notify.user.merge.error.content',
                parameters: {}
            }, 'notify.user.merge.error.title', err);
        });
    }

    separate = (dupId) => {
        return this.spinner.perform(dupId, this.user.separateDuplicate(dupId)).then(res => {
            this.userListService.$updateSubject.next();
            this.ns.success({
                key: 'notify.user.dissociate.success.content',
                parameters: {}
            }, 'notify.user.dissociate.success.title');
        }).catch((err) => {
            this.ns.error({
                key: 'notify.user.dissociate.error.content',
                parameters: {}
            }, 'notify.user.dissociate.error.title', err);
        });
    }
}
