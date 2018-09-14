import { ChangeDetectorRef, Component, OnInit } from '@angular/core'
import { Router } from '@angular/router'

import { AbstractSection } from '../abstract.section'
import { SpinnerService, UserListService, NotifyService } from '../../../../core/services'
import { SessionModel, Session, globalStore } from '../../../../core/store'
import { UsersStore } from '../../../users.store'

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
                        <a class="action" 
                            [routerLink]="['/admin', findVisibleStruct(duplicate.structures).id, 'users', duplicate.id]">
                            {{ duplicate.lastName | uppercase }} {{ duplicate.firstName }} {{ formatStructures(duplicate.structures) }}
                        </a>
                    </span>
                    <span *ngIf="!findVisibleStruct(duplicate.structures)">
                        {{ duplicate.lastName | uppercase }} {{ duplicate.firstName }} {{ (formatStructures(duplicate.structures)) }}
                    </span>
                    <span class="badge alert" *ngIf="duplicate.score > 3" 
                        [tooltip]="'blocking.duplicate.tooltip' | translate">
                        <s5l>blocking.duplicate</s5l>
                    </span>
                    <span class="badge info" *ngIf="duplicate.score < 4" 
                        [tooltip]="'minor.duplicate.tooltip' | translate">
                        <s5l>minor.duplicate</s5l>
                    </span>
                    <div>
                        <button (click)="separate(duplicate.id)" 
                            [disabled]="spinner.isLoading(duplicate.id)">
                            <spinner-cube class="button-spinner" waitingFor="duplicate.id">
                            </spinner-cube>
                            <s5l>separate</s5l>
                        </button>
                    </div>
                    <div>
                        <button (click)="merge(duplicate.id)" 
                            *ngIf="canMerge(duplicate)" [disabled]="spinner.isLoading(duplicate.id)">
                            <spinner-cube class="button-spinner" 
                                waitingFor="duplicate.id"></spinner-cube>
                            <s5l>merge</s5l>
                        </button>
                    </div>
                </li>
            </ul>
        </panel-section>
    `,
    inputs: ['user', 'structure', 'open'],
    providers: [ UserListService ],
    styles: [`
        ul.actions-list {
            margin: 0px;
        }
    `]
})
export class UserDuplicatesSection extends AbstractSection implements OnInit {

    constructor(protected spinner: SpinnerService,
        protected cdRef: ChangeDetectorRef,
        private router: Router,
        private usersStore: UsersStore,
        private ns: NotifyService) {
        super()
    }

    private open = true
    private session : Session
    protected onUserChange(){}

    ngOnInit() {
        SessionModel.getSession().then(session => { this.session = session })
    }

    formatStructures(structures) {
        return '(' + structures.map(structure => structure.name).join(', ') + ')';
    }

    canMerge(duplicate: { code: string, structures:[{id: string, name: string}]}) {
        if(!this.session)
            return false
        let localScope = this.session.functions['ADMIN_LOCAL'] && this.session.functions['ADMIN_LOCAL'].scope
        let superAdmin = this.session.functions['SUPER_ADMIN']
        let bothActivated = !this.user.code && !duplicate.code
        return !bothActivated && (superAdmin || localScope && duplicate.structures.some(structure => localScope.some(f =>  f == structure.id)))
    }

    findVisibleStruct(structures:[{id: string, name: string}]) {
        return structures.find(structure => globalStore.structures.data.some(struct => struct.id == structure.id))
    }

    private merge = (dupId) => {
        return this.spinner.perform(dupId, this.user.mergeDuplicate(dupId)).then(res => {
            if(res.id !== this.user.id && res['structure']) {
                this.usersStore.structure.users.data.splice(
                    this.usersStore.structure.users.data.findIndex(u => u.id == this.user.id), 1
                );
                this.router.navigate(['/admin', res['structure'], 'users', res.id])
                this.ns.success({
                    key: 'notify.user.merge.success.content',
                    parameters: {}
                }, 'notify.user.merge.success.title');
            }
            else {
                this.usersStore.structure.users.data.splice(
                    this.usersStore.structure.users.data.findIndex(u => u.id == res.id), 1
                );
            }
        }).catch((err) => {
            this.ns.error({
                key: 'notify.user.merge.error.content',
                parameters: {}
            }, 'notify.user.merge.error.title', err);
        })
    }

    private separate = (dupId) => {
        return this.spinner.perform(dupId, this.user.separateDuplicate(dupId)).then(res => {
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