import { ChangeDetectorRef, Component, OnInit } from '@angular/core'
import { Router } from '@angular/router'

import { AbstractSection } from '../abstract.section'
import { SpinnerService, UserListService } from '../../../../core/services'
import { SessionModel, Session, globalStore } from '../../../../core/store'

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
                            [routerLink]="['/admin', findVisibleStruct(duplicate.structures), 'users', duplicate.id]">
                            {{ duplicate.lastName | uppercase }} {{ duplicate.firstName }}
                        </a>
                    </span>
                    <span *ngIf="!findVisibleStruct(duplicate.structures)">
                        {{ duplicate.lastName | uppercase }} {{ duplicate.firstName }}
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
                        <button (click)="spinner.perform(duplicate.id, user.separateDuplicate(duplicate.id))" 
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
        private router: Router) {
        super()
    }

    private open = true
    private session : Session
    protected onUserChange(){}

    ngOnInit() {
        SessionModel.getSession().then(session => { this.session = session })
    }

    canMerge(duplicate: { code: string, structures: string[] }) {
        if(!this.session)
            return false
        let localScope = this.session.functions['ADMIN_LOCAL'] && this.session.functions['ADMIN_LOCAL'].scope
        let superAdmin = this.session.functions['SUPER_ADMIN']
        let bothActivated = !this.user.code && !duplicate.code
        return !bothActivated && (superAdmin || localScope && duplicate.structures.some(sId => localScope.some(f =>  f === sId)))
    }

    findVisibleStruct(sIds: string[]) {
        return sIds.find(id => globalStore.structures.data.some(struct => struct.id === id))
    }

    private merge = (dupId) => {
        return this.spinner.perform(dupId, this.user.mergeDuplicate(dupId)).then(res => {
            if(res.id !== this.user.id && res['structure']) {
                this.router.navigate(['/admin', res['structure'], 'users', res.id])
            }
        })
    }
}